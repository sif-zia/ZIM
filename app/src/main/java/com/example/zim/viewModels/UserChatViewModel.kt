package com.example.zim.viewModels

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zim.data.room.Dao.MessageDao
import com.example.zim.data.room.Dao.UserDao
import com.example.zim.events.UserChatEvent
import com.example.zim.states.UserChatState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.Q)
@HiltViewModel
class UserChatViewModel @Inject constructor(
    private val userDao: UserDao,
    private val messageDao: MessageDao,
) : ViewModel() {
    private val _state = MutableStateFlow(UserChatState())
    val state: StateFlow<UserChatState> = _state.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000), UserChatState()
    )

    // Track and cancel the previous chat loading job
    private var currentLoadChatJob: Job? = null

    @SuppressLint("MissingPermission")
    fun onEvent(event: UserChatEvent) {
        when (event) {
            is UserChatEvent.LoadData -> {
                if (event.userId != _state.value.userId) {
                    viewModelScope.launch {
                        val user = userDao.getUserById(event.userId)
                        _state.update {
                            it.copy(
                                username = "${user.fName} ${user.lName}",
                                dpUri = user.cover,
                                userId = user.id,
                                uuid = user.UUID,
                                connected = _state.value.connectionStatuses[user.UUID] ?: false,
                                // Clear previous messages while loading
                                messages = emptyList(),
                                // Reset selection state when changing users
                                isSelectionModeActive = false,
                                selectedMessageIds = emptySet()
                            )
                        }
                        loadChats(event.userId)
                    }
                }
            }

            is UserChatEvent.ReadAllMessages -> {
                viewModelScope.launch {
                    messageDao.readAllMessages(event.userId)
                }
            }

            is UserChatEvent.ConnectToUser -> {
                // Implementation for connecting to user
            }

            is UserChatEvent.EnterSelectionMode -> {
                _state.update { it.copy(isSelectionModeActive = true) }
            }

            is UserChatEvent.ExitSelectionMode -> {
                _state.update { it.copy(isSelectionModeActive = false, selectedMessageIds = emptySet()) }
            }

            is UserChatEvent.ToggleMessageSelection -> {
                _state.update { currentState ->
                    val updatedSelection = currentState.selectedMessageIds.toMutableSet()

                    // Toggle selection state
                    if (updatedSelection.contains(event.messageId)) {
                        updatedSelection.remove(event.messageId)
                    } else {
                        updatedSelection.add(event.messageId)
                    }

                    // If no messages are selected after toggling, exit selection mode
                    if (updatedSelection.isEmpty() && currentState.isSelectionModeActive) {
                        currentState.copy(
                            selectedMessageIds = updatedSelection,
                            isSelectionModeActive = false
                        )
                    } else {
                        currentState.copy(selectedMessageIds = updatedSelection)
                    }
                }
            }

            is UserChatEvent.DeleteSelectedMessages -> {
                val selectedIds = _state.value.selectedMessageIds
                if (selectedIds.isNotEmpty()) {
                    viewModelScope.launch {
                        try {
                            // Delete messages from all relevant tables
                            // This will delete the messages from the messages table,
                            // received_messages table, and sent_messages table
                            messageDao.deleteMessagesCompletely(selectedIds.toList())

                            // Exit selection mode after deletion
                            _state.update {
                                it.copy(
                                    isSelectionModeActive = false,
                                    selectedMessageIds = emptySet()
                                )
                            }
                        } catch (e: Exception) {
                            // Handle error (log or show a message)
                            // For now, we'll still exit selection mode
                            _state.update {
                                it.copy(
                                    isSelectionModeActive = false,
                                    selectedMessageIds = emptySet()
                                )
                            }
                        }
                    }
                }
            }

            is UserChatEvent.SelectAllMessages -> {
                viewModelScope.launch {
                    val allMessageIds = _state.value.messages.map { it.id }.toSet()
                    _state.update {
                        it.copy(selectedMessageIds = allMessageIds)
                    }
                }
            }

            is UserChatEvent.DeselectAllMessages -> {
                _state.update {
                    it.copy(selectedMessageIds = emptySet())
                }
            }
        }
    }

    private suspend fun loadChats(userId: Int) {
        // Cancel any existing chat loading job
        currentLoadChatJob?.cancel()

        if (userId != -1) {
            // Start a new job and keep reference to cancel it later if needed
            currentLoadChatJob = viewModelScope.launch {
                messageDao.getAllMessagesOfAUser(userId).collectLatest { chatContentList ->
                    _state.update { it.copy(messages = chatContentList) }
                }
            }
        }
    }
}