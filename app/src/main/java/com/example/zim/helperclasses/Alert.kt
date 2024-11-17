package com.example.zim.helperclasses

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FireTruck
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.PersonalInjury
import androidx.compose.material.icons.outlined.SportsMartialArts
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.LocalDateTime

enum class AlertType {
    HEALTH,
    FIRE,
    FALL,
    SAFETY,
    CUSTOM;

    fun toIcon(): ImageVector {
        return when (this) {
            HEALTH -> Icons.Outlined.PersonalInjury
            FALL -> Icons.Outlined.SportsMartialArts
            SAFETY -> Icons.Outlined.HealthAndSafety
            FIRE -> Icons.Outlined.FireTruck
            CUSTOM -> Icons.Outlined.Warning
        }
    }

    fun toName(): String {
        return when (this) {
            HEALTH -> "Health Alert"
            FALL -> "Fall Alert"
            SAFETY -> "Safety Alert"
            FIRE -> "Fire Alert"
            CUSTOM -> "Custom Alert"
        }
    }
}
data class Alert (
    val type: AlertType,
    val senderName: String,
    val hops: Int,
    val time: LocalDateTime,
)