package com.example.zim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zim.data.room.Dao.MessageDao
import com.example.zim.data.room.Dao.UserDao
import com.example.zim.data.room.models.Users
import com.example.zim.events.ChatsEvent
import com.example.zim.helperclasses.Chat
import com.example.zim.states.ChatsState
import com.example.zim.states.SignUpState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val userDao: UserDao,
    private val messageDao: MessageDao
) : ViewModel() {

    private val _state = MutableStateFlow(ChatsState())

    init {
        fetchUsers("")
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
        }

    }

    private fun fetchUsers(query: String) {
        viewModelScope.launch {
            userDao.getUsersByName(query).collectLatest { usersList ->
                val chats = usersList.map { user ->
                    Chat(name = "${user.fName} ${user.lName}", id = user.id)
                }

                // Update _state with new chats
                _state.update { it.copy(chats = chats) }

                // Map each chat with its latest message
                val updatedChats = chats.map { chat ->
                    val sentMessage = messageDao.getLatestSentMessageByReceiverId(chat.id)
                    val receivedMessage = messageDao.getLatestReceivedMessageBySenderId(chat.id)

                    // Determine the latest message ID and time by comparing timestamps
                    val (latestMessageId, latestMessageTime) = when {
                        sentMessage == null && receivedMessage == null -> null to null
                        sentMessage == null -> receivedMessage?.id to receivedMessage?.receivedTime
                        receivedMessage == null -> sentMessage.id to sentMessage.sentTime
                        sentMessage.sentTime.isAfter(receivedMessage.receivedTime) -> sentMessage.id to sentMessage.sentTime
                        else -> receivedMessage.id to receivedMessage.receivedTime
                    }

                    // Fetch message content if `latestMessageId` is not null
                    val messageContent = latestMessageId?.let { id ->
                        messageDao.getMessageById(id)?.msg
                    }

                    // Create a modified chat with the latest message content and timestamp
                    chat.copy(
                        lastMsg = messageContent,
                        time = latestMessageTime
                    )
                }

                // Update _state with chats containing the latest messages
                _state.update { it.copy(chats = updatedChats) }
            }
        }
    }


}