import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.widget.Toast
import com.example.zim.WIFI_AP_STATE_CHANGED
import com.example.zim.events.ChatsEvent
import com.example.zim.events.ConnectionsEvent
import com.example.zim.events.ProtocolEvent
import com.example.zim.viewModels.ChatsViewModel
import com.example.zim.viewModels.ConnectionsViewModel
import com.example.zim.viewModels.ProtocolViewModel
import java.net.InetAddress
import javax.inject.Inject


class WifiP2pBroadcastReceiver @Inject constructor(
    private val wifiP2pManager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val locationManager: LocationManager,
    private val protocolViewModel: ProtocolViewModel,
    private val chatsViewModel: ChatsViewModel,
    private val connectionsViewModel: ConnectionsViewModel
) : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {

        when (intent.action) {
            WIFI_AP_STATE_CHANGED -> {
                val state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0)

                if (WifiManager.WIFI_STATE_ENABLED == state % 10) {
                    protocolViewModel.onEvent(ProtocolEvent.HotspotEnabled)
                } else {
                    protocolViewModel.onEvent(ProtocolEvent.HotspotDisabled)
                }
            }

            LocationManager.PROVIDERS_CHANGED_ACTION -> {
                // Location provider has changed
                val isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                if (isLocationEnabled) {
                    protocolViewModel.onEvent(ProtocolEvent.LocationEnabled)
                } else {
                    protocolViewModel.onEvent(ProtocolEvent.LocationDisabled)
                }
            }

            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                // Check if Wi-Fi P2P is enabled or disabled

                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    protocolViewModel.onEvent(ProtocolEvent.WifiEnabled)
                } else {
                    protocolViewModel.onEvent(ProtocolEvent.WifiDisabled)
                }
            }
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                wifiP2pManager.requestPeers(channel) { peers ->
                    onPeersAvailable(peers.deviceList)
                }
            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                // Connection state has changed
                val networkInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO, NetworkInfo::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)
                }

                @Suppress("DEPRECATION")
                if (networkInfo?.isConnected == true) {
                    // First get connection info
                    wifiP2pManager.requestConnectionInfo(channel) { info ->
                        val groupOwnerAddress: InetAddress = info.groupOwnerAddress

                        // Then request group info to get device names
                        wifiP2pManager.requestGroupInfo(channel) { group ->
                            val connectedDevices = group?.clientList ?: emptyList()
                            val deviceNames = connectedDevices.map { it.deviceName }
                            val deviceAddresses = connectedDevices.map { it.deviceAddress }

                            if(info.groupFormed && !info.isGroupOwner) {
                                // Client Device
                                protocolViewModel.onEvent(ProtocolEvent.StartClient(
                                    deviceName = deviceNames.firstOrNull(),
                                    deviceAddress = deviceAddresses.firstOrNull(),
                                    groupOwnerIp = groupOwnerAddress.hostAddress
                                ))
                            }
                        }
                    }
                } else {
                    protocolViewModel.onEvent(ProtocolEvent.Disconnect)
                }
            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                // Device details have changed
                val thisDevice = intent.getParcelableExtra<WifiP2pDevice>(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                onThisDeviceChanged(thisDevice)
            }
        }
    }

    fun onThisDeviceChanged(device: WifiP2pDevice?) {
        val deviceName: String? = device?.deviceName

        deviceName?.let {
            protocolViewModel.onEvent(ProtocolEvent.ChangeMyDeviceName(deviceName))
        }
    }

    fun onPeersAvailable(peers: Collection<WifiP2pDevice>) {
        val connectionsOnEvent = connectionsViewModel::onEvent
        connectionsOnEvent(ConnectionsEvent.LoadConnections(peers))
        chatsViewModel.onEvent(ChatsEvent.UpdateStatus(connectionsViewModel.state.value.connectionStatus))
    }
}
