package com.example.zim.events

sealed interface ProtocolEvent {
    data object WifiEnabled : ProtocolEvent
    data object WifiDisabled : ProtocolEvent
    data object LocationEnabled : ProtocolEvent
    data object LocationDisabled : ProtocolEvent
    data object HotspotEnabled : ProtocolEvent
    data object HotspotDisabled : ProtocolEvent
    data object LaunchEnableWifi : ProtocolEvent
    data object LaunchEnableLocation : ProtocolEvent
    data object LaunchEnableHotspot : ProtocolEvent
    data class ChangeMyDeviceName(val newDeviceName: String) : ProtocolEvent
}