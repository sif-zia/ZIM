package com.example.zim.api

import kotlinx.serialization.Serializable

@Serializable
data class HelloData (
    val name: String,
    val publicKey: String,
)