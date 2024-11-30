package com.example.zim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zim.data.room.Dao.MessageDao
import com.example.zim.data.room.Dao.UserDao
import com.example.zim.events.UserChatEvent
import com.example.zim.helperclasses.ChatBox
import com.example.zim.states.UserChatState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserChatViewModel @Inject constructor(
    private val userDao: UserDao,
    private val messageDao: MessageDao
) : ViewModel() {
    private val _state = MutableStateFlow(UserChatState())
    val state: StateFlow<UserChatState> = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        UserChatState()
    )

    fun onEvent(event: UserChatEvent) {
        when (event) {
            is UserChatEvent.LoadData -> {
                viewModelScope.launch {
                    val user = userDao.getUserById(event.userId)
                    _state.update {
                        it.copy(
                            username = "${user.fName} ${user.lName}",
                            dpUri = user.cover
                        )
                    }

                    val chatBoxList = mutableListOf<ChatBox>()


                    messageDao.getAllMessagesOfAUser(event.userId)
                        .collectLatest { chatContentList ->
                            chatBoxList.clear()
                            chatBoxList.addAll(
                                chatContentList.map { chatContent ->
                                    if (chatContent.isReceived)
                                        ChatBox.ReceivedMessage(
                                            chatContent.message,
                                            chatContent.time.toLocalTime(),
                                            chatContent.time.toLocalDate()
                                        )
                                    else
                                        ChatBox.SentMessage(
                                            chatContent.message,
                                            chatContent.time.toLocalTime(),
                                            chatContent.time.toLocalDate()
                                        )
                                }
                            )

                            if (chatBoxList.isNotEmpty()) {
                                val firstMessage = chatBoxList[0]
                                if (firstMessage is ChatBox.SentMessage)
                                    chatBoxList.add(0, ChatBox.DateChip(firstMessage.date))
                                else if (firstMessage is ChatBox.ReceivedMessage)
                                    chatBoxList.add(0, ChatBox.DateChip(firstMessage.date))

                                for (index in 2..<chatBoxList.size) {
                                    val currMessage = chatBoxList[index]
                                    val prevMessage = chatBoxList[index - 1]

                                    if (currMessage is ChatBox.ReceivedMessage && prevMessage is ChatBox.ReceivedMessage) {
                                        currMessage.isFirst = false
                                        if (currMessage.date.isAfter(prevMessage.date)) {
                                            chatBoxList.add(
                                                index,
                                                ChatBox.DateChip(currMessage.date)
                                            )
                                        }
                                    } else if (currMessage is ChatBox.SentMessage && prevMessage is ChatBox.SentMessage) {
                                        currMessage.isFirst = false
                                        if (currMessage.date.isAfter(prevMessage.date)) {
                                            chatBoxList.add(
                                                index,
                                                ChatBox.DateChip(currMessage.date)
                                            )
                                        }
                                    } else if (currMessage is ChatBox.ReceivedMessage && prevMessage is ChatBox.SentMessage) {
                                        if (currMessage.date.isAfter(prevMessage.date)) {
                                            chatBoxList.add(
                                                index,
                                                ChatBox.DateChip(currMessage.date)
                                            )
                                            currMessage.isFirst = false
                                        }
                                    } else if (currMessage is ChatBox.SentMessage && prevMessage is ChatBox.ReceivedMessage) {
                                        if (currMessage.date.isAfter(prevMessage.date)) {
                                            chatBoxList.add(
                                                index,
                                                ChatBox.DateChip(currMessage.date)
                                            )
                                            currMessage.isFirst = false
                                        }
                                    }
                                }
                            }

                            _state.update { it.copy(messages = chatBoxList) }
                        }


                }
            }
        }
    }
}