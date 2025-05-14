package com.example.zim.helperclasses

import androidx.room.ColumnInfo
import java.time.LocalDateTime

data class ChatContent (
    @ColumnInfo(name = "id")
    val id: Int = 0,
    val message: String,
    val time: LocalDateTime,
    val isReceived: Boolean,
    val type: String,
)