package com.example.zim.helperclasses

import java.time.LocalDateTime

data class Chat(
    val fName: String,
    val lName: String,
    val lastMsg: String? = null,
    val isConnected: Boolean? = false,
    val time: LocalDateTime? = null,
    val unReadMsgs: Int = 0,
    val isDM: Boolean? = true,
    val id: Int,
    val UUID: String,
    val lastMsgType: String? = null,
)