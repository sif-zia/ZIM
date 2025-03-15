package com.example.zim.batman

import kotlinx.serialization.Serializable

@Serializable
data class Acknowledgement (
    val messageId: Int,              // Unique ID for the message
    val sourceAddress: String,          // Original sender
    val senderAddress: String,
    val ttl: Int,
    val timestamp: Long = System.currentTimeMillis()
)