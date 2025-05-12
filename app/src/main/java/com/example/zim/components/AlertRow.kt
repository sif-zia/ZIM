package com.example.zim.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zim.data.room.models.AlertsWithReceivedAlertsAndSender
import com.example.zim.helperclasses.Alert
import com.example.zim.helperclasses.AlertType
import com.example.zim.helperclasses.toAlertType
import kotlinx.coroutines.delay

@Composable
fun AlertRow(modifier: Modifier = Modifier, alert: AlertsWithReceivedAlertsAndSender) {

    var timeString by remember {
        mutableStateOf(formatDateTime(alert.alert.sentTime))
    }

    LaunchedEffect(alert.alert.sentTime) {
        while(true)
        {
            delay(60 * 1000L)
            timeString = formatDateTime(alert.alert.sentTime)
        }
    }

    Row(
        modifier = modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth(0.9f)
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


            val minDistance = (alert.receivedAlert.hops) * 100
            val maxDistance = (alert.receivedAlert.hops+1) * 100
            Icon(
                imageVector = alert.alert.type.toAlertType().toIcon(),
                contentDescription = "Alert Icon",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Row (horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier
                .padding(horizontal = 8.dp)
                .weight(1f),) {
                Column(
                    modifier = Modifier.height(45.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    //Alert Name
                    Text(
                        text = alert.alert.type.toAlertType().toName(),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                    )
                    // Alert Sender
                    Text(
                        text = alert.sender.fName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary.copy(0.66f)
                    )

                }
                Column(
                    modifier = Modifier.height(45.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.End
                ) {
                    //Distance
                    Text(
                        text = "${minDistance}m - ${maxDistance}m",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                    )
                    //Time
                    Text(
                        text = timeString,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary.copy(0.66f)
                    )

                }
            }

        }
    }


}
