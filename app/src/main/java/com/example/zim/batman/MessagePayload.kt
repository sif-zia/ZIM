package com.example.zim.batman

import kotlinx.serialization.Serializable

@Serializable
data class MessagePayload(
    val messageId: String,              // Unique ID for the message
    val sourceAddress: String,          // Original sender
    val destinationAddress: String,     // Final recipient
    val content: String,                // Message content
    val timestamp: Long = System.currentTimeMillis()
)