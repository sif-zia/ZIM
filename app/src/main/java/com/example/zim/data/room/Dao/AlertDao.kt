package com.example.zim.data.room.Dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.zim.data.room.models.Alerts
import com.example.zim.data.room.models.AlertsWithReceivedAlertsAndSender
import com.example.zim.data.room.models.ReceivedAlerts
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface AlertDao{

    @Insert
    suspend fun insertAlert(alert: Alerts): Long

    @Insert
    suspend fun insertReceivedAlert(alert: ReceivedAlerts): Long

    @Query("SELECT * FROM Alerts WHERE isSent = 1 ORDER BY Alerts_ID DESC LIMIT 1")
    fun getLastSentAlert(): Flow<Alerts?>

    @Query("UPDATE Alerts SET sentTime = :time WHERE Alerts_ID = :alertId")
    suspend fun updateAlertTime(alertId: Int, time: LocalDateTime = LocalDateTime.now())

    @Query("""
        SELECT * 
        from Received_Alerts as RA 
        JOIN USERS AS U ON RA.User_ID_FK=U.User_ID
        WHERE U.UUID = :userPuKey AND RA.receivedAlertId = :receivedAlertId""")
    suspend fun getAlert(userPuKey:String,receivedAlertId:Int) : ReceivedAlerts?

    @Query("UPDATE Received_Alerts SET hops = :hops,receivedTime = :time WHERE Received_Alerts_ID= :receivedAlertId ")
    suspend fun updateReceivedAlert(receivedAlertId:Int,hops:Int,time: LocalDateTime)

//    @Query("""
//        SELECT *
//        FROM ALERTS AS A
//        JOIN Received_Alerts AS RA ON A.Alerts_ID = RA.Received_Alerts_ID""")
//    suspend fun getAllReceivedAlerts() : Flow<List<AlertsWithReceivedAlertsAndSender>>
}