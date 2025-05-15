package com.example.zim.events

import com.example.zim.data.room.models.Users

sealed interface GroupsEvent {
    data class AddGroup(val groupName: String, val groupMembers: List<Users>, val groupDescription: String) : GroupsEvent
    data class UpdateSearchQuery(val query: String) : GroupsEvent
    data class LoadGroupData(val groupId: Int) : GroupsEvent
    data class SendGroupMessage(val groupId: Int, val message: String) : GroupsEvent
}