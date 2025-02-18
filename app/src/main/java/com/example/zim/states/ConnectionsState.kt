package com.example.zim.states

import android.net.wifi.p2p.WifiP2pDevice
import com.example.zim.helperclasses.Connection

data class ConnectionsState(
    val connections: List<WifiP2pDevice> = emptyList(),
    val promptConnections:List<WifiP2pDevice> = emptyList(),
    val connectionStatus: Map<String, Boolean> = emptyMap()
)