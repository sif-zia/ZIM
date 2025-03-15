package com.example.zim.batman

import kotlinx.serialization.Serializable

@Serializable
data class MessagePayload(
    val messageId: Int,              // Unique ID for the message
    val sourceAddress: String,          // Original sender
    val destinationAddress: String,     // Final recipient
    val senderAddress: String,
    val content: String,                // Message content
    val ttl: Int,
    val timestamp: Long = System.currentTimeMillis()
)