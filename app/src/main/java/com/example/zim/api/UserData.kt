package com.example.zim.api

import kotlinx.serialization.Serializable
import java.util.UUID

//on first connection send Basic info to GO
@Serializable
data class UserData (
    val id: String = UUID.randomUUID().toString(),
    val publicKey:String,
    val fName:String,
    val lName:String?,
    val deviceName: String?,
    val isGroupOwner: Boolean = false,
)