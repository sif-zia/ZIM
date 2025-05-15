package com.example.zim.states

import com.example.zim.data.room.models.Users

data class GroupsState(
    val users: List<Users> = emptyList(),
)
