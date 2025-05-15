package com.example.zim.data.room.models

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.zim.data.room.schema.Schema

@Entity(tableName = Schema.GROUPS)
data class Groups (
    @ColumnInfo(name = Schema.GROUP_ID)
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = Schema.USER_ID_FK)
    val admin: Int,
    val name: String,
    val description: String? = null,
    val cover: Uri? = null, // URI
    val secretKey: String? = null,
)