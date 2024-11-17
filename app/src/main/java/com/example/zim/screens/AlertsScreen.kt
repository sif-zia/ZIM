package com.example.zim.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.zim.components.AlertRow
import com.example.zim.components.FloatingButton
import com.example.zim.components.myAlert
import com.example.zim.helperclasses.Alert
import com.example.zim.helperclasses.AlertType
import com.example.zim.navigation.Navigation
import java.time.LocalDateTime

@Composable
fun AlertsScreen(navController: NavController) {
    val verticalPadding = 12.dp
    var showAddAlertBtn by remember {
        mutableStateOf(true)
    }
    var currentAlert by remember {
        mutableStateOf<Alert?>(null)
    }
    currentAlert = Alert(AlertType.SAFETY, "Muaaz", 3, LocalDateTime.now())
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
            currentAlert?.let { alert ->
                showAddAlertBtn = myAlert(alert = alert)
            } ?: run {
                Text(
                    text = "No Recent Alerts",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = verticalPadding),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary.copy(0.6f)
                )
            }

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
        AnimatedVisibility(
            visible = showAddAlertBtn,
            enter = slideInVertically { it }, // Slide in from the bottom
            exit = slideOutVertically { it }, // Slide out t other bottom
        ) {
            FloatingButton(onClick = { /*TODO*/ }) {
                Icon(
                    imageVector = Icons.Outlined.NotificationAdd,
                    contentDescription = "Add Alert Button",
                    modifier = Modifier.size(25.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}