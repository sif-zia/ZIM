package com.example.zim.data.room.models

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.zim.data.room.schema.Schema
import java.time.LocalDateTime

@Entity(tableName = Schema.SENT_MESSAGES)
data class SentMessages(
    @ColumnInfo(name = Schema.SENT_MESSAGE_ID)
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(defaultValue = "Sending")
    val status: String = "Sending", // Failed, Sent, Sending
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    val sentTime: LocalDateTime = LocalDateTime.now(),

    // Foreign Keys
    @ColumnInfo(name = Schema.USER_ID_FK)
    val userIDFK: Int,
    @ColumnInfo(name=Schema.MESSAGE_ID_FK)
    val msgIDFK: Int,
)
data class UserWithSentMessages(
    @Embedded
    val user: Users,

    @Relation(
        parentColumn = Schema.USER_ID,
        entityColumn = Schema.USER_ID_FK,
    )
    val sentMessages: SentMessages,
)
data class MessagesWithSentMessages(
    @Embedded
    val message: Messages,

    @Relation(
        parentColumn = Schema.MESSAGE_ID,
        entityColumn = Schema.USER_ID_FK,
    )
    val sentMessage: SentMessages,
)