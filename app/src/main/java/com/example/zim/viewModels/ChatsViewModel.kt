package com.example.zim.viewModels

import android.util.Log
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
    private val userDao: UserDao
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
            try {
                userDao.getUsersWithLatestMessage(query).collectLatest { chats ->
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


}