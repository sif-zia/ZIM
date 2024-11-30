package com.example.zim.states

import android.net.Uri
import com.example.zim.helperclasses.ChatBox

data class UserChatState (
    val messages: List<ChatBox> = emptyList(),
    val username: String = "",
    val connected: Boolean = true,
    val dpUri: Uri? = null
)