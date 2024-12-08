import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.zim.events.ProtocolEvent
import com.example.zim.viewModels.ProtocolViewModel
import com.example.zim.wifiP2P.WifiP2pListener
import javax.inject.Inject

class WifiP2pBroadcastReceiver @Inject constructor(
    private val wifiP2pManager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val locationManager: LocationManager,
    private val listener: WifiP2pListener, // A custom interface for event handling
    private val protocolViewModel: ProtocolViewModel
) : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
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
                    listener.onPeersAvailable(peers.deviceList)
                }
            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                // Connection state has changed
                val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                if (networkInfo?.isConnected == true) {
                    // We are connected, request connection info
                    wifiP2pManager.requestConnectionInfo(channel) { info ->
                        listener.onConnectionInfoAvailable(info)
                    }
                } else {
                    listener.onDisconnected()
                }
            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                // Device details have changed
                val thisDevice = intent.getParcelableExtra<WifiP2pDevice>(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                listener.onThisDeviceChanged(thisDevice)
            }
        }
    }
}
