package com.example.zim.data.room.models

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.zim.data.room.schema.Schema
import java.time.LocalDate
import java.time.LocalDateTime


@Entity(tableName = Schema.USERS_TABLE)
data class Users(
    @ColumnInfo(name = Schema.USER_ID)
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val deviceName: String? = null,
    val deviceAddress: String? = null,
    val fName: String,  // Any
    var UUID: String,
    val lName: String? = null, // Any
    val DOB: LocalDate?=null,
    val cover: Uri? = null, // URI
    val puKey: String?=null,
    var isActive: Boolean = true,
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    val connectionTime: LocalDateTime = LocalDateTime.now(),
)

