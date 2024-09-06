package com.example.zim.data.room.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "Users")
data class Users(
    @ColumnInfo(name = "User_ID")
    @PrimaryKey(autoGenerate = true)
    val id: Int = 1,
    val deviceName: String? = null,
    val fName: String,  // Any
    val lName: String?, // Any
    val DOB: LocalDate,
    val cover: String? = null // URI
)

@Entity(
    tableName = "Current_User",
    foreignKeys = [
        ForeignKey(
            entity = Users::class,
            parentColumns = arrayOf("User_ID"),
            childColumns = arrayOf("User_ID"),
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )]
)
data class CrrUser(
    @ColumnInfo(name = "CrrUser_ID")
    @PrimaryKey(autoGenerate = true)
    val id: Int = 1,
    val pKey: String, // Any
    @ColumnInfo(name = "User_ID")
    val userIdFk: Int,
)

@Entity(
    tableName = "Connections",
    foreignKeys = [
        ForeignKey(
            entity = Users::class,
            parentColumns = arrayOf("User_ID"),
            childColumns = arrayOf("UserA_ID"),
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Users::class,
            parentColumns = arrayOf("User_ID"),
            childColumns = arrayOf("UserB_ID"),
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )],

    )
data class Connections(
    @ColumnInfo(name = "Con_ID")
    @PrimaryKey(autoGenerate = true)
    val id: Int = 1,
    val status: String, // Connected, Not Connected
    @ColumnInfo(name = "UserA_ID")
    val userAIdFk: Int,
    @ColumnInfo(name = "UserB_ID")
    val userBIdFk: Int,
)

@Entity(
    tableName = "Keys",
    foreignKeys = [
        ForeignKey(
            entity = Users::class,
            parentColumns = arrayOf("User_ID"),
            childColumns = arrayOf("User_ID"),
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )]
)
data class Keys(
    @ColumnInfo(name = "Key_ID")
    @PrimaryKey(autoGenerate = true)
    val id: Int = 1,
    val key: String, // Any
    @ColumnInfo(name = "User_ID")
    val ownerIdFk: Int,
)

@Entity(
    tableName = "Messages",
    foreignKeys = [
        ForeignKey(
            entity = Messages::class,
            parentColumns = arrayOf("Msg_ID"),
            childColumns = arrayOf("ReplyMsg_ID"),
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.SET_NULL
        )]
)
data class Messages(
    @ColumnInfo(name = "Msg_ID")
    @PrimaryKey(autoGenerate = true)
    val id: Int = 1,
    val msg: String, // Any
    val type: String, // Text, Audio, Image, Video, File
    val sentTime: LocalDateTime,
    val isSent: Boolean,
    val isForwarded: Boolean,
    @ColumnInfo(name = "ReplyMsg_ID")
    val replyOfFk: Int? = null,
)

@Entity(tableName = "Sent_Messages",
    foreignKeys = [
        ForeignKey(
            entity = Messages::class,
            parentColumns = arrayOf("Msg_ID"),
            childColumns = arrayOf("SMsg_ID"),
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Users::class,
            parentColumns = arrayOf("User_ID"),
            childColumns = arrayOf("User_ID"),
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ])
data class SentMessages(
    @ColumnInfo(name = "SMsg_ID")
    @PrimaryKey(autoGenerate = true)
    val id: Int = 1,
    val status: String, // Failed, Sent, Sending
    @ColumnInfo(name = "Msg_ID")
    val msgIdFk: Int,
    @ColumnInfo(name = "User_ID")
    val receiverIdFk: Int,
)

@Entity(tableName = "Received_Messages",
    foreignKeys = [
        ForeignKey(
            entity = Messages::class,
            parentColumns = arrayOf("Msg_ID"),
            childColumns = arrayOf("RMsg_ID"),
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Users::class,
            parentColumns = arrayOf("User_ID"),
            childColumns = arrayOf("User_ID"),
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ])
data class ReceivedMessages(
    @ColumnInfo(name = "RMsg_ID")
    @PrimaryKey(autoGenerate = true)
    val id: Int = 1,
    val receivedTime: LocalDateTime,
    @ColumnInfo(name = "Msg_ID")
    val msgIdFk: Int,
    @ColumnInfo(name = "User_ID")
    val senderIdFk: Int,
)

@Entity(tableName = "Alerts")
data class Alerts(
    @ColumnInfo(name = "Alert_ID")
    @PrimaryKey(autoGenerate = true)
    val id: Int = 1,
    val description: String, // Any
    val type: String, // Phone Drop, Health Emergency, Lost Alert, Safety Hazard, Others
    val isSent: Boolean,
    val sentTime: LocalDateTime,
)

@Entity(tableName = "Received_Alerts",
    foreignKeys = [
        ForeignKey(
            entity = Alerts::class,
            parentColumns = arrayOf("Alert_ID"),
            childColumns = arrayOf("RAlert_ID"),
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Users::class,
            parentColumns = arrayOf("User_ID"),
            childColumns = arrayOf("User_ID"),
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE
        )
    ])
data class ReceivedAlerts(
    @ColumnInfo(name = "RAlert_ID")
    @PrimaryKey(autoGenerate = true)
    val id: Int = 1,
    val hops: Int,
    val receivedTime: LocalDateTime,
    @ColumnInfo(name = "Alert_ID")
    val alertIdFk: Int,
    @ColumnInfo(name = "User_ID")
    val initiatorIdFk: Int,
)



