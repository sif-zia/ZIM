package com.example.zim.states

import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import com.example.zim.helperclasses.NewConnectionProtocol
import com.example.zim.utils.Package

data class ProtocolState(
    val isWifiEnabled: Boolean = false,
    val isLocationEnabled: Boolean = false,
    val isHotspotEnabled: Boolean = true,

    val groupOwnerIp: String = "192.168.49.1",
    val amIGroupOwner: Boolean? = null,
    val newConnectionProtocol: NewConnectionProtocol? = null,
    val connectionStatues: Map<String, Boolean> = emptyMap(),

    val wifiP2pManager: WifiP2pManager? = null,
    val wifiChannel: Channel?= null,

    val imageArrays: Map<String,List<Package.Type.Image>> = emptyMap(),
    )