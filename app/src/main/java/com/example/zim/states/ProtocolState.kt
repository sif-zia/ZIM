package com.example.zim.states

data class ProtocolState(
    val isWifiEnabled: Boolean = false,
    val isLocationEnabled: Boolean = false,
    val isHotspotEnabled: Boolean = true,
)