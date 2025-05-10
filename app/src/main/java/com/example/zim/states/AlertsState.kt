package com.example.zim.states

import com.example.zim.data.room.models.Alerts
import com.example.zim.data.room.models.AlertsWithReceivedAlertsAndSender
import com.example.zim.data.room.models.ReceivedAlerts

data class AlertsState (
    var lastAlert: Alerts? = null,
    var receivedAlerts: List<AlertsWithReceivedAlertsAndSender> = emptyList()
)