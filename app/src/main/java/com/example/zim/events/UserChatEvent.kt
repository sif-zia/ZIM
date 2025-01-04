package com.example.zim.events

import android.net.wifi.p2p.WifiP2pDevice
import com.example.zim.helperclasses.Connection

sealed interface UserChatEvent {
    data class LoadData(val userId: Int): UserChatEvent
    data class SendMessage(val message: String): UserChatEvent
    data class ReadAllMessages(val userId: Int): UserChatEvent
    data class ConnectToUser(val userId: Int): UserChatEvent
}