package com.example.zim.events

import android.net.wifi.p2p.WifiP2pDevice
import com.example.zim.helperclasses.Connection

sealed interface UserChatEvent {
    data class LoadData(val userId: Int): UserChatEvent
    data class ReadAllMessages(val userId: Int): UserChatEvent
    data class ConnectToUser(val userId: Int): UserChatEvent

    object EnterSelectionMode : UserChatEvent
    object ExitSelectionMode : UserChatEvent
    data class ToggleMessageSelection(val messageId: Int) : UserChatEvent
    object DeleteSelectedMessages : UserChatEvent

    object SelectAllMessages : UserChatEvent
    object DeselectAllMessages : UserChatEvent
}