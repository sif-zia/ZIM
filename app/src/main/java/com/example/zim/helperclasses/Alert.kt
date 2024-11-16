package com.example.zim.helperclasses

import java.time.LocalDateTime

enum class AlertType {
    HEALTH,
    FIRE,
    FALL,
    SAFETY,
}
data class Alert (
    val type: AlertType,
    val senderName: String,
    val hops: Int,
    val time: LocalDateTime,
)