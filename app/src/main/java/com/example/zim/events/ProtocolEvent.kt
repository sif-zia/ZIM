package com.example.zim.events

import android.net.Uri

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
    data class SendMessage(val message: String, val id: Int) : ProtocolEvent
    data class AutoConnect(val userId: Int) : ProtocolEvent
    data class SendImage(val imageUri: Uri, val userId: Int): ProtocolEvent

    data class StartServer(val deviceName: String?, val deviceAddress: String?, val groupOwnerIp: String?) : ProtocolEvent
    data class StartClient(val deviceName: String?, val deviceAddress: String?, val groupOwnerIp: String?) : ProtocolEvent
    data object InitServer : ProtocolEvent
}