package com.example.zim.helperclasses

// Data class to hold the query result
data class GroupWithPendingInvite(
    val groupId: Int,
    val groupName: String,
    val groupDescription: String?,
    val groupSecretKey: String?,
    val adminPuKey: String,
    val userId: Int,
    val firstName: String,
    val lastName: String?,
    val puKey: String
)
