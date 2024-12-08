package com.example.zim.viewModels

import android.app.Application
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zim.events.ProtocolEvent
import com.example.zim.states.ProtocolState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ProtocolViewModel @Inject constructor(
    private val application: Application
) : ViewModel() {

    private val _state = MutableStateFlow(ProtocolState())

    val state: StateFlow<ProtocolState> = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        ProtocolState()
    )

    init {
        _state.update { it.copy(isLocationEnabled = (application.getSystemService(Context.LOCATION_SERVICE) as LocationManager).isLocationEnabled) }
    }

    fun onEvent(event: ProtocolEvent) {
        when (event) {
            is ProtocolEvent.LocationEnabled -> {
                _state.update { it.copy(isLocationEnabled = true) }
            }

            is ProtocolEvent.LocationDisabled -> {
                _state.update { it.copy(isLocationEnabled = false) }
            }

            is ProtocolEvent.WifiEnabled -> {
                _state.update { it.copy(isWifiEnabled = true) }
            }

            is ProtocolEvent.WifiDisabled -> {
                _state.update { it.copy(isWifiEnabled = false) }
            }

            is ProtocolEvent.LaunchEnableLocation -> {
                promptEnableLocation()
            }

            is ProtocolEvent.LaunchEnableWifi -> {
                promptEnableWifi()
            }
        }
    }

    fun promptEnableLocation() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(intent)
    }

    fun promptEnableWifi() {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(intent)
    }
}