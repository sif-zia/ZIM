package com.example.zim.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationAdd
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.zim.components.AddAlertDialog
import com.example.zim.components.AlertRow
import com.example.zim.components.FloatingButton
import com.example.zim.components.MyAlert
import com.example.zim.helperclasses.Alert
import com.example.zim.helperclasses.AlertType
import kotlinx.coroutines.delay
import java.time.LocalDateTime

@Composable
fun AlertsScreen(navController: NavController) {
    val verticalPadding = 12.dp
    var showAddAlertBtn by remember { mutableStateOf(true) }
    var currentAlert by remember {
        mutableStateOf<Alert?>(null)
    }
    var showDialog by remember {
        mutableStateOf(false)
    }
    var alerts by remember {
        mutableStateOf(
            listOf(
                Alert(AlertType.HEALTH, "Itisam", 5, LocalDateTime.now()),
                Alert(AlertType.FALL, "Muaaz", 2, LocalDateTime.now()),
                Alert(AlertType.FIRE, "Afroze", 3, LocalDateTime.now()),
                Alert(AlertType.SAFETY, "Saim", 1, LocalDateTime.now())
            )
        )
    }

    val duration = 10 * 1000L

    LaunchedEffect(currentAlert) {
        if (currentAlert != null) {
            showAddAlertBtn = false

            delay(duration)
            showAddAlertBtn = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // My Alerts Header
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(vertical = verticalPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(0.33f),
                    color = MaterialTheme.colorScheme.primary.copy(.5f)
                )
                Text(
                    text = "My Alerts",
                    modifier = Modifier.weight(0.33f),
                    textAlign = TextAlign.Center
                )
                HorizontalDivider(
                    modifier = Modifier.weight(0.33f),
                    color = MaterialTheme.colorScheme.primary.copy(.5f)
                )
            }

            // MyAlert Section
            currentAlert?.let { alert ->
                MyAlert(alert = alert, duration = duration)
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

            // Alerts Header
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(vertical = verticalPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(0.33f),
                    color = MaterialTheme.colorScheme.primary.copy(.5f)
                )
                Text(
                    text = "Alerts",
                    modifier = Modifier.weight(0.33f),
                    textAlign = TextAlign.Center
                )
                HorizontalDivider(
                    modifier = Modifier.weight(0.33f),
                    color = MaterialTheme.colorScheme.primary.copy(.5f)
                )
            }

            // Alerts List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                alerts.forEach { alert ->
                    item {
                        AlertRow(
                            alert = alert
                        )
                    }
                }
            }
        }

        // Floating Button Visibility
        AnimatedVisibility(
            visible = showAddAlertBtn,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            FloatingButton(onClick = {
                // Add Alert Logic
                showDialog = true
            }) {
                Icon(
                    imageVector = Icons.Outlined.NotificationAdd,
                    contentDescription = "Add Alert Button",
                    modifier = Modifier.size(25.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if(showDialog)
            AddAlertDialog(onDismiss = {showDialog = false}, onConfirm = {currentAlert = Alert(AlertType.CUSTOM, "Itisam", 3, LocalDateTime.now())})
    }
}
