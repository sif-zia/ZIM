package com.example.zim.api

import com.example.zim.data.room.models.Users
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val fName: String,
    val lName: String,
    val puKey: String,
)

@Serializable
data class GroupInvite(
    val groupName: String,
    val groupDescription: String,
    val groupSecretKey: String,
    val groupMembers: List<User>,
    val groupAdminPuKey: String,
)
