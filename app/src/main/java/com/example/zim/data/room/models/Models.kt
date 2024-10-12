//package com.example.zim.data.room.models
//
//import androidx.room.ColumnInfo
//import androidx.room.Entity
//import androidx.room.ForeignKey
//import androidx.room.PrimaryKey
//import java.time.LocalDate
//import java.time.LocalDateTime
//
//
//
//
//@Entity(
//    tableName = "Connections",
////    foreignKeys = [
////        ForeignKey(
////            entity = Users::class,
////            parentColumns = arrayOf("User_ID"),
////            childColumns = arrayOf("UserA_ID"),
////            onUpdate = ForeignKey.CASCADE,
////            onDelete = ForeignKey.CASCADE
////        ),
////        ForeignKey(
////            entity = Users::class,
////            parentColumns = arrayOf("User_ID"),
////            childColumns = arrayOf("UserB_ID"),
////            onUpdate = ForeignKey.CASCADE,
////            onDelete = ForeignKey.CASCADE
////        )],
//
//    )
//data class Connections(
//    @ColumnInfo(name = "Con_ID")
//    @PrimaryKey(autoGenerate = true)
//    val id: Int = 1,
//    val status: String, // Connected, Not Connected
//    @ColumnInfo(name = "UserA_ID")
//    val userAIdFk: Int,
//    @ColumnInfo(name = "UserB_ID")
//    val userBIdFk: Int,
//)
//
//
//
//
//
//
//@Entity(tableName = "Alerts")
//data class Alerts(
//    @ColumnInfo(name = "Alert_ID")
//    @PrimaryKey(autoGenerate = true)
//    val id: Int = 1,
//    val description: String, // Any
//    val type: String, // Phone Drop, Health Emergency, Lost Alert, Safety Hazard, Others
//    val isSent: Boolean,
//    val sentTime: LocalDateTime,
//)
//
//@Entity(tableName = "Received_Alerts",
//    foreignKeys = [
//        ForeignKey(
//            entity = Alerts::class,
//            parentColumns = arrayOf("Alert_ID"),
//            childColumns = arrayOf("RAlert_ID"),
//            onUpdate = ForeignKey.CASCADE,
//            onDelete = ForeignKey.CASCADE
//        ),
//        ForeignKey(
//            entity = Users::class,
//            parentColumns = arrayOf("User_ID"),
//            childColumns = arrayOf("User_ID"),
//            onUpdate = ForeignKey.CASCADE,
//            onDelete = ForeignKey.CASCADE
//        )
//    ])
//data class ReceivedAlerts(
//    @ColumnInfo(name = "RAlert_ID")
//    @PrimaryKey(autoGenerate = true)
//    val id: Int = 1,
//    val hops: Int,
//    val receivedTime: LocalDateTime,
//    @ColumnInfo(name = "Alert_ID")
//    val alertIdFk: Int,
//    @ColumnInfo(name = "User_ID")
//    val initiatorIdFk: Int,
//)
//
//
//
