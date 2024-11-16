package com.example.zim.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zim.data.room.Dao.UserDao
import com.example.zim.data.room.models.Users
import com.example.zim.events.ConnectionsEvent
import com.example.zim.helperclasses.Connection
import com.example.zim.states.ConnectionsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConnectionsViewModel @Inject constructor(
    private val userDao: UserDao,
) : ViewModel() {
    private val _state = MutableStateFlow(ConnectionsState())

    val state: StateFlow<ConnectionsState> = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        ConnectionsState()
    )

    fun onEvent(event: ConnectionsEvent) {
        when (event) {
            is ConnectionsEvent.AddConnection -> {
                _state.update { it.copy(connections = _state.value.connections + event.newConnection) }
            }

            is ConnectionsEvent.MakeConnection -> {
                addUser(event.connection)
            }

            is ConnectionsEvent.RemoveConnection -> {
                _state.update { it.copy(connections = _state.value.connections - event.connection) }
            }

            is ConnectionsEvent.ScanForConnections -> {
                //wifi direct
                _state.update {
                    it.copy(
                        connections = listOf(
                            Connection(fName = "Asad", lName = "Rehman", description = "Iphone 15"),
                            Connection(fName = "Afroze", lName = "Ali", description = "Iphone 13"),
                            Connection(
                                fName = "Taimoor",
                                lName = "Mohsin",
                                description = "Iphone 10"
                            )
                        )
                    )
                }
            }

            is ConnectionsEvent.HidePrompt -> {
                if (_state.value.promptConnections.isNotEmpty() ) {
                    _state.update { currentState ->

                        val lastConnection =
                            currentState.promptConnections[currentState.promptConnections.size - 1]
                        currentState.copy(promptConnections = currentState.promptConnections - lastConnection)
                    }
                }
            }

            is ConnectionsEvent.ShowPrompt -> {
                if (event.connection !in _state.value.promptConnections) {
                    _state.update { it.copy(promptConnections = _state.value.promptConnections + event.connection) }
                    // Start a timer for 1 minute to remove the connection if it still exists
//                    CoroutineScope(Dispatchers.Default).launch {
//                        delay(60000L) // 1 minute delay (60000 milliseconds)
//                        _state.update { currentState ->
//                            if (event.connection in currentState.promptConnections) {
//                                val updatedConnections = currentState.promptConnections - event.connection
//                                currentState.copy(promptConnections = updatedConnections)
//                            } else {
//                                currentState
//                            }
//                        }
//                    }
                }
            }
        }
    }

    private fun addUser(newConnection: Connection) {
        viewModelScope.launch {
            try {
                userDao.insertUser(
                    Users(
                        fName = newConnection.fName,
                        lName = newConnection.lName,
                        deviceName = newConnection.description
                    )
                )
            } catch (e: Exception) {
                Log.d("addUser", "Error fetching users with latest messages: ${e.message}")

            }
        }
    }
}
