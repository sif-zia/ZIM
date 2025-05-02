package com.example.zim.data.room.Dao
import androidx.room.Dao
import androidx.room.Insert
import com.example.zim.data.room.models.Alerts
import com.example.zim.data.room.models.ReceivedAlerts
import com.example.zim.helperclasses.Alert

@Dao
interface AlertDao{

    @Insert
    suspend fun insertAlert(alert: Alerts): Long

    @Insert
    suspend fun insertReceivedAlert(alert: ReceivedAlerts): Long



}