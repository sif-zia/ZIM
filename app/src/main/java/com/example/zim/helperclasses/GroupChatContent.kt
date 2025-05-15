package com.example.zim.helperclasses

import java.time.LocalDateTime

data class GroupChatContent(
    val id: Int = 0,
    val message: String,
    val time: LocalDateTime,
    val isReceived: Boolean,
    val type: String,
    val senderFName: String,
    val senderLName: String? = null,
)
