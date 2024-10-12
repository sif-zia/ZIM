package com.example.zim.data.room.models

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.zim.data.room.schema.Schema
import java.time.LocalDate


@Entity(tableName = Schema.USERS_TABLE)
data class Users(
    @ColumnInfo(name = Schema.USER_ID)
    @PrimaryKey(autoGenerate = true)
    val id: Int = 1,
    val deviceName: String? = null,
    val fName: String,  // Any
    val lName: String? = null, // Any
    val DOB: LocalDate,
    val cover: Uri? = null, // URI
    val puKey: String?=null,
)

