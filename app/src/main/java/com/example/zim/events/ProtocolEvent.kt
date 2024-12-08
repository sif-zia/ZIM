package com.example.zim.events

sealed interface ProtocolEvent {
    object WifiEnabled : ProtocolEvent
    object WifiDisabled : ProtocolEvent
    object LocationEnabled : ProtocolEvent
    object LocationDisabled : ProtocolEvent
    object HotspotEnabled : ProtocolEvent
    object HotspotDisabled : ProtocolEvent
    object LaunchEnableWifi : ProtocolEvent
    object LaunchEnableLocation : ProtocolEvent
    object LaunchEnableHotspot : ProtocolEvent
}