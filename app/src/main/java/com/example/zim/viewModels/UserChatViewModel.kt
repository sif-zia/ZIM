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
    private var wifiP2pManager: WifiP2pManager? = null
    private var channel: Channel? = null

    private val _state = MutableStateFlow(UserChatState())
    val state: StateFlow<UserChatState> = _state.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000), UserChatState()
    )

    @SuppressLint("MissingPermission")
    fun onEvent(event: UserChatEvent) {
        when (event) {
            is UserChatEvent.LoadData -> {
                viewModelScope.launch {
                    val user = userDao.getUserById(event.userId)
                    _state.update {
                        it.copy(
                            username = "${user.fName} ${user.lName}",
                            dpUri = user.cover,
                            userId = user.id,
                            uuid = user.UUID,
                            connected = _state.value.connectionStatuses[user.UUID] ?: false
                        )
                    }
                    loadChats(event.userId)
                }
            }

            is UserChatEvent.ReadAllMessages -> {
                viewModelScope.launch {
                    messageDao.readAllMessages(event.userId)
                }
            }

            is UserChatEvent.ConnectToUser -> {

            }
        }
    }

    fun initWifiP2p(wifiP2pManager: WifiP2pManager, channel: Channel) {
        this.wifiP2pManager = wifiP2pManager
        this.channel = channel
    }

    private suspend fun loadChats(userId: Int) {
        if (userId != -1) {
            messageDao.getAllMessagesOfAUser(userId).collect { chatContentList ->
                _state.update { it.copy(messages = chatContentList) }
            }
        }
    }
}