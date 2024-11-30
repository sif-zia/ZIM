package com.example.zim.events

import android.net.wifi.p2p.WifiP2pDevice
import com.example.zim.helperclasses.Connection

sealed interface ConnectionsEvent {
    data class AddConnection(val newConnection: Connection) : ConnectionsEvent
    data class RemoveConnection(val connection: Connection) : ConnectionsEvent
    data class MakeConnection(val connection: Connection) : ConnectionsEvent
    data class LoadConnections(val peers: Collection<WifiP2pDevice>) : ConnectionsEvent
    data object ScanForConnections : ConnectionsEvent
    data class ShowPrompt(val connection: Connection) : ConnectionsEvent
    data object HidePrompt : ConnectionsEvent
}