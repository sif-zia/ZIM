package com.example.zim.states

import com.example.zim.helperclasses.Connection

data class ConnectionsState(
    val connections: List<Connection> = emptyList(),
    val promptConnections:List<Connection> = emptyList(),
    val connectionStatus: Map<String, Boolean> = emptyMap()
)