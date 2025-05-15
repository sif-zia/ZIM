package com.example.zim.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zim.data.room.Dao.UserDao
import com.example.zim.states.GroupsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val userDao: UserDao
): ViewModel() {
    private val _state = MutableStateFlow(GroupsState())
    val state: StateFlow<GroupsState> = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        GroupsState()
    )

    init {
        viewModelScope.launch {
            userDao.getConnectedUsers().collect { users ->
                _state.value = _state.value.copy(users = users)
            }
        }
    }
}