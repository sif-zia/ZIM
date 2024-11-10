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
    val id: Int = 0,
    val msg: String, // Any
    @ColumnInfo(defaultValue = "Text")
    val type: String = "Text", // Text, Audio, Image, Video, File
    val isSent: Boolean,
    @ColumnInfo(defaultValue = true.toString())
    val isDM: Boolean = true,
    @ColumnInfo(defaultValue = false.toString())
    val isForwarded: Boolean = false,
    @ColumnInfo(defaultValue = false.toString())
    val isDeleted: Boolean = false
)