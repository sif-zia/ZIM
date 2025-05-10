package com.example.zim.data.room.models

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.zim.data.room.schema.Schema
import com.typesafe.config.ConfigException.Null
import java.time.LocalDate
import java.time.LocalDateTime

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

@Entity(tableName = Schema.ALERTS)
data class Alerts(
    @ColumnInfo(name = Schema.ALERTS_ID)
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val description: String? = null,
    val type: String, // Phone Drop, Health Emergency, Lost Alert, Safety Hazard, Others
    val isSent: Boolean,
    val sentTime: LocalDateTime,
)


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
@Entity(tableName = Schema.RECEIVED_ALERTS)
data class ReceivedAlerts(
    @ColumnInfo(name = Schema.RECEIVED_ALERTS_ID)
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val hops: Int,
    val receivedTime: LocalDateTime,
    @ColumnInfo(name = Schema.ALERTS_ID_FK)
    val alertIdFk: Int,
    @ColumnInfo(name = Schema.USER_ID_FK)
    val initiatorIdFk: Int,
    val receivedAlertId: Int,
)

data class AlertsWithReceivedAlertsAndSender(
    @Embedded
    val alert: Alerts,

    @Relation(
        entity = ReceivedAlerts::class,
        parentColumn = Schema.ALERTS_ID,
        entityColumn = Schema.ALERTS_ID_FK
    )
    val receivedAlert: ReceivedAlerts,

    @Relation(
        entity = Users::class,
        parentColumn = Schema.ALERTS_ID,
        entityColumn = Schema.USER_ID,
        associateBy = Junction(
            value = ReceivedAlerts::class,
            parentColumn = Schema.ALERTS_ID_FK,  // Changed from ALERTS_ID to ALERTS_ID_FK
            entityColumn = Schema.USER_ID_FK     // This maps to the user field in ReceivedAlerts
        )
    )
    val sender: Users
)
