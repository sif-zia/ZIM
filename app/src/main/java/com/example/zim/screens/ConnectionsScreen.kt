package com.example.zim.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.zim.components.ConnectionPrompt
import com.example.zim.components.ConnectionRow
import com.example.zim.events.ConnectionsEvent
import com.example.zim.helperclasses.Connection
import com.example.zim.states.ConnectionsState

@Composable
fun ConnectionsScreen(
    navController: NavController, state: ConnectionsState, onEvent: (ConnectionsEvent) -> Unit
) {

    val verticalPadding = 12.dp;
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = state.promptConnections.isNotEmpty(),
            enter = slideInVertically { -it }, // Slide in from the bottom
            exit = slideOutVertically { -it }, // Slide out t other bottom
        ) {
            if (state.promptConnections.isNotEmpty()) {
                ConnectionPrompt(
                    state.promptConnections.last(),
                    onAccept = { onEvent(ConnectionsEvent.MakeConnection(state.promptConnections.last())) },
                    onReject = { onEvent(ConnectionsEvent.HidePrompt) }
                )
            }
            else {
                val dummyConnection = Connection("","","")
                ConnectionPrompt(
                    dummyConnection,
                    onAccept = {},
                    onReject = {}
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.weight(1F),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.Sensors,
                    contentDescription = "Connection Logo",
                    modifier = Modifier.size(128.dp),
                )
                Text(
                    text = "Searching For Nearby Devices...",
                    fontSize = 24.sp,
                    modifier = Modifier.padding(vertical = verticalPadding)
                )
                Text(
                    text = "Open the app in the other device and make sure its Wifi is turned on!",
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5F),
                    textAlign = TextAlign.Center,
                )
            }
            HorizontalDivider(modifier = Modifier.fillMaxWidth(0.8f))
            LazyColumn(
                modifier = Modifier.weight(1F),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                state = rememberLazyListState()
            ) {
                state.connections.forEach { connection ->

                    item {
                        ConnectionRow(
                            phoneName = "${connection.fName} ${connection.lName}",
                            description = connection.description
                        ) {

                            onEvent(ConnectionsEvent.ShowPrompt(connection))
                        }
                        HorizontalDivider(modifier = Modifier.fillMaxWidth(0.5f))
                    }
                }

            }
        }
    }
}