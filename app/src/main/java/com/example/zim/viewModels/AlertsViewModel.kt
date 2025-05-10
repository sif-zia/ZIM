package com.example.zim.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zim.api.ActiveUserManager
import com.example.zim.api.AlertData
import com.example.zim.api.ClientRepository
import com.example.zim.data.room.Dao.AlertDao
import com.example.zim.data.room.Dao.UserDao
import com.example.zim.data.room.models.Alerts
import com.example.zim.events.AlertsEvent
import com.example.zim.states.AlertsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val clientRepository: ClientRepository,
    private val activeUserManager: ActiveUserManager,
    private val userDao: UserDao,
    private val alertDao: AlertDao
) : ViewModel() {

    private val _state = MutableStateFlow(AlertsState())
    val state: StateFlow<AlertsState> = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        AlertsState()
    )

    init {
        viewModelScope.launch {
            observedLastSendAlert()
        }
        viewModelScope.launch {
            observedReceivedAlerts()
        }
    }

    suspend fun observedReceivedAlerts(){
        alertDao.getAllReceivedAlerts().collect{alerts->
            _state.update { it.copy(receivedAlerts = alerts) }
        }
    }
    suspend fun observedLastSendAlert(){
        alertDao.getLastSentAlert().collect { alert ->
            _state.update { it.copy(lastAlert = alert) }
        }
    }

    fun onEvent(event: AlertsEvent) {
        when (event) {
            is AlertsEvent.SendAlert -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val currentUser = userDao.getCurrentUser()?.users

                    val alertId=alertDao.insertAlert(Alerts(
                        description = event.description,
                        type = event.type,
                        sentTime = event.time,
                        isSent = true,
                    ))

                    if (currentUser != null) {
                        activeUserManager.activeUsers.value.values.forEach { ip ->
                            clientRepository.sendAlert(
                                AlertData(
                                    alertType = event.type,
                                    alertDescription = event.description,
                                    alertTime = event.time,
                                    alertSenderFName = currentUser.fName,
                                    alertSenderLName = currentUser.lName ?: "",
                                    alertSenderPuKey = currentUser.UUID,
                                    alertHops = 0,
                                    alertId=alertId.toInt()
                                ),
                                neighborIp = ip
                            )
                        }
                    }
                }
            }
            is AlertsEvent.ResendAlert -> {
                viewModelScope.launch(Dispatchers.IO) {
                    alertDao.updateAlertTime(event.oldAlert.id)
                    val currentUser = userDao.getCurrentUser()?.users


                    if (currentUser != null) {
                        activeUserManager.activeUsers.value.values.forEach { ip ->
                            clientRepository.sendAlert(
                                AlertData(
                                    alertType = event.oldAlert.type,
                                    alertDescription = event.oldAlert.description,
                                    alertTime = event.oldAlert.sentTime,
                                    alertSenderFName = currentUser.fName,
                                    alertSenderLName = currentUser.lName ?: "",
                                    alertSenderPuKey = currentUser.UUID,
                                    alertHops = 0,
                                    alertId = event.oldAlert.id
                                ),
                                neighborIp = ip
                            )
                        }
                    }
                }
            }
        }
    }
}