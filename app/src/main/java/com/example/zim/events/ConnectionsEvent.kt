package com.example.zim.events

import android.net.wifi.p2p.WifiP2pDevice
import com.example.zim.helperclasses.Connection

sealed interface ConnectionsEvent {
    data class AddConnection(val newConnection: WifiP2pDevice) : ConnectionsEvent
    data class RemoveConnection(val connection: WifiP2pDevice) : ConnectionsEvent
    data class MakeConnection(val connection: WifiP2pDevice) : ConnectionsEvent
    data class LoadConnections(val peers: Collection<WifiP2pDevice>) : ConnectionsEvent
    data object ScanForConnections : ConnectionsEvent
    data class ShowPrompt(val connection: WifiP2pDevice) : ConnectionsEvent
    data object HidePrompt : ConnectionsEvent
    data object ConnectToUsers : ConnectionsEvent
    data class ConnectToDevice(val connection: WifiP2pDevice): ConnectionsEvent
}