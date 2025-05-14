package com.example.zim.states

import android.net.Uri
import android.net.wifi.p2p.WifiP2pDevice
import com.example.zim.helperclasses.ChatContent
import com.example.zim.helperclasses.ConnectionMetadata

data class UserChatState (
    val messages: List<ChatContent> = emptyList(),
    val username: String = "",
    val connected: Boolean = false,
    val dpUri: Uri? = null,
    val connectionMetadata: ConnectionMetadata = ConnectionMetadata(),
    val protocolStepNumber: Int = 0,
    val amIHost: Boolean = false,
    val myData: ConnectionMetadata = ConnectionMetadata(),
    val peersInRange: Collection<WifiP2pDevice> = emptyList(),
    val userId: Int = -1,
    val uuid: String = "0",
    val connectionStatuses: Map<String, Boolean> = emptyMap(),
    val isNewConnection: Boolean = true,
    val isSelectionModeActive: Boolean = false,
    val selectedMessageIds: Set<Int> = emptySet()
)