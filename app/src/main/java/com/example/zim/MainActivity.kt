package com.example.zim

import WifiP2pBroadcastReceiver
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import com.example.zim.api.ClientRepository
import com.example.zim.api.ServerRepository
import com.example.zim.events.ChatsEvent
import com.example.zim.events.ConnectionsEvent
import com.example.zim.events.ProtocolEvent
import com.example.zim.events.SignUpEvent
import com.example.zim.events.UserChatEvent
import com.example.zim.navigation.NavGraph
import com.example.zim.ui.theme.ZIMTheme
import com.example.zim.viewModels.ChatsViewModel
import com.example.zim.viewModels.ConnectionsViewModel
import com.example.zim.viewModels.ProtocolViewModel
import com.example.zim.viewModels.SignUpViewModel
import com.example.zim.viewModels.UserChatViewModel
import com.example.zim.wifiP2P.WifiP2pListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.net.InetAddress
import javax.inject.Inject

val WIFI_AP_STATE_CHANGED = "android.net.wifi.WIFI_AP_STATE_CHANGED"

@AndroidEntryPoint
class MainActivity : ComponentActivity(), WifiP2pListener {
    @Inject
    lateinit var clientRepository: ClientRepository

    private val signUpViewModel: SignUpViewModel by viewModels()
    private val chatsViewModel: ChatsViewModel by viewModels()
    private val connectionsViewModel: ConnectionsViewModel by viewModels()
    private val userChatViewModel: UserChatViewModel by viewModels()
    private val protocolViewModel: ProtocolViewModel by viewModels()

    private lateinit var wifiP2pManager: WifiP2pManager
    private lateinit var locationManager: LocationManager
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var intentFilter: IntentFilter
    private lateinit var broadcastReceiver: WifiP2pBroadcastReceiver

    private var permissionsGranted = false


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.NEARBY_WIFI_DEVICES,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.CHANGE_NETWORK_STATE

    )

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ZIMTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background

                ) {

                    NavGraph(
                        chatsViewModel,
                        signUpViewModel,
                        connectionsViewModel,
                        userChatViewModel
                    )
                }
            }
        }

        wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        channel = wifiP2pManager.initialize(this, mainLooper, null)

        intentFilter = IntentFilter().apply {
            addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            addAction(WIFI_AP_STATE_CHANGED)
        }

        broadcastReceiver = WifiP2pBroadcastReceiver(wifiP2pManager, channel, locationManager, this, protocolViewModel)

        checkAndRequestPermissions()
        connectionsViewModel.initWifiP2p(wifiP2pManager, channel)
        userChatViewModel.initWifiP2p(wifiP2pManager, channel)
        protocolViewModel.initWifiManager(wifiP2pManager,channel)
    }

    override fun onPeersAvailable(peers: Collection<WifiP2pDevice>) {
        val connectionsOnEvent = connectionsViewModel::onEvent
        connectionsOnEvent(ConnectionsEvent.LoadConnections(peers))
        chatsViewModel.onEvent(ChatsEvent.UpdateStatus(connectionsViewModel.state.value.connectionStatus))
    }

    override fun onDisconnected() {
//        Toast.makeText(application, "Device Disconnected", Toast.LENGTH_SHORT).show()
    }

    override fun onThisDeviceChanged(device: WifiP2pDevice?) {
        val deviceName: String? = device?.deviceName

        deviceName?.let {
            protocolViewModel.onEvent(ProtocolEvent.ChangeMyDeviceName(deviceName))
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(broadcastReceiver)
    }

    private val PERMISSION_REQUEST_CODE = 1001

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = REQUIRED_PERMISSIONS.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }

        permissionsGranted = permissionsToRequest.isEmpty()
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val deniedPermissions = permissions.filterIndexed { index, _ ->
                grantResults[index] != PackageManager.PERMISSION_GRANTED
            }

            if (deniedPermissions.isNotEmpty()) {
                Toast.makeText(this, "Permissions denied: $deniedPermissions", Toast.LENGTH_SHORT)
                    .show()
                permissionsGranted = false
            } else {
                Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()
                permissionsGranted = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        wifiP2pManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
            }

            override fun onFailure(reason: Int) {}
        })
    }
}
