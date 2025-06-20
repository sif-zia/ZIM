package com.example.zim.wifiP2P

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.widget.Toast
import com.example.zim.api.UserData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiDirectManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wifiP2pManager: WifiP2pManager,
    private val locationManager: LocationManager
) {
    private val _state = MutableStateFlow(WifiDirectState())
    val state: StateFlow<WifiDirectState> = _state.asStateFlow()
    private var bridgeScope: CoroutineScope? = null

    // Individual callback functions
    private var onWifiStateChangedCallbacks = mutableListOf<(Boolean) -> Unit>()
    private var onLocationStateChangedCallbacks = mutableListOf<(Boolean) -> Unit>()
    private var onHotspotStateChangedCallbacks = mutableListOf<(Boolean) -> Unit>()
    private var onPeersDiscoveredCallbacks = mutableListOf<(List<WifiP2pDevice>) -> Unit>()
    private var onDeviceNameChangedCallbacks = mutableListOf<(String) -> Unit>()
    private var onStartConnectionCallbacks = mutableListOf<(WifiP2pInfo, WifiP2pGroup, Int) -> Unit>()
    private var onDisconnectCallbacks = mutableListOf<() -> Unit>()

    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var receiver: WifiDirectBroadcastReceiver

    init {
        initializeWifiDirect()
    }

    private fun initializeWifiDirect() {
        channel = wifiP2pManager.initialize(context, context.mainLooper, null)
        receiver = WifiDirectBroadcastReceiver()
    }

    // Methods to register individual callbacks
    fun addOnWifiStateChangedCallback(callback: (Boolean) -> Unit) {
        onWifiStateChangedCallbacks.add(callback)
    }

    fun addOnLocationStateChangedCallback(callback: (Boolean) -> Unit) {
        onLocationStateChangedCallbacks.add(callback)
    }

    fun addOnHotspotStateChangedCallback(callback: (Boolean) -> Unit) {
        onHotspotStateChangedCallbacks.add(callback)
    }

    fun addOnPeersDiscoveredCallback(callback: (List<WifiP2pDevice>) -> Unit) {
        onPeersDiscoveredCallbacks.add(callback)
    }

    fun addOnDeviceNameChangedCallback(callback: (String) -> Unit) {
        onDeviceNameChangedCallbacks.add(callback)
    }

    fun addOnConnectionCallback(callback: (WifiP2pInfo, WifiP2pGroup, Int) -> Unit) {
        onStartConnectionCallbacks.add(callback)
    }

    fun addOnDisconnectCallback(callback: () -> Unit) {
        onDisconnectCallbacks.add(callback)
    }

    // Methods to remove individual callbacks
    fun removeOnWifiStateChangedCallback(callback: (Boolean) -> Unit) {
        onWifiStateChangedCallbacks.remove(callback)
    }

    fun removeOnLocationStateChangedCallback(callback: (Boolean) -> Unit) {
        onLocationStateChangedCallbacks.remove(callback)
    }

    fun removeOnHotspotStateChangedCallback(callback: (Boolean) -> Unit) {
        onHotspotStateChangedCallbacks.remove(callback)
    }

    fun removeOnPeersDiscoveredCallback(callback: (List<WifiP2pDevice>) -> Unit) {
        onPeersDiscoveredCallbacks.remove(callback)
    }

    fun removeOnDeviceNameChangedCallback(callback: (String) -> Unit) {
        onDeviceNameChangedCallbacks.remove(callback)
    }

    fun removeOnConnectionCallback(callback: (WifiP2pInfo, WifiP2pGroup, Int) -> Unit) {
        onStartConnectionCallbacks.remove(callback)
    }

    fun removeOnDisconnectCallback(callback: () -> Unit) {
        onDisconnectCallbacks.remove(callback)
    }

    fun registerBroadcastReceiver() {
        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
            addAction(WIFI_AP_STATE_CHANGED)
        }
        context.registerReceiver(receiver, intentFilter)
        startBridgeMode()
    }

    fun unregisterBroadcastReceiver() {
        try {
            context.unregisterReceiver(receiver)
            stopBridgeMode()
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
    }

    @SuppressLint("MissingPermission")
    fun discoverPeers(onSuccess: () -> Unit = {}, onFailure: (Int) -> Unit = {}) {
        wifiP2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                onSuccess()
            }

            override fun onFailure(reason: Int) {
                onFailure(reason)
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun connect(device: WifiP2pDevice, onSuccess: () -> Unit = {}, onFailure: (Int) -> Unit = {}) {
        if (device.status == WifiP2pDevice.CONNECTED) {
            wifiP2pManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Toast.makeText(context, "Disconnected", Toast.LENGTH_LONG).show()

                    val config = WifiP2pConfig().apply {
                        deviceAddress = device.deviceAddress
                    }

                    wifiP2pManager.connect(
                        channel,
                        config,
                        object : WifiP2pManager.ActionListener {
                            override fun onSuccess() {
                                Toast.makeText(
                                    context,
                                    "Connecting to ${device.deviceName}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onSuccess()
                            }

                            override fun onFailure(reason: Int) {
                                Toast.makeText(
                                    context,
                                    "Connection Request Failed",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onFailure(reason)
                            }
                        }
                    )
                }

                override fun onFailure(reason: Int) {
                    onFailure(reason)
                }
            })
        } else {
            val config = WifiP2pConfig().apply {
                deviceAddress = device.deviceAddress
            }

            wifiP2pManager.connect(
                channel,
                config,
                object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Toast.makeText(
                            context,
                            "Connecting to ${device.deviceName}",
                            Toast.LENGTH_SHORT
                        ).show()
                        onSuccess()
                    }

                    override fun onFailure(reason: Int) {
                        Toast.makeText(
                            context,
                            "Connection Request Failed",
                            Toast.LENGTH_SHORT
                        ).show()
                        onFailure(reason)
                    }
                }
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun connectByName(deviceName: String?, onSuccess: () -> Unit = {}, onFailure: (String) -> Unit = {}) {
        if(deviceName.isNullOrEmpty()) return

        // First check if we have any peers
        val currentPeers = _state.value.peers
        if (currentPeers.isEmpty()) return

        // Find the device with the matching name
        val targetDevice = currentPeers.find { it.deviceName == deviceName }
        if (targetDevice == null) {
            onFailure("Device '$deviceName' not found among available peers")
            return
        }

        // Connect to the found device
        connect(
            device = targetDevice,
            onSuccess = onSuccess,
            onFailure = { reason ->
                onFailure("Connection failed with reason code: $reason")
            }
        )
    }

    fun disconnect(onSuccess: () -> Unit = {}, onFailure: (Int) -> Unit = {}) {
        wifiP2pManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                _state.update {
                    it.copy(
                        isConnected = false,
                        groupOwnerAddress = null,
                        isGroupOwner = false,
                        connectedDevices = emptyList()
                    )
                }
                notifyDisconnect()
                onSuccess()
            }

            override fun onFailure(reason: Int) {
                onFailure(reason)
            }
        })
    }

    private fun notifyWifiStateChanged(enabled: Boolean) {
        onWifiStateChangedCallbacks.forEach { it(enabled) }
    }

    private fun notifyLocationStateChanged(enabled: Boolean) {
        onLocationStateChangedCallbacks.forEach { it(enabled) }
    }

    private fun notifyHotspotStateChanged(enabled: Boolean) {
        onHotspotStateChangedCallbacks.forEach { it(enabled) }
    }

    private fun notifyPeersDiscovered(peers: List<WifiP2pDevice>) {
        onPeersDiscoveredCallbacks.forEach { it(peers) }
    }

    private fun notifyDeviceNameChanged(deviceName: String) {
        onDeviceNameChangedCallbacks.forEach { it(deviceName) }
    }

    private fun notifyConnected(info: WifiP2pInfo, group: WifiP2pGroup, size: Int) {
        onStartConnectionCallbacks.forEach { it(info, group, size) }
    }

    private fun notifyDisconnect() {
        onDisconnectCallbacks.forEach { it() }
    }

    inner class WifiDirectBroadcastReceiver : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WIFI_AP_STATE_CHANGED -> {
                    val state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0)
                    val isHotspotEnabled = WifiManager.WIFI_STATE_ENABLED == state % 10

                    _state.update { it.copy(isHotspotEnabled = isHotspotEnabled) }
                    notifyHotspotStateChanged(isHotspotEnabled)

                    if(!isBridgeActive())
                        startBridgeMode()
                }

                LocationManager.PROVIDERS_CHANGED_ACTION -> {
                    val isLocationEnabled =
                        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                    _state.update { it.copy(isLocationEnabled = isLocationEnabled) }
                    notifyLocationStateChanged(isLocationEnabled)

                    if(!isBridgeActive())
                        startBridgeMode()
                }

                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    val isWifiEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED

                    _state.update { it.copy(isWifiEnabled = isWifiEnabled) }
                    notifyWifiStateChanged(isWifiEnabled)

                    if(!isBridgeActive())
                        startBridgeMode()
                }

                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    wifiP2pManager.requestPeers(channel) { peerList ->
                        val peers = peerList.deviceList.toList()
                        _state.update { it.copy(peers = peers) }
                        notifyPeersDiscovered(peers)
                    }
                }

                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val networkInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(
                            WifiP2pManager.EXTRA_NETWORK_INFO,
                            NetworkInfo::class.java
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)
                    }

                    @Suppress("DEPRECATION")
                    if (networkInfo?.isConnected == true) {
                        wifiP2pManager.requestConnectionInfo(channel) { info ->
                            val groupOwnerAddress = info.groupOwnerAddress?.hostAddress
                            val isConnected = info.groupFormed
                            val isGroupOwner = info.isGroupOwner

                            _state.update {
                                it.copy(
                                    isConnected = isConnected,
                                    groupOwnerAddress = groupOwnerAddress,
                                    isGroupOwner = isGroupOwner
                                )
                            }

                            wifiP2pManager.requestGroupInfo(channel) { group ->
                                val clientList = group.clientList
                                notifyConnected(info, group, clientList.size)
                            }
                        }
                    } else {
                        _state.update {
                            it.copy(
                                isConnected = false,
                                groupOwnerAddress = null,
                                isGroupOwner = false,
                                connectedDevices = emptyList()
                            )
                        }
                        notifyDisconnect()
                    }
                }

                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(
                            WifiP2pManager.EXTRA_WIFI_P2P_DEVICE,
                            WifiP2pDevice::class.java
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                    }

                    device?.deviceName?.let { deviceName ->
                        _state.update { it.copy(deviceName = deviceName) }
                        notifyDeviceNameChanged(deviceName)
                    }
                }
            }
        }
    }

    fun addConnectedDevice(device: UserData) {
        _state.update {
            it.copy(connectedDevices = it.connectedDevices + device)
        }
    }

    fun removeConnectedDevice(device: UserData) {
        _state.update {
            it.copy(connectedDevices = it.connectedDevices - device)
        }
    }

    fun removeConnectedDevice(publicKey: String) {
        _state.update {
            it.copy(connectedDevices = it.connectedDevices.filter { it.publicKey != publicKey })
        }
    }

    fun getConnectedDevices(): List<UserData> {
        return _state.value.connectedDevices
    }

    @SuppressLint("MissingPermission")
    fun startBridgeMode() {
        // Cancel existing scope if running
        bridgeScope?.cancel()
        bridgeScope = CoroutineScope(Dispatchers.IO)

        bridgeScope!!.launch {
            while (isActive) {
//                try {
//                    // Only non-group owners should act as bridges
//                    if (_state.value.isWifiEnabled && _state.value.isConnected) {
//                        Log.d("WifiDirectManager", "Bridge mode: Scanning for new groups")
//
//                        // Discover available peers
//                        discoverPeers()
//                        // Give discovery time to find peers
//                        delay(5000L)
//
//                        val currentPeers = _state.value.peers
//                        val connectedDeviceNames = _state.value.connectedDevices.map { it.deviceName }
//
//                        // Filter out devices that are already connected
//                        val availableDevices = currentPeers.filter {
//                            !connectedDeviceNames.contains(it.deviceName) &&
//                                    it.status != WifiP2pDevice.CONNECTED
//                        }
//                        Log.d("WifiDirectManager", "Available devices: ${availableDevices.size}")
//
//                        if (availableDevices.isNotEmpty() && _state.value.isConnected) {
//                            // Select a device to connect to
//                            val targetDevice = availableDevices.firstOrNull()
//
//                            targetDevice?.let {
//                                Log.d("WifiDirectManager", "Bridge mode: Planning to switch to ${it.deviceName}")
//
//                                var disconnectSuccessful = true
//                                // First disconnect fully, with a completion handler
//                                if(!state.value.isGroupOwner) {
//                                    disconnectSuccessful =
//                                        suspendCancellableCoroutine<Boolean> { continuation ->
//                                            disconnect(
//                                                onSuccess = { continuation.resume(true) },
//                                                onFailure = { reason ->
//                                                    Log.d(
//                                                        "WifiDirectManager",
//                                                        "Bridge mode: Disconnect failed: $reason"
//                                                    )
//                                                    continuation.resume(false)
//                                                }
//                                            )
//                                        }
//                                }
//
//                                // Add delay to allow the system to stabilize after disconnect
//                                delay(100L)
//
//                                // Only attempt to connect if disconnect was successful
//                                if (disconnectSuccessful) {
//                                    val connectSuccessful = suspendCancellableCoroutine<Boolean> { continuation ->
//                                        connect(
//                                            device = it,
//                                            onSuccess = {
//                                                Log.d("WifiDirectManager", "Bridge mode: Connected to ${it.deviceName}")
//                                                continuation.resume(true)
//                                            },
//                                            onFailure = { reason ->
//                                                Log.d("WifiDirectManager", "Bridge mode: Connection failed: $reason")
//                                                continuation.resume(false)
//                                            }
//                                        )
//                                    }
//
//                                    // If connection successful, wait longer before next attempt
//                                    if (connectSuccessful) {
//                                        delay(BRIDGING_INTERVAL)
//                                    }
//                                }
//                            }
//                        } else if (!_state.value.isConnected && availableDevices.isNotEmpty()) {
//                            // We're not connected to anything, try to connect directly
//                            val targetDevice = availableDevices.firstOrNull()
//                            targetDevice?.let {
//                                Log.d("WifiDirectManager", "Bridge mode: Attempting first connection to ${it.deviceName}")
//                                connect(
//                                    device = it,
//                                    onSuccess = {
//                                        Log.d("WifiDirectManager", "Bridge mode: Connected to ${it.deviceName}")
//                                    },
//                                    onFailure = { reason ->
//                                        Log.d("WifiDirectManager", "Bridge mode: Connection failed: $reason")
//                                    }
//                                )
//                                // Wait before next cycle
//                                delay(BRIDGING_INTERVAL)
//                            }
//                        }
//                    }
//                } catch (e: Exception) {
//                    Log.e("WifiDirectManager", "Bridge mode error: ${e.message}", e)
//                }
            }
        }
    }

    // Add a function to stop bridge mode
    private fun stopBridgeMode() {
        bridgeScope?.cancel()
        bridgeScope = null
    }

    fun isBridgeActive(): Boolean {
        return bridgeScope != null && bridgeScope?.isActive == true
    }

    companion object {
        private const val WIFI_AP_STATE_CHANGED = "android.net.wifi.WIFI_AP_STATE_CHANGED"
        private const val BRIDGING_INTERVAL = 30000L
    }

}