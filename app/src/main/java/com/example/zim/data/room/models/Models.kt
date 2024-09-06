package com.example.zim.data.room.models

import androidx.room.ColumnInfo
import androidx.room.Entity
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

@Entity(tableName = "Current_User")
data class CrrUser(
    @ColumnInfo(name = "CrrUser_ID")
    @PrimaryKey(autoGenerate = true)
    val id: Int = 1,
    val pKey: String, // Any
    val userIdFk:Int,
)

@Entity(tableName = "Connections")
data class Connections(
    @ColumnInfo(name = "Con_ID")
    @PrimaryKey(autoGenerate = true)
    val id: Int = 1,
    val status:String, // Connected, Not Connected
    val userAIdFk:Int,
    val userBIdFk:Int,
)

@Entity(tableName = "Keys")
data class Keys(
    @ColumnInfo(name = "Key_ID")
    @PrimaryKey(autoGenerate = true)
    val id: Int = 1,
    val key: String, // Any
    val ownerIdFk: Int,
)

@Entity(tableName = "Messages")
data class Messages(
    @ColumnInfo(name = "Msg_ID")
    @PrimaryKey(autoGenerate = true)
    val id: Int = 1,
    val msg: String, // Any
    val type: String, // Text, Audio, Image, Video, File
    val sentTime: LocalDateTime,
    val isSent: Boolean,
    val isForwarded: Boolean,
    val groupIdFk: Int,
    val replyOfFk: Int?=null,
)

@Entity(tableName = "Sent_Messages")
data class SentMessages(
    @ColumnInfo(name = "SMsg_ID")
    @PrimaryKey(autoGenerate = true)
    val id: Int = 1,
    val status: String, // Failed, Sent, Sending
    val msgIdFk: Int,
    val receiverIdFk: Int,
)

@Entity(tableName = "Received_Messages")
data class ReceivedMessages(
    @ColumnInfo(name = "RMsg_ID")
    @PrimaryKey(autoGenerate = true)
    val id: Int = 1,
    val receivedTime: LocalDateTime,
    val msgIdFk: Int,
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

@Entity(tableName = "Received_Alerts")
data class ReceivedAlerts(
    @ColumnInfo(name = "RAlert_ID")
    @PrimaryKey(autoGenerate = true)
    val id: Int = 1,
    val hops: Int,
    val receivedTime: LocalDateTime,
    val alertIdFk: Int,
    val initiatorIdFk: Int,
)



