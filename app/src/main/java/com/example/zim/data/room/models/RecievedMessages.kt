package com.example.zim.data.room.models

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.zim.data.room.schema.Schema
import java.time.LocalDateTime

@Entity(tableName = Schema.RECEIVED_MESSAGES)
data class ReceivedMessages(
    @ColumnInfo(name = Schema.RECEIVED_MESSAGE_ID)
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val receivedTime: LocalDateTime,
    @ColumnInfo(defaultValue = false.toString())
    val isRead: Boolean = false,

    // Foreign Keys
    @ColumnInfo(name = Schema.USER_ID_FK)
    val userIDFK: Int,
    @ColumnInfo(name=Schema.MESSAGE_ID_FK)
    val msgIDFK: Int,
)
data class UserWithReceivedMessages(
    @Embedded
    val users: Users,

    @Relation(
        parentColumn = Schema.USER_ID,
        entityColumn = Schema.USER_ID_FK,
    )
    val receivedMessage: ReceivedMessages,
)

data class MessagesWithReceivedMessages(
    @Embedded
    val message: Messages,

    @Relation(
        parentColumn = Schema.MESSAGE_ID,
        entityColumn = Schema.USER_ID_FK,
    )
    val receivedMessage: ReceivedMessages,
)


