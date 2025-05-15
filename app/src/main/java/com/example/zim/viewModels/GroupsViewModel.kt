package com.example.zim.viewModels

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zim.data.room.Dao.GroupDao
import com.example.zim.data.room.Dao.UserDao
import com.example.zim.data.room.models.GroupMemberships
import com.example.zim.data.room.models.Groups
import com.example.zim.events.GroupsEvent
import com.example.zim.states.GroupsState
import com.example.zim.utils.CryptoHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class GroupsViewModel @Inject constructor(
    val application: Application,
    private val userDao: UserDao,
    private val groupDao: GroupDao,
    private val cryptoHelper: CryptoHelper
): ViewModel() {
    private val _state = MutableStateFlow(GroupsState())
    val state: StateFlow<GroupsState> = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        GroupsState()
    )

    private val tag = "GroupsViewModel"

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
    }

    fun onEvent(event: GroupsEvent) {
        when(event) {
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

                    if(groupId < 0) {
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

                    groupMembers.add(GroupMemberships(
                        groupId = groupId.toInt(),
                        userId = currentUser.users.id,
                        hasReceivedInvitation = true
                    ))

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
        }
    }

    suspend fun showToast(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(application, message, Toast.LENGTH_SHORT).show()
        }
    }
}