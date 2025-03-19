package com.example.zim.viewModels

import android.annotation.SuppressLint
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zim.data.room.Dao.MessageDao
import com.example.zim.data.room.Dao.UserDao
import com.example.zim.events.UserChatEvent
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

    // Add this to track and cancel the previous chat loading job
    private var currentLoadChatJob: kotlinx.coroutines.Job? = null

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
                                messages = emptyList()
                            )
                        }
                        loadChats(event.userId)
                    }
                }
            }

            // Other events remain the same
            is UserChatEvent.ReadAllMessages -> {
                viewModelScope.launch {
                    messageDao.readAllMessages(event.userId)
                }
            }

            is UserChatEvent.ConnectToUser -> {
                // Implementation for connecting to user
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