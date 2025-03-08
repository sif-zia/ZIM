package com.example.zim.api

import kotlinx.serialization.Serializable

//on first connection send Basic info to GO
@Serializable
data class UserData (
    val publicKey:String,
    val fName:String,
    val lName:String
)