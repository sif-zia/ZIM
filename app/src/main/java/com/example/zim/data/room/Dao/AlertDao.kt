package com.example.zim.data.room.Dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.zim.data.room.models.Alerts
import com.example.zim.data.room.models.ReceivedAlerts
import com.example.zim.helperclasses.Alert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface AlertDao{

    @Insert
    suspend fun insertAlert(alert: Alerts): Long

    @Insert
    suspend fun insertReceivedAlert(alert: ReceivedAlerts): Long

    @Query("SELECT * FROM Alerts WHERE isSent = 1 ORDER BY Alerts_ID DESC LIMIT 1")
    fun getLastAlert(): Flow<Alerts?>

    @Query("UPDATE Alerts SET sentTime = :time WHERE Alerts_ID = :alertId")
    fun updateAlertTime(alertId: Int, time: LocalDateTime = LocalDateTime.now())
}