package com.example.zim.helperclasses

import java.time.LocalDateTime

data class ChatContent (
    val message: String,
    val time: LocalDateTime,
    val isReceived: Boolean,
    val type: String,
)