package com.example.zim.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zim.data.room.Dao.MessageDao
import com.example.zim.data.room.Dao.UserDao
import com.example.zim.events.ChatsEvent
import com.example.zim.states.ChatsState
import com.example.zim.utils.Logger
import com.example.zim.utils.LogType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val userDao: UserDao,
    private val messageDao: MessageDao,
    private val logger: Logger
) : ViewModel() {

    private val _state = MutableStateFlow(ChatsState())

    val logs = logger.logs

    init {
        fetchUsers("")
        fetchUnReadMsgs()
    }

    val state: StateFlow<ChatsState> = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        ChatsState()
    )

    fun onEvent(event: ChatsEvent) {
        when (event) {
            is ChatsEvent.ExpandMenu -> _state.update { it.copy(menuExpanded = true) }
            is ChatsEvent.DismissMenu -> _state.update { it.copy(menuExpanded = false) }
            is ChatsEvent.ChangeQuery -> {
                _state.update { it.copy(query = event.newQuery) }
                fetchUsers(event.newQuery)
            }
            is ChatsEvent.EnterSelectionMode -> {
                _state.update {
                    it.copy(
                        isSelectionModeActive = true,
                        selectedChatIds = if (event.chatId != null)
                            it.selectedChatIds + event.chatId
                        else
                            it.selectedChatIds
                    )
                }
            }
            is ChatsEvent.ToggleChatSelection -> {
                _state.update {
                    val updatedIds = if (it.selectedChatIds.contains(event.chatId)) {
                        it.selectedChatIds - event.chatId
                    } else {
                        it.selectedChatIds + event.chatId
                    }
                    // If no chats are selected, exit selection mode
                    val shouldRemainInSelectionMode = updatedIds.isNotEmpty()
                    it.copy(
                        selectedChatIds = updatedIds,
                        isSelectionModeActive = shouldRemainInSelectionMode
                    )
                }
            }
            is ChatsEvent.ExitSelectionMode -> {
                _state.update {
                    it.copy(
                        isSelectionModeActive = false,
                        selectedChatIds = emptySet()
                    )
                }
            }
            is ChatsEvent.DeleteSelectedChats -> {
                markChatsAsInactive(_state.value.selectedChatIds.toList())
                _state.update {
                    it.copy(
                        isSelectionModeActive = false,
                        selectedChatIds = emptySet()
                    )
                }
            }
        }
    }

    private fun fetchUsers(query: String) {
        viewModelScope.launch {
            try {
                userDao.getUsersWithLatestMessage(query).collect { chats ->
                    _state.update { it.copy(chats = chats) }
                }
            } catch (e: Exception) {
                // Handle errors gracefully
                Log.e("fetchUsers", "Error fetching users with latest messages: ${e.message}")
                // Optionally, update _state with an error or fallback data
                _state.update { it.copy(chats = emptyList()) }
            }
        }
    }

    private fun fetchUnReadMsgs() {
        viewModelScope.launch {
            try {
                messageDao.getUnReadMsgsCount().collect { count ->
                    _state.update { it.copy(unReadMsgs = count) }
                }
            } catch (e: Exception) {
                // Handle errors gracefully
                Log.e("fetchUsers", "Error fetching users with latest messages: ${e.message}")
                // Optionally, update _state with an error or fallback data
                _state.update { it.copy(chats = emptyList()) }
            }
        }
    }

    // Function to mark users as inactive
    private fun markChatsAsInactive(chatIds: List<Int>) {
        viewModelScope.launch {
            try {
                chatIds.forEach { chatId ->
                    try {
                        userDao.updateUserActiveStatus(chatId, false)

                        // Get and delete all messages related to this user
                        val messages = messageDao.getAllMessagesOfUser(chatId)
                        messageDao.deleteMessages(messages)

                    } catch (e: Exception) {
//                        logger.log("Failed to delete messages for chat $chatId: ${e.message}", "ChatsViewModel", LogType.ERROR)
                    }
                }

                fetchUsers(_state.value.query)

            } catch (e: Exception) {
//                logger.log("Error in markChatsAsInactive: ${e.message}", "ChatsViewModel", LogType.ERROR)
            }
        }
    }

}