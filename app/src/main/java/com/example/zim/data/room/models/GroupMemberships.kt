package com.example.zim.data.room.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.zim.data.room.schema.Schema

@Entity(tableName = Schema.GROUP_MEMBERSHIPS)
data class GroupMemberships(
    @ColumnInfo(name = Schema.GROUP_MEMBERSHIPS_ID)
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = Schema.GROUP_ID_FK)
    val groupId: Int,
    @ColumnInfo(name = Schema.USER_ID_FK)
    val userId: Int,
    val hasReceivedInvitation: Boolean = false,
)
