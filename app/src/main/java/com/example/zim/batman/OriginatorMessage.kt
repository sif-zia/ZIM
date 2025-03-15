package com.example.zim.batman

import kotlinx.serialization.Serializable

@Serializable
data class OriginatorMessage(
    val originatorAddress: String,      // MAC address or unique device ID
    val senderAddress: String,
    val sequenceNumber: Int,            // Incremented for each new OGM
    val ttl: Int,                       // Time To Live
    val originalTtl: Int,               // Original TTL value for calculations
    val timestamp: Long = System.currentTimeMillis(),
    val payload: MessagePayload? = null // Optional: actual message being sent
)