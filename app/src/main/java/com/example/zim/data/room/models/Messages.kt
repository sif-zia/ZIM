package com.example.zim.data.room.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.zim.data.room.schema.Schema
import java.time.LocalDateTime

@Entity(
    tableName = Schema.MESSAGES,
)
data class Messages(
    @ColumnInfo(name = Schema.MESSAGE_ID)
    @PrimaryKey(autoGenerate = true)
    val id: Int = 1,
    val msg: String, // Any
    val type: String, // Text, Audio, Image, Video, File
    val sentTime: LocalDateTime,
    val isSent: Boolean,
    val isDM: Boolean,
    val isForwarded: Boolean,
    val isDeleted: Boolean
)