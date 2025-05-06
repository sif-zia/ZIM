package com.example.zim.events

import com.example.zim.data.room.models.Alerts
import com.example.zim.data.room.models.Users
import java.time.LocalDateTime

interface AlertsEvent {
    data class SendAlert(
        val type: String,
        val description: String,
        val time: LocalDateTime = LocalDateTime.now()
    ) : AlertsEvent

    data class ResendAlert(val oldAlert: Alerts) : AlertsEvent
}