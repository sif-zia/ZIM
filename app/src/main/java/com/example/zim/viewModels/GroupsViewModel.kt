package com.example.zim.viewModels

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zim.api.ActiveUserManager
import com.example.zim.api.ClientRepository
import com.example.zim.api.GroupInvite
import com.example.zim.api.User
import com.example.zim.data.room.Dao.GroupDao
import com.example.zim.data.room.Dao.MessageDao
import com.example.zim.data.room.Dao.UserDao
import com.example.zim.data.room.models.GroupMemberships
import com.example.zim.data.room.models.GroupMsgReceivers
import com.example.zim.data.room.models.Groups
import com.example.zim.data.room.models.Messages
import com.example.zim.data.room.models.SentMessages
import com.example.zim.events.GroupsEvent
import com.example.zim.helperclasses.GroupWithPendingInvite
import com.example.zim.states.GroupsState
import com.example.zim.utils.CryptoHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class GroupsViewModel @Inject constructor(
    val application: Application,
    private val userDao: UserDao,
    private val groupDao: GroupDao,
    private val messageDao: MessageDao,
    private val cryptoHelper: CryptoHelper,
    private val activeUserManager: ActiveUserManager,
    private val clientRepository: ClientRepository
) : ViewModel() {
    private val _state = MutableStateFlow(GroupsState())
    val state: StateFlow<GroupsState> = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        GroupsState()
    )

    private val tag = "GroupsViewModel"
    private var currentGroupJob: Job? = null

    init {
        viewModelScope.launch {
            userDao.getConnectedUsers().collect { users ->
                _state.value = _state.value.copy(users = users)
            }
        }

        viewModelScope.launch {
            _state.collect { state ->
                groupDao.getGroupsWithLatestMessage(state.searchQuery).collect { groups ->
                    _state.value = _state.value.copy(groups = groups)
                }
            }
        }

        viewModelScope.launch {
            groupDao.getGroupsWithPendingInvitations().collect { pendingInvites ->
                val groupInvites = convertToGroupInvite(pendingInvites)
                _state.value = _state.value.copy(groupInvites = groupInvites)
            }
        }

        viewModelScope.launch {
            groupDao.getUsersWithPendingGroupInvitations().collect { pendingUsersPuKeys ->
                _state.update { it.copy(pendingUsersPuKeys = pendingUsersPuKeys) }
            }
        }

        viewModelScope.launch {
            activeUserManager.activeUsers.collect { activeUsers ->
                activeUsers.keys.forEach { puKey ->
                    if (puKey in _state.value.pendingUsersPuKeys) {
                        val groupInvites = _state.value.groupInvites.filter { groupInvite ->
                            groupInvite.groupMembers.any { user -> user.puKey == puKey }
                        }

                        if (groupInvites.isNotEmpty()) {
                            groupInvites.forEach { groupInvite ->
                                val ip = activeUserManager.getIpAddressForUser(puKey)
                                if (ip != null) {
                                    val wasInviteSent =
                                        clientRepository.sendGroupInvite(groupInvite, ip)
                                    if (wasInviteSent) {
                                        Log.d(tag, "Group invite sent to $puKey")
                                        groupDao.setGroupInviteReceived(
                                            groupInvite.groupSecretKey,
                                            puKey
                                        )
                                    } else {
                                        Log.d(tag, "Failed to send group invite to $puKey")
                                        showToast("Failed to send group invite to $puKey")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun onEvent(event: GroupsEvent) {
        when (event) {
            is GroupsEvent.AddGroup -> {
                viewModelScope.launch {
                    val currentUser = userDao.getCurrentUser() ?: return@launch
                    val group = Groups(
                        name = event.groupName,
                        description = event.groupDescription,
                        admin = currentUser.users.id,
                        secretKey = cryptoHelper.generateRandomSecretKey()
                    )
                    val groupId = groupDao.insertGroup(group)

                    if (groupId < 0) {
                        Log.d(tag, "Failed to create group")
                        showToast("Failed to create group")
                        return@launch
                    }

                    val groupMembers = event.groupMembers.map { user ->
                        GroupMemberships(
                            groupId = groupId.toInt(),
                            userId = user.id,
                            hasReceivedInvitation = false
                        )
                    }.toMutableList()

                    groupMembers.add(
                        GroupMemberships(
                            groupId = groupId.toInt(),
                            userId = currentUser.users.id,
                            hasReceivedInvitation = true
                        )
                    )

                    groupMembers.map { groupMembership ->
                        groupDao.insertGroupMember(groupMembership)
                    }
                }
            }

            is GroupsEvent.UpdateSearchQuery -> {
                viewModelScope.launch {
                    _state.update {
                        it.copy(searchQuery = event.query)
                    }
                }
            }

            is GroupsEvent.LoadGroupData -> {
                currentGroupJob?.cancel()

                viewModelScope.launch {
                    val group = groupDao.getGroupById(event.groupId)
                    _state.update {
                        it.copy(
                            currentGroupId = event.groupId,
                            currentGroupName = group.name,
                            currentGroupDescription = group.description
                        )
                    }
                }

                currentGroupJob = viewModelScope.launch {

                    Log.d(tag, "Group: ${_state.value.currentGroupName}")
                    messageDao.getGroupChatContent(groupID = event.groupId).collect { groupChats ->
                        _state.update {
                            it.copy(
                                currentGroupChats = groupChats
                            )
                        }
                    }
                }
            }

            is GroupsEvent.SendGroupMessage -> {
                viewModelScope.launch {
                    val currentUser = userDao.getCurrentUser() ?: return@launch
                    val messageId = insertGroupSentMessage(
                        groupId = event.groupId,
                        message = event.message,
                        currentUserId = currentUser.users.id
                    )

                    if (messageId < 0) {
                        Log.d(tag, "Failed to send group message")
                        showToast("Failed to send group message")
                    } else {
                        Log.d(tag, "Group message sent successfully")
                    }
                }
            }
        }
    }

    private suspend fun showToast(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(application, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Converts a list of GroupWithPendingInvite to a list of GroupInvite
     * Groups the results by groupId and aggregates users for each group
     */
    private fun convertToGroupInvite(pendingInvites: List<GroupWithPendingInvite>): List<GroupInvite> {
        // Group items by groupId to collect all members for each group
        return pendingInvites.groupBy { it.groupId }
            .map { (_, groupInvites) ->
                // We can take group details from any item in the list since they'll be the same for a group
                val firstItem = groupInvites.first()

                GroupInvite(
                    groupName = firstItem.groupName,
                    // Use empty string as fallback for null values
                    groupDescription = firstItem.groupDescription ?: "",
                    groupSecretKey = firstItem.groupSecretKey ?: "",
                    groupAdminPuKey = firstItem.adminPuKey,
                    // Map all users from this group
                    groupMembers = groupInvites.map { invite ->
                        User(
                            fName = invite.firstName,
                            lName = invite.lastName ?: "",
                            puKey = invite.puKey
                        )
                    }
                )
            }
    }

    // Function to insert a message sent to a group
    private suspend fun insertGroupSentMessage(
        groupId: Int,                    // ID of the group to send to
        message: String,                 // Message content
        msgType: String = "Text",        // Message type (default: Text)
        currentUserId: Int? = null       // Current user ID (optional - will use current user if null)
    ): Int {
        // Get current user ID if not provided
        val userId = currentUserId ?: userDao.getCurrentUser()?.users?.id
        ?: return -1  // Return -1 if we can't determine current user

        // Insert message with isDM = false to indicate it's a group message
        val msgId = messageDao.insertMessage(
            Messages(
                msg = message,
                isSent = true,
                type = msgType,
                isDM = false  // This is a group message
            )
        )

        if (msgId <= 0) return -1  // Failed to insert message

        val messageIdInt = msgId.toInt()

        // Insert sent message record
        val sentMessageId = messageDao.insertSentMessage(
            SentMessages(
                sentTime = LocalDateTime.now(),
                userIDFK = userId,
                status = "Sent",  // Assuming group messages are marked as sent immediately
                msgIDFK = messageIdInt
            )
        ).toInt()

        if (sentMessageId <= 0) return -1  // Failed to insert sent message

        // Get all members of the group
        val groupMembers = groupDao.getGroupMemberIds(groupId)

        // Insert entries in GroupMessageReceivers for each member of the group
        for (memberId in groupMembers) {
            // Skip the sender (current user)
            if (memberId != userId) {
                messageDao.insertGroupMessageReceiver(
                    GroupMsgReceivers(
                        msgIdFK = messageIdInt,
                        userIdFK = memberId,
                        groupIdFK = groupId
                    )
                )
            }
        }

        // Also insert an entry for the sender to link the message with the group
        messageDao.insertGroupMessageReceiver(
            GroupMsgReceivers(
                msgIdFK = messageIdInt,
                userIdFK = userId,
                groupIdFK = groupId
            )
        )

        return sentMessageId
    }
}