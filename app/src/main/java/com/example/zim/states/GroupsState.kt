package com.example.zim.states

import com.example.zim.data.room.models.Users
import com.example.zim.helperclasses.Group

data class GroupsState(
    val users: List<Users> = emptyList(),
    val groups: List<Group> = emptyList(),
    val searchQuery: String = "",
)
