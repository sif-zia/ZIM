package com.example.zim.states

import com.example.zim.helperclasses.Chat

data class ChatsState (
    val menuExpanded: Boolean = false,
    val query: String = "",
    val chats: List<Chat> = emptyList(),
    val unReadMsgs: Int = 0
)