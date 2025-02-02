package com.example.zim.states

import com.example.zim.helperclasses.NewConnectionProtocol

data class ProtocolState(
    val isWifiEnabled: Boolean = false,
    val isLocationEnabled: Boolean = false,
    val isHotspotEnabled: Boolean = true,

    val groupOwnerIp: String = "192.168.49.1",
    val amIGroupOwner: Boolean? = null,
    val newConnectionProtocol: NewConnectionProtocol? = null,
    val connectionStatues: Map<String, Boolean> = emptyMap(),
)