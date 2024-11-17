package com.example.zim.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FireTruck
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.PersonalInjury
import androidx.compose.material.icons.outlined.SportsMartialArts
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zim.helperclasses.Alert
import com.example.zim.helperclasses.AlertType
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDateTime

fun getNewAngle(alertTime: LocalDateTime): Float {
    val currentTime = LocalDateTime.now()
    val duration = Duration.between(alertTime, currentTime)
    val differenceInMillis = duration.toMillis()
    val fiveMinutesInMillis = 10 * 1000
    val fraction = differenceInMillis.toFloat() / fiveMinutesInMillis.toFloat()
    val newAngle = fraction * -360f
    return newAngle
}

@Composable
fun myAlert(modifier: Modifier = Modifier, alert: Alert): Boolean {
    var timeAngle by remember {
        mutableStateOf(0f)
    }

    LaunchedEffect(alert.time) {
        while (timeAngle > -360f) {
            timeAngle = getNewAngle(alert.time)
            delay(1000L) // Wait for 1 second
        }
    }

    Row(
        modifier = modifier

            .fillMaxWidth(0.8f)
            .clip(shape = RoundedCornerShape(33))
            .border(1.dp, MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(33))
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 8.dp)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {

            val icon = when (alert.type) {
                AlertType.HEALTH -> Icons.Outlined.PersonalInjury
                AlertType.FALL -> Icons.Outlined.SportsMartialArts
                AlertType.SAFETY -> Icons.Outlined.HealthAndSafety
                AlertType.FIRE -> Icons.Outlined.FireTruck
            }
            val alertType = when (alert.type) {
                AlertType.HEALTH -> "Health Alert"
                AlertType.FALL -> "Fall Alert"
                AlertType.SAFETY -> "Safety Alert"
                AlertType.FIRE -> "Fire Alert"
            }
            val minDistance = (alert.hops - 1) * 100
            val maxDistance = (alert.hops) * 100
            Icon(
                imageVector = icon,
                contentDescription = "Alert Icon",
                modifier = Modifier.size(64.dp)
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier.height(64.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(text = alertType, fontSize = 18.sp)
                    Text(
                        text = alert.senderName,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary.copy(0.66f)
                    )

                }
                Column(
                    modifier = Modifier.height(64.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val backgroundColor = MaterialTheme.colorScheme.secondary
                    if (timeAngle > -360f) {
                        Box(
                            modifier = Modifier
                                .height(45.dp)
                                .width(45.dp)
                                .clip(RoundedCornerShape(100))
                                .background(color = MaterialTheme.colorScheme.primary)
                        ) {
                            Canvas(modifier = Modifier.matchParentSize()) {
                                // Draw a sector (a filled arc that looks like a pie slice)
                                drawArc(
                                    color = backgroundColor, // Color of the sector
                                    startAngle = -90f, // Start angle of the arc (in degrees)
                                    sweepAngle = timeAngle, // Sweep angle (extent of the arc, in degrees)
                                    useCenter = true, // Connect the arc to the center to form a sector
                                    style = Fill // Fill the sector with color
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(color = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = "Resend",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }

    }
    if (timeAngle>-360f)
        return true
}