package com.example.zim.helperclasses

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FireTruck
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.PersonalInjury
import androidx.compose.material.icons.outlined.SportsMartialArts
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.zim.helperclasses.AlertType.CUSTOM
import com.example.zim.helperclasses.AlertType.FALL
import com.example.zim.helperclasses.AlertType.FIRE
import com.example.zim.helperclasses.AlertType.HEALTH
import com.example.zim.helperclasses.AlertType.SAFETY
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
            else -> "Custom Alert"
        }
    }
}

fun String.toAlertType(): AlertType {
    return when (this) {
        "Health Alert" -> HEALTH
        "Fall Alert" -> FALL
        "Safety Alert" -> SAFETY
        "Fire Alert" -> FIRE
        "Custom Alert" -> CUSTOM
        else -> CUSTOM
    }
}



data class Alert(
    val type: AlertType,
    val senderName: String,
    val hops: Int,
    val time: LocalDateTime,
)