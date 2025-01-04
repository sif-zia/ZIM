package com.example.zim.viewModels

import android.app.Application
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zim.data.room.Dao.UserDao
import com.example.zim.events.ProtocolEvent
import com.example.zim.states.ProtocolState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProtocolViewModel @Inject constructor(
    private val application: Application,
    private val userDao: UserDao
) : ViewModel() {

    private val _state = MutableStateFlow(ProtocolState())

    val state: StateFlow<ProtocolState> = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        ProtocolState()
    )

    init {
        _state.update {
            it.copy(
                isLocationEnabled = isLocationEnabled(),
                isHotspotEnabled = isHotspotEnabled(),
            )
        }
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

            is ProtocolEvent.HotspotEnabled -> {
                _state.update { it.copy(isHotspotEnabled = true) }
            }

            is ProtocolEvent.HotspotDisabled -> {
                _state.update { it.copy(isHotspotEnabled = false) }
            }

            is ProtocolEvent.LaunchEnableLocation -> {
                promptEnableLocation()
            }

            is ProtocolEvent.LaunchEnableWifi -> {
                promptEnableWifi()
            }

            is ProtocolEvent.LaunchEnableHotspot -> {
                promptEnableHotspot()
            }

            is ProtocolEvent.ChangeMyDeviceName -> {
                viewModelScope.launch {
                    userDao.setCurrUserDeviceName(event.newDeviceName);
                }
            }
        }
    }

    private fun promptEnableLocation() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(intent)
    }

    private fun promptEnableWifi() {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(intent)
    }

    private fun promptEnableHotspot() {
        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(intent)
    }

    private fun isHotspotEnabled(): Boolean {
        return try {
            val wifiManager = application.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val method = wifiManager.javaClass.getDeclaredMethod("isWifiApEnabled")
            method.isAccessible = true
            method.invoke(wifiManager) as Boolean
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

}