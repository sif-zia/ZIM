package com.example.zim.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.zim.components.AlertRow
import com.example.zim.helperclasses.Alert
import com.example.zim.helperclasses.AlertType
import com.example.zim.navigation.Navigation
import java.time.LocalDateTime

@Composable
fun AlertsScreen(navController: NavController) {
    val verticalPadding = 12.dp
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(vertical = verticalPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(0.33f))
                Text(
                    text = "My Alerts",
                    modifier = Modifier.weight(0.33f),
                    textAlign = TextAlign.Center
                )
                HorizontalDivider(modifier = Modifier.weight(0.33f))
            }
            Text(
                text = "No Recent Alerts",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = verticalPadding),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary.copy(0.6f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(vertical = verticalPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(0.33f))
                Text(
                    text = "Alerts",
                    modifier = Modifier.weight(0.33f),
                    textAlign = TextAlign.Center
                )
                HorizontalDivider(modifier = Modifier.weight(0.33f))
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    AlertRow(
                        alert = Alert(AlertType.HEALTH, "Itisam", 5, LocalDateTime.now())
                    )
                }
                item {
                    AlertRow(
                        alert = Alert(AlertType.FALL, "Muaaz", 2, LocalDateTime.now())
                    )
                }
                item {
                    AlertRow(
                        alert = Alert(AlertType.FIRE, "Afroze", 3, LocalDateTime.now())
                    )
                }
                item {
                    AlertRow(
                        alert = Alert(AlertType.SAFETY, "Saim", 1, LocalDateTime.now())
                    )
                }


            }


        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier
                    .width(64.dp)
                    .height(64.dp)
                    .clip(shape = RoundedCornerShape(100))
                    .border(
                        1.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(100)
                    )
                    .background(color = MaterialTheme.colorScheme.tertiary),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center

            )
            {
                Icon(
                    imageVector = Icons.Outlined.NotificationAdd,
                    contentDescription = "Add Alert Button",
                    modifier = Modifier.size(35.dp),
                    tint= MaterialTheme.colorScheme.primary,

                )
            }
        }

    }
}