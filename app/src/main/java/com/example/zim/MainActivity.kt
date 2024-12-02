package com.example.zim

import WifiP2pBroadcastReceiver
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
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
import com.example.zim.events.ChatsEvent
import com.example.zim.events.ConnectionsEvent
import com.example.zim.events.SignUpEvent
import com.example.zim.events.UserChatEvent
import com.example.zim.navigation.NavGraph
import com.example.zim.ui.theme.ZIMTheme
import com.example.zim.viewModels.ChatsViewModel
import com.example.zim.viewModels.ConnectionsViewModel
import com.example.zim.viewModels.SignUpViewModel
import com.example.zim.viewModels.UserChatViewModel
import com.example.zim.wifiP2P.LocationListener
import com.example.zim.wifiP2P.WifiP2pListener
import com.example.zim.wifiP2Pimport.LocationBroadcastReceiver
import dagger.hilt.android.AndroidEntryPoint
import java.net.InetAddress

@AndroidEntryPoint
class MainActivity : ComponentActivity(), WifiP2pListener, LocationListener {
    private val signUpViewModel: SignUpViewModel by viewModels()
    private val chatsViewModel: ChatsViewModel by viewModels()
    private val connectionsViewModel: ConnectionsViewModel by viewModels()
    private val userChatViewModel: UserChatViewModel by viewModels()

    private lateinit var wifiP2pManager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var intentFilter: IntentFilter
    private lateinit var broadcastReceiver: WifiP2pBroadcastReceiver

    private lateinit var locationIntentFilter: IntentFilter
    private lateinit var locationBroadcastReceiver: LocationBroadcastReceiver

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
        channel = wifiP2pManager.initialize(this, mainLooper, null)

        intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        broadcastReceiver = WifiP2pBroadcastReceiver(wifiP2pManager, channel, this, this)

        checkAndRequestPermissions()
        connectionsViewModel.initWifiP2p(wifiP2pManager, channel)
        userChatViewModel.initWifiP2p(wifiP2pManager, channel)

        val connectionsOnEvent = connectionsViewModel::onEvent
        connectionsOnEvent(ConnectionsEvent.ScanForConnections)

        locationBroadcastReceiver = LocationBroadcastReceiver(this)
        locationIntentFilter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
    }

    override fun onWifiP2pEnabled() {
        connectionsViewModel.updateDependency()
    }

    override fun onWifiP2pDisabled() {
//        Toast.makeText(this@MainActivity, "Wifi is Enabled", Toast.LENGTH_SHORT).show()
        connectionsViewModel.updateDependency()
    }

    override fun onPeersAvailable(peers: Collection<WifiP2pDevice>) {
        val connectionsOnEvent = connectionsViewModel::onEvent
        connectionsOnEvent(ConnectionsEvent.LoadConnections(peers))
//        connectionsOnEvent(ConnectionsEvent.ConnectToUsers)
        (chatsViewModel::onEvent)(ChatsEvent.UpdateStatus(connectionsViewModel.state.value.connectionStatus))
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onConnectionInfoAvailable(info: WifiP2pInfo) {
        val groupOwnerAddress: InetAddress = info.groupOwnerAddress

        if(info.groupFormed && info.isGroupOwner) {
            Toast.makeText(application, "Host Device", Toast.LENGTH_SHORT).show()
            userChatViewModel.makeMeHost()
            userChatViewModel.startServer(8888)
        }
        else if(info.groupFormed) {
            Toast.makeText(application, "Client Device", Toast.LENGTH_SHORT).show()
            groupOwnerAddress.hostAddress?.let { userChatViewModel.connectToServer(it, 8888) }
        }
    }

    override fun onDisconnected() {
//        Toast.makeText(application, "Device Disconnected", Toast.LENGTH_SHORT).show()
    }

    override fun onThisDeviceChanged(device: WifiP2pDevice?) {
//        TODO("Handle device change")
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(broadcastReceiver, intentFilter)
        registerReceiver(locationBroadcastReceiver, locationIntentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(broadcastReceiver)
        unregisterReceiver(locationBroadcastReceiver)
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

    override fun onLocationChange() {
        connectionsViewModel.updateDependency()
    }
}
