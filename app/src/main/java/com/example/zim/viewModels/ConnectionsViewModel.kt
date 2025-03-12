package com.example.zim.viewModels

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pDevice
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zim.events.ConnectionsEvent
import com.example.zim.states.ConnectionsState
import com.example.zim.wifiP2P.WifiDirectManager
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
    private val application: Application,
    private val wifiDirectManager: WifiDirectManager
) : ViewModel() {
    private val _state = MutableStateFlow(ConnectionsState())

    val state: StateFlow<ConnectionsState> = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        ConnectionsState()
    )

    init {
        wifiDirectManager.addOnPeersDiscoveredCallback(::onPeersDiscovered)
    }

    private fun onPeersDiscovered(peers: List<WifiP2pDevice>) {
        _state.update { it.copy(connections = peers) }
    }

    @SuppressLint("MissingPermission")
    fun onEvent(event: ConnectionsEvent) {
        when (event) {
            is ConnectionsEvent.AddConnection -> {
                _state.update { it.copy(connections = _state.value.connections + event.newConnection) }
            }

            is ConnectionsEvent.MakeConnection -> {
                onEvent(ConnectionsEvent.HidePrompt)
            }

            is ConnectionsEvent.RemoveConnection -> {
                _state.update { it.copy(connections = _state.value.connections - event.connection) }
            }

            is ConnectionsEvent.HidePrompt -> {
                if (_state.value.promptConnections.isNotEmpty()) {
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
                    CoroutineScope(Dispatchers.Default).launch {
                        delay(60000L) // 1 minute delay
                        try {
                            _state.update { currentState ->
                                if (event.connection in currentState.promptConnections) {
                                    val updatedConnections =
                                        currentState.promptConnections - event.connection
                                    currentState.copy(promptConnections = updatedConnections)
                                } else {
                                    currentState
                                }
                            }
                        } catch (e: Exception) {
                            Log.d("removeConnections", "Error removing Connections: ${e.message}")

                        }
                    }
                }
            }

            is ConnectionsEvent.ScanForConnections -> {
                discoverPeers()
            }

            is ConnectionsEvent.ConnectToDevice -> {
                wifiDirectManager.connect(event.connection)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun discoverPeers() {
        if (!isWifiEnabled()) {
            return
        }

        if (!isLocationEnabled()) {
            return
        }

        wifiDirectManager.discoverPeers()
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager =
            application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun isWifiEnabled(): Boolean {
        val wifiManager = application.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifiManager.isWifiEnabled
    }

    override fun onCleared() {
        super.onCleared()
        wifiDirectManager.removeOnPeersDiscoveredCallback(::onPeersDiscovered)
    }
}
