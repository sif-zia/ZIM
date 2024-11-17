package com.example.zim.components

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zim.helperclasses.Alert
import com.example.zim.helperclasses.AlertType


@Composable
fun AlertRow(modifier: Modifier = Modifier, alert: Alert) {
    Row(
        modifier = modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth(0.8f)
            .clip(shape = RoundedCornerShape(33))
            .border(1.dp, MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(33))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
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
            Row (horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.padding(horizontal = 8.dp).weight(1f),) {
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
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(text = "${minDistance}m - ${maxDistance}m", fontSize = 14.sp)
                    Text(
                        text = formatDateTime(alert.time),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary.copy(0.66f)
                    )

                }
            }

        }
    }


}
