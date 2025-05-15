package com.example.zim.helperclasses

import android.net.Uri
import java.time.LocalDateTime

data class Group(
    val groupId: Int,
    val groupName: String,
    val lastMessageTime: LocalDateTime? = null,
    val lastMessage: String? = null,
    val coverUri: Uri? = null,
    val unreadMessages: Int = 0,
    val lastMessageType: String? = null
)
