package com.example.zim.events

import android.net.Uri

sealed interface ProtocolEvent {
    data object LaunchEnableWifi : ProtocolEvent
    data object LaunchEnableLocation : ProtocolEvent
    data object LaunchEnableHotspot : ProtocolEvent
    data class SendMessage(val message: String, val id: Int) : ProtocolEvent
    data class AutoConnect(val userId: Int) : ProtocolEvent
    data class SendImage(val imageUri: Uri, val userId: Int): ProtocolEvent
}