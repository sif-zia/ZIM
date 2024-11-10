package com.example.zim.helperclasses

import java.time.LocalDateTime

data class Chat(
    val name: String,
    val lastMsg: String? = null,
    val isConnected: Boolean = false,
    val time: LocalDateTime? = null,
    val isRead: Boolean = false,
    val isDM: Boolean = true,
    val id: Int
)