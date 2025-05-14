package com.example.zim.events

interface UpdateUserEvent {
    data class UpdateUser(
        val id: Int,
        val deviceName: String? = null,
        val deviceAddress: String? = null,
        val fName: String? = null,
        val lName: String? = null,
        val DOB: String? = null,
        val cover: String? = null, // URI
        val puKey: String? = null
    ) : UpdateUserEvent

    object DeleteUser : UpdateUserEvent
}