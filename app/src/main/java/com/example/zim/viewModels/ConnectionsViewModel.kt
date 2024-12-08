package com.example.zim.viewModels

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.provider.Settings
import android.util.Log
import android.widget.Toast
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
    private val application: Application
) : ViewModel() {
    private var wifiP2pManager: WifiP2pManager? = null
    private var channel: Channel? = null

    private val _state = MutableStateFlow(ConnectionsState())

    val state: StateFlow<ConnectionsState> = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        ConnectionsState()
    )

    init {
        connectToUsers()
    }

    @SuppressLint("MissingPermission")
    fun onEvent(event: ConnectionsEvent) {
        when (event) {
            is ConnectionsEvent.AddConnection -> {
                _state.update { it.copy(connections = _state.value.connections + event.newConnection) }
            }

            is ConnectionsEvent.MakeConnection -> {
                addUser(event.connection)
                onEvent(ConnectionsEvent.HidePrompt)
            }

            is ConnectionsEvent.RemoveConnection -> {
                _state.update { it.copy(connections = _state.value.connections - event.connection) }
            }

            is ConnectionsEvent.LoadConnections -> {
                //wifi direct
                viewModelScope.launch {
                    val deviceAddresses = userDao.getUUIDs()

                    val newPeers = mutableListOf<WifiP2pDevice>()
                    event.peers.forEach{peer ->
                        if (peer.deviceAddress !in deviceAddresses)
                            newPeers.add(peer)
                    }

                    _state.update {
                        it.copy(connections = newPeers.map { peer ->
                            Connection(fName = peer.deviceName, lName = "", description = peer.primaryDeviceType, deviceAddress = peer.deviceAddress)
                        })
                    }
                }

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
                val config = WifiP2pConfig()
                config.deviceAddress = event.connection.deviceAddress
                wifiP2pManager?.connect(channel, config, object: WifiP2pManager.ActionListener {
                    override fun onSuccess() {
//                        addUser(event.connection)
                        Toast.makeText(application, "Connection Request Sent", Toast.LENGTH_SHORT).show()
                    }

                    override fun onFailure(p0: Int) {
                        Toast.makeText(application, "Connection Request Failed", Toast.LENGTH_SHORT).show()
                    }

                })
            }

            is ConnectionsEvent.ConnectToUsers -> {
//                connectToUsers()
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
                        deviceName = newConnection.description,
                        UUID = newConnection.deviceAddress
                    )
                )
            } catch (e: Exception) {
                Log.d("addUser", "Error fetching users with latest messages: ${e.message}")

            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun discoverPeers() {
        if (!isWifiEnabled()) {
            return
        }

        if(!isLocationEnabled()) {
            return
        }


        wifiP2pManager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("ConnectionsViewModel", "Discovery started successfully")
            }

            override fun onFailure(reason: Int) {
                Log.d("ConnectionsViewModel", "Discovery Failed")
                val message = when (reason) {
                    WifiP2pManager.P2P_UNSUPPORTED -> "P2P is unsupported on this device"
                    WifiP2pManager.BUSY -> "System is busy, please retry"
                    WifiP2pManager.ERROR -> "An internal error occurred"
                    WifiP2pManager.NO_SERVICE_REQUESTS -> "No service requests found"
                    else -> "Unknown error code: $reason"
                }
                Toast.makeText(application, message, Toast.LENGTH_SHORT).show()
            }
        })

    }

    fun initWifiP2p(wifiP2pManager: WifiP2pManager, channel: Channel) {
        this.wifiP2pManager = wifiP2pManager
        this.channel = channel
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun isWifiEnabled(): Boolean {
        val wifiManager = application.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifiManager.isWifiEnabled
    }

    @SuppressLint("MissingPermission")
    fun connectToUsers() {
        viewModelScope.launch {
            val deviceAddresses = userDao.getUUIDs()

            deviceAddresses.forEach { deviceAddress ->
                val config = WifiP2pConfig()
                config.deviceAddress = deviceAddress
                wifiP2pManager?.connect(channel, config, object: WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        _state.update { it.copy(connectionStatus = it.connectionStatus+ (deviceAddress to true)) }
                    }

                    override fun onFailure(p0: Int) {
                        _state.update { it.copy(connectionStatus = it.connectionStatus+ (deviceAddress to false)) }
                    }

                })
            }
        }
    }
}
