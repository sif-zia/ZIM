package com.example.zim.states

import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel

data class ProtocolState(
    val isWifiEnabled: Boolean = false,
    val isLocationEnabled: Boolean = false,
    val isHotspotEnabled: Boolean = true,

    val groupOwnerIp: String = "192.168.49.1",
    val amIGroupOwner: Boolean? = null,

    val wifiP2pManager: WifiP2pManager? = null,
    val wifiChannel: Channel?= null,
)