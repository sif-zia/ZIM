package com.example.zim.data.room.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.zim.data.room.schema.Schema

@Entity(tableName = Schema.GROUP_MESSAGE_RECEIVERS)
data class GroupMsgReceivers(
    @ColumnInfo(name = Schema.GROUP_MESSAGE_RECEIVERS_ID)
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = Schema.MESSAGE_ID_FK)
    val msgIdFK: Int,
    @ColumnInfo(name = Schema.USER_ID_FK)
    val userIdFK: Int,
    @ColumnInfo(name = Schema.GROUP_ID_FK)
    val groupIdFK: Int,
)
