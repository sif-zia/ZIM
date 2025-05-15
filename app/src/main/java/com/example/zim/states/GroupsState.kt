package com.example.zim.states

import com.example.zim.api.GroupInvite
import com.example.zim.data.room.models.Users
import com.example.zim.helperclasses.Group
import com.example.zim.helperclasses.GroupChatContent

data class GroupsState(
    val users: List<Users> = emptyList(),
    val groups: List<Group> = emptyList(),
    val searchQuery: String = "",
    val groupInvites: List<GroupInvite> = emptyList(),
    val pendingUsersPuKeys: List<String> = emptyList(),

    val currentGroupId : Int = -1,
    val currentGroupName : String = "",
    val currentGroupDescription : String? = "",

    val currentGroupChats: List<GroupChatContent> = emptyList(),
)
