package com.example.zim.api

import kotlinx.serialization.Serializable

@Serializable
data class MessageData (
    val messageId: Int,
    val content: String
)