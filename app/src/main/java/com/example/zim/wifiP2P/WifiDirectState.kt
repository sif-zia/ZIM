package com.example.zim.wifiP2P

import android.net.wifi.p2p.WifiP2pDevice

data class WifiDirectState(
    val isWifiEnabled: Boolean = false,
    val isLocationEnabled: Boolean = false,
    val isHotspotEnabled: Boolean = false,
    val isConnected: Boolean = false,
    val deviceName: String = "",
    val peers: List<WifiP2pDevice> = emptyList(),
    val connectedDevices: List<WifiP2pDevice> = emptyList(),
    val groupOwnerAddress: String? = null,
    val isGroupOwner: Boolean = false
)