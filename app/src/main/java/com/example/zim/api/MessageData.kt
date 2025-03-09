package com.example.zim.api

import kotlinx.serialization.Serializable

@Serializable
data class MessageData (
    val sender: String,
    val receiver: String,
    val carrier: String,
    val msg: String
)