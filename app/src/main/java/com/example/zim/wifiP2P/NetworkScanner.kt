package com.example.zim.wifiP2P

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.LocalOnlyHotspotCallback
import android.net.wifi.WifiManager.LocalOnlyHotspotReservation
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.zim.api.ClientRepository
import com.example.zim.data.room.Dao.UserDao
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.nearby.connection.ConnectionsClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.ArrayList
import java.util.Collections
import java.util.UUID
import java.util.concurrent.Executor
import java.util.logging.Handler
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A singleton class that scans for devices on connected networks,
 * manages LocalOnlyHotspot functionality, and broadcasts hotspot credentials
 * using Nearby Connections API
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Singleton
class NetworkScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clientRepository: ClientRepository,
    private val userDao: UserDao
) {
    // Use StateFlow to emit discovered devices that can be collected in the UI
    private val _discoveredDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val discoveredDevices: StateFlow<List<DeviceInfo>> = _discoveredDevices.asStateFlow()

    // Status flow to track scanning activity
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Status flow to track hotspot activity
    private val _isHotspotActive = MutableStateFlow(false)
    val isHotspotActive: StateFlow<Boolean> = _isHotspotActive.asStateFlow()

    // Hotspot information flow
    private val _hotspotInfo = MutableStateFlow<HotspotInfo?>(null)
    val hotspotInfo: StateFlow<HotspotInfo?> = _hotspotInfo.asStateFlow()

    // Nearby connections status flow
    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    // Found nearby hotspots
    private val _nearbyHotspots = MutableStateFlow<List<HotspotInfo>>(emptyList())
    val nearbyHotspots: StateFlow<List<HotspotInfo>> = _nearbyHotspots.asStateFlow()

    // Coroutine scope for the scanner
    private val scannerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Tag for logging
    private val TAG = "NetworkScanner"

    // Auto-scan configuration
    private var autoScanEnabled = false
    private var autoScanInterval = 60000L // 1 minute default

    // Hotspot reservation reference
    private var hotspotReservation: LocalOnlyHotspotReservation? = null

    // Nearby Connections variables
    private lateinit var connectionsClient: ConnectionsClient
    private val deviceId = UUID.randomUUID().toString()
    private val SERVICE_ID = "com.example.zim.wifi.hotspot"
    private val STRATEGY = Strategy.P2P_CLUSTER

    private var nearbyApiNodeName = "Unknown Device"

    init {
        Log.d(TAG, "NetworkScanner initialized")
        connectionsClient = Nearby.getConnectionsClient(context)
        enableAutoScan(true)
        // Start discovery automatically when initialized
        startNearbyDiscovery()

        CoroutineScope(Dispatchers.IO).launch {
            userDao.getCurrentUserFlow().collect { currentUser ->
                if(currentUser?.users != null) {
                    val androidVersion = android.os.Build.VERSION.SDK_INT
                    val serviceNamePrefix = if (androidVersion <= android.os.Build.VERSION_CODES.R) "ZIM0" else "ZIM"
                    nearbyApiNodeName = "${serviceNamePrefix}_${currentUser.users.fName}_${currentUser.users.lName}_${currentUser.users.UUID.substring(0, 4)}"
                }
            }
        }
    }

    // Device information data class
    data class DeviceInfo(
        val ipAddress: String,
        val hostname: String? = null,
        val isReachable: Boolean = false,
        val responseTime: Long = 0,
        val deviceName: String? = null,
        val publicKey: String? = null,
    )

    // Hotspot information data class
    data class HotspotInfo(
        val ssid: String,
        val password: String,
        val securityType: String,
        val ipAddress: String? = null,
        val endpointId: String? = null,
        val deviceName: String? = null
    )

    /**
     * Starts the LocalOnlyHotspot and begins advertising credentials
     */
    suspend fun startHotspot() {
        withContext(Dispatchers.Main) {
            turnOnHotspot()
            // We'll start advertising once the hotspot is active (in the callback)
        }
    }

    /**
     * Stops the LocalOnlyHotspot and stops advertising
     */
    suspend fun stopHotspot() {
        withContext(Dispatchers.Main) {
            stopNearbyAdvertising()
            turnOffHotspot()
        }
    }

    /**
     * Performs a single network scan
     */
    suspend fun performSingleScan() {
        scanNetwork()
    }

    /**
     * Returns the current status of all systems
     */
    fun getStatus(): Map<String, Any?> {
        return mapOf(
            "isScanning" to _isScanning.value,
            "isHotspotActive" to _isHotspotActive.value,
            "hotspotInfo" to _hotspotInfo.value,
            "deviceIp" to getDeviceIpAddress(),
            "discoveredDevicesCount" to _discoveredDevices.value.size,
            "isAdvertising" to _isAdvertising.value,
            "isDiscovering" to _isDiscovering.value,
            "nearbyHotspotsCount" to _nearbyHotspots.value.size
        )
    }

    /**
     * Enable or disable auto-scanning with a configurable interval
     */
    fun enableAutoScan(enabled: Boolean, intervalMs: Long = 60000) {
        autoScanEnabled = enabled
        autoScanInterval = intervalMs

        if (enabled && !_isScanning.value) {
            scannerScope.launch {
                startContinuousScan(intervalMs)
            }
        }
    }

    /**
     * Get the device IP address
     */
    private fun getDeviceIpAddress(): String? {
        try {
            val networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (networkInterface in networkInterfaces) {
                val inetAddresses = Collections.list(networkInterface.inetAddresses)
                for (inetAddress in inetAddresses) {
                    if (!inetAddress.isLoopbackAddress && inetAddress.hostAddress?.contains(':') == false) {
                        return inetAddress.hostAddress
                    }
                }
            }
        } catch (e: SocketException) {
            Log.e(TAG, "Error getting device IP address", e)
        }
        return null
    }

    /**
     * Scan for devices on the network
     */
    private suspend fun scanNetwork() {
        if (_isScanning.value) return // Prevent multiple simultaneous scans

        _isScanning.value = true
        Log.d(TAG, "Starting network scan")

        try {
            withContext(Dispatchers.IO) {
                val deviceList = ArrayList<DeviceInfo>()
                val deviceIp = getDeviceIpAddress() ?: return@withContext

                // Extract the subnet from the device IP
                val ipParts = deviceIp.split(".")
                if (ipParts.size != 4) return@withContext

                val subnet = "${ipParts[0]}.${ipParts[1]}.${ipParts[2]}"
                Log.d(TAG, "Scanning subnet: $subnet")

                // Create coroutines for parallel scanning
                val deferreds = (1..254).map { i ->
                    async {
                        val ipToCheck = "$subnet.$i"
                        try {
                            val inetAddress = InetAddress.getByName(ipToCheck)
                            val startTime = System.currentTimeMillis()

                            // Check if device is reachable (timeout 500ms)
                            val reachable = inetAddress.isReachable(500)
                            val responseTime = System.currentTimeMillis() - startTime

                            if (reachable) {
                                val hostname = inetAddress.hostName
                                // Now we can call the suspend function properly
                                val helloData = clientRepository.hello(ipToCheck)

                                val deviceInfo = DeviceInfo(
                                    ipAddress = ipToCheck,
                                    hostname = hostname,
                                    isReachable = true,
                                    responseTime = responseTime,
                                    deviceName = helloData?.name,
                                    publicKey = helloData?.publicKey
                                )

                                Log.d(TAG, "Device found: $deviceInfo")
                                synchronized(deviceList) {
                                    deviceList.add(deviceInfo)
                                }
                            }
                        } catch (e: IOException) {
                            // Just log and continue
                        }
                    }
                }

                // Wait for all coroutines to complete
                deferreds.awaitAll()

                _discoveredDevices.update { ArrayList(deviceList) }
                Log.d(TAG, "Network scan completed. Found ${deviceList.size} devices")
            }
        } finally {
            _isScanning.value = false
        }
    }

    /**
     * Method to perform continuous scanning with interval
     */
    private suspend fun startContinuousScan(intervalMs: Long = autoScanInterval) {
        while (autoScanEnabled) {
            scanNetwork()
            withContext(Dispatchers.IO) {
                try {
                    Thread.sleep(intervalMs)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return@withContext
                }
            }
        }
    }

    /**
     * Start the LocalOnlyHotspot
     */
    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun startLocalOnlyHotspot(
        context: Context,
        executor: Executor,
        callback: WifiManager.LocalOnlyHotspotCallback
    ) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        try {
            // For API level compatibility, use the appropriate method
            wifiManager.startLocalOnlyHotspot(callback, null)  // null Handler for default
            Log.d(TAG, "LocalOnlyHotspot request sent")
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting hotspot: ${e.message}")
            _isHotspotActive.value = false
            callback.onFailed(LocalOnlyHotspotCallback.ERROR_INCOMPATIBLE_MODE)
        }
    }

    /**
     * Turn on the Wi-Fi hotspot
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("MissingPermission")
    private fun turnOnHotspot() {
        try {
            // Start the hotspot with a custom callback
            val hotspotCallback = ZimHotspotCallback()
            startLocalOnlyHotspot(context, context.mainExecutor, hotspotCallback)

            Log.d(TAG, "Requested to turn on hotspot")
        } catch (e: Exception) {
            Log.e(TAG, "Error turning on hotspot: ${e.message}")
        }
    }

    /**
     * Get connected clients to the hotspot
     */
    @RequiresApi(26)
    fun getConnectedClients(): List<DeviceInfo> {
        val connectedDevices = mutableListOf<DeviceInfo>()

        try {
            // Check the ARP cache for connected clients
            val reader = ProcessBuilder("ip", "neigh").start().inputStream.bufferedReader()
            val lines = reader.readLines()

            for (line in lines) {
                // Example line: "192.168.43.5 dev wlan0 lladdr 00:11:22:33:44:55 REACHABLE"
                val parts = line.split(" ")
                if (parts.size >= 4 && parts.contains("REACHABLE")) {
                    val ipAddress = parts[0]

                    // Skip the device's own IP
                    if (ipAddress != getDeviceIpAddress()) {
                        connectedDevices.add(
                            DeviceInfo(
                                ipAddress = ipAddress,
                                isReachable = true
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting connected clients: ${e.message}")
        }

        return connectedDevices
    }

    /**
     * Custom callback class for handling LocalOnlyHotspot events
     */
    private inner class ZimHotspotCallback : WifiManager.LocalOnlyHotspotCallback() {
        override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
            hotspotReservation = reservation
            val config = reservation.wifiConfiguration

            // Log and store the assigned SSID and password
            val actualSsid = config?.SSID ?: "unknown"
            val actualPassword = config?.preSharedKey ?: "unknown"

            Log.d(TAG, "Wifi Hotspot started with system-assigned values:")
            Log.d(TAG, "  SSID: $actualSsid")
            Log.d(TAG, "  Password: $actualPassword")

            // Update the hotspot info state
            if (config != null) {
                // Update the hotspot info state
                _hotspotInfo.value = HotspotInfo(
                    ssid = config.SSID,
                    password = config.preSharedKey,
                    securityType = getSecurityType(config),
                    ipAddress = getDeviceIpAddress()
                )

                // Start advertising the hotspot credentials via Nearby Connections
                startNearbyAdvertising()
            }

            _isHotspotActive.value = true
        }

        override fun onFailed(reason: Int) {
            Log.e(TAG, "Hotspot failed to start, reason: $reason")
            _isHotspotActive.value = false
            _hotspotInfo.value = null
            hotspotReservation = null
        }

        override fun onStopped() {
            Log.d(TAG, "Hotspot stopped")
            _isHotspotActive.value = false
            _hotspotInfo.value = null
            hotspotReservation = null

            // Stop advertising since the hotspot is gone
            stopNearbyAdvertising()
        }
    }

    /**
     * Helper method to get security type as a string
     */
    private fun getSecurityType(config: android.net.wifi.WifiConfiguration): String {
        return when {
            !config.allowedKeyManagement.get(android.net.wifi.WifiConfiguration.KeyMgmt.WPA_PSK) -> "Open"
            config.allowedKeyManagement.get(android.net.wifi.WifiConfiguration.KeyMgmt.WPA2_PSK) -> "WPA2"
            else -> "WPA"
        }
    }

    /**
     * Turn off the hotspot
     */
    private fun turnOffHotspot() {
        try {
            if (hotspotReservation != null) {
                hotspotReservation?.close()
                hotspotReservation = null
                _isHotspotActive.value = false
                _hotspotInfo.value = null
                Log.d(TAG, "Hotspot turned off")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error turning off hotspot: ${e.message}")
        }
    }

    /**
     * Start advertising the hotspot credentials using Nearby Connections API
     */
    @SuppressLint("MissingPermission")
    private fun startNearbyAdvertising() {
        if (_isAdvertising.value) return

        val hotspotInfo = _hotspotInfo.value ?: return

        // Get user info for the payload
        scannerScope.launch {
            // Create the advertising options with user name as the device name
            val advertisingOptions = AdvertisingOptions.Builder()
                .setStrategy(STRATEGY)
                .build()

            // Create a JSON object with the hotspot details
            val hotspotJson = JSONObject().apply {
                put("ssid", hotspotInfo.ssid)
                put("password", hotspotInfo.password)
                put("securityType", hotspotInfo.securityType)
                put("ipAddress", hotspotInfo.ipAddress)
                put("deviceName", nearbyApiNodeName)
            }

            // Convert JSON to bytes for payload
            val hotspotPayload = Payload.fromBytes(hotspotJson.toString().toByteArray())

            // Start advertising
            connectionsClient.startAdvertising(
                nearbyApiNodeName,
                SERVICE_ID,
                object : ConnectionLifecycleCallback() {
                    override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
                        // Automatically accept the connection
                        connectionsClient.acceptConnection(endpointId, object : PayloadCallback() {
                            override fun onPayloadReceived(endpointId: String, payload: Payload) {
                                // We don't expect payloads from connections at this point
                                Log.d(TAG, "Received payload from $endpointId")
                            }

                            override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
                                // Handle payload transfer updates if needed
                                if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
                                    Log.d(TAG, "Hotspot info sent successfully to $endpointId")
                                }
                            }
                        })
                    }

                    override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                        when (result.status.statusCode) {
                            ConnectionsStatusCodes.STATUS_OK -> {
                                // Connection established, send hotspot information
                                connectionsClient.sendPayload(endpointId, hotspotPayload)
                                Log.d(TAG, "Connection established with $endpointId, sending hotspot info")
                            }
                            else -> {
                                Log.d(TAG, "Connection failed with $endpointId: ${result.status.statusCode}")
                            }
                        }
                    }

                    override fun onDisconnected(endpointId: String) {
                        Log.d(TAG, "Disconnected from $endpointId")
                    }
                },
                advertisingOptions
            ).addOnSuccessListener {
                _isAdvertising.value = true
                Log.d(TAG, "Started advertising hotspot credentials")
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to start advertising: ${e.message}")
            }
        }
    }

    /**
     * Stop advertising hotspot credentials
     */
    private fun stopNearbyAdvertising() {
        if (!_isAdvertising.value) return

        connectionsClient.stopAdvertising()
        _isAdvertising.value = false
        Log.d(TAG, "Stopped advertising hotspot credentials")
    }

    /**
     * Start discovering nearby advertised hotspots
     */
    @SuppressLint("MissingPermission")
    fun startNearbyDiscovery() {
        if (_isDiscovering.value) return

        // Create discovery options
        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(STRATEGY)
            .build()

        // Start discovery
        connectionsClient.startDiscovery(
            SERVICE_ID,
            object : EndpointDiscoveryCallback() {
                override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                    // Found an endpoint that's advertising our service
                    Log.d(TAG, "Found endpoint: $endpointId with name: ${info.endpointName}")

                    if(!info.endpointName.startsWith("ZIM_")) {
                        Log.d(TAG, "Ignoring endpoint: $endpointId with name: ${info.endpointName} because does not belong to ZIM app")
                        return
                    }

                    if(info.endpointName.startsWith("ZIM0_")) {
                        Log.d(TAG, "Ignoring endpoint: $endpointId with name: ${info.endpointName} because does not have hotspot")
                        return
                    }

                    if(info.endpointName == nearbyApiNodeName) {
                        Log.d(TAG, "Ignoring own endpoint: $endpointId with name: ${info.endpointName}")
                        return
                    }

                    if(info.endpointName > nearbyApiNodeName) {
                        Log.d(TAG, "Ignoring endpoint: $endpointId with name: ${info.endpointName} because giving him the chance to connect")
                        return
                    }

                    Log.d(TAG, "Requesting connection to endpoint: $endpointId")

                    // Initiate connection to get the hotspot details
                    connectionsClient.requestConnection(
                        nearbyApiNodeName,
                        endpointId,
                        object : ConnectionLifecycleCallback() {
                            override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
                                // Automatically accept connection
                                connectionsClient.acceptConnection(endpointId, object : PayloadCallback() {
                                    override fun onPayloadReceived(endpointId: String, payload: Payload) {
                                        // Process received hotspot information
                                        if (payload.type == Payload.Type.BYTES) {
                                            val payloadBytes = payload.asBytes() ?: return
                                            val jsonString = String(payloadBytes)

                                            try {
                                                val hotspotJson = JSONObject(jsonString)
                                                val hotspotInfo = HotspotInfo(
                                                    ssid = hotspotJson.getString("ssid"),
                                                    password = hotspotJson.getString("password"),
                                                    securityType = hotspotJson.getString("securityType"),
                                                    ipAddress = hotspotJson.optString("ipAddress"),
                                                    endpointId = endpointId,
                                                    deviceName = hotspotJson.optString("deviceName")
                                                )

                                                // Update the list of nearby hotspots
                                                val currentList = _nearbyHotspots.value.toMutableList()
                                                // Replace if existing or add new
                                                val existingIndex = currentList.indexOfFirst { it.endpointId == endpointId }
                                                if (existingIndex >= 0) {
                                                    currentList[existingIndex] = hotspotInfo
                                                } else {
                                                    currentList.add(hotspotInfo)
                                                }
                                                _nearbyHotspots.value = currentList
                                                connectToHotspotByEndpointId(endpointId)

                                                Log.d(TAG, "Received hotspot info: ${hotspotInfo.ssid}")
                                            } catch (e: Exception) {
                                                Log.e(TAG, "Error parsing hotspot info: ${e.message}")
                                            }
                                        }
                                    }

                                    override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
                                        // Handle transfer updates if needed
                                    }
                                })
                            }

                            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                                if (result.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
                                    Log.d(TAG, "Connected to endpoint $endpointId")
                                } else {
                                    Log.d(TAG, "Connection failed with endpoint $endpointId: ${result.status.statusCode}")
                                }
                            }

                            override fun onDisconnected(endpointId: String) {
                                Log.d(TAG, "Disconnected from endpoint $endpointId")

                                // Remove this endpoint from nearbyHotspots when disconnected
                                val currentList = _nearbyHotspots.value.toMutableList()
                                currentList.removeAll { it.endpointId == endpointId }
                                _nearbyHotspots.value = currentList
                            }
                        }
                    ).addOnFailureListener { e ->
                        Log.e(TAG, "Failed to request connection to $endpointId: ${e.message}")
                    }
                }

                override fun onEndpointLost(endpointId: String) {
                    Log.d(TAG, "Lost endpoint: $endpointId")

                    // Remove from nearby hotspots when lost
                    val currentList = _nearbyHotspots.value.toMutableList()
                    currentList.removeAll { it.endpointId == endpointId }
                    _nearbyHotspots.value = currentList
                }
            },
            discoveryOptions
        ).addOnSuccessListener {
            _isDiscovering.value = true
            Log.d(TAG, "Started discovering nearby hotspots")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to start discovery: ${e.message}")
        }
    }

    /**
     * Stop discovering nearby hotspots
     */
    fun stopNearbyDiscovery() {
        if (!_isDiscovering.value) return

        connectionsClient.stopDiscovery()
        _isDiscovering.value = false
        Log.d(TAG, "Stopped discovering nearby hotspots")
    }

    /**
     * Connect to a discovered hotspot by SSID and password
     * Works on Android 10+ by using WiFi suggestion API and system panel as fallback
     */
    @SuppressLint("MissingPermission")
    fun connectToHotspot(hotspotInfo: HotspotInfo) {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

        // Enable WiFi if it's not already enabled
        if (!wifiManager.isWifiEnabled) {
            wifiManager.isWifiEnabled = true
        }

        // For Android 10 (API level 29) and above, use WifiNetworkSpecifier
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val networkSpecifier = WifiNetworkSpecifier.Builder()
                .setSsid(hotspotInfo.ssid)
                .setWpa2Passphrase(hotspotInfo.password)
                .build()

            val networkRequest = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(networkSpecifier)
                .build()

            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    // Network is available
                    // You might want to tell ConnectivityManager to use this network
                    connectivityManager.bindProcessToNetwork(network)
                }
            }

            // Release any previous request
            connectivityManager.requestNetwork(networkRequest, networkCallback)
        } else {
            // For older Android versions
            @Suppress("DEPRECATION")
            val wifiConfiguration = WifiConfiguration().apply {
                SSID = "\"${hotspotInfo.ssid}\""
                preSharedKey = "\"${hotspotInfo.password}\""
                status = WifiConfiguration.Status.ENABLED
                priority = 40
            }

            val networkId = wifiManager.addNetwork(wifiConfiguration)
            wifiManager.disconnect()
            wifiManager.enableNetwork(networkId, true)
            wifiManager.reconnect()
        }
    }


    /**
     * Connect to a specific hotspot by endpoint ID
     */
    fun connectToHotspotByEndpointId(endpointId: String) {
        val hotspot = _nearbyHotspots.value.find { it.endpointId == endpointId }
        if (hotspot != null) {
            connectToHotspot(hotspot)
        } else {
            Log.e(TAG, "No hotspot found with endpoint ID: $endpointId")
        }
    }

    /**
     * Clean up resources when the component is destroyed
     */
    fun onDestroy() {
        autoScanEnabled = false
        stopNearbyDiscovery()
        stopNearbyAdvertising()
        turnOffHotspot()
        scannerScope.launch {
            // Give any ongoing scan a chance to complete
            if (_isScanning.value) {
                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return@launch
                }
            }
        }
    }
}