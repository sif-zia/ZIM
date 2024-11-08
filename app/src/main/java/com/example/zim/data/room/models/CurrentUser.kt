package com.example.zim.data.room.models

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.zim.data.room.schema.Schema


@Entity(tableName = Schema.CURR_USER)
data class CurrentUser(
    @ColumnInfo(name = Schema.CURR_USER_ID)
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val prKey: String? = null, // Any
    // Foreign Keys
    @ColumnInfo(name = Schema.USER_ID_FK)
    val userIDFK: Int
)

data class UserWithCurrentUser(
    @Embedded
    val users: Users,
    @Relation(
        parentColumn = Schema.USER_ID,
        entityColumn = Schema.USER_ID_FK,
    )
    val currentUser: CurrentUser
)

