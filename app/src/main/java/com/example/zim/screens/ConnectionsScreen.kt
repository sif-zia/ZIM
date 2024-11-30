package com.example.zim.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sensors
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.zim.components.ConnectionPrompt
import com.example.zim.components.ConnectionRow
import com.example.zim.events.ConnectionsEvent
import com.example.zim.helperclasses.Connection
import com.example.zim.states.ConnectionsState
import com.example.zim.viewModels.ConnectionsViewModel

@Composable
fun ConnectionsScreen(
    navController: NavController, state: ConnectionsState, onEvent: (ConnectionsEvent) -> Unit,
    viewModel: ConnectionsViewModel = hiltViewModel<ConnectionsViewModel>()
) {

    var lastPromptConnection by remember {
        mutableStateOf<Connection?>(null)
    }

    if (state.promptConnections.isNotEmpty())
        lastPromptConnection = state.promptConnections.last().copy()

    val context = LocalContext.current

    val verticalPadding = 12.dp
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Sensors,
                contentDescription = "Connection Logo",
                modifier = Modifier.size(128.dp),
            )

            if (!state.isLocationEnabled || !state.isWifiEnabled) {
                if (!state.isLocationEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Location is Disabled")
                        Text(
                            text = "Enable",
                            modifier = Modifier
                                .clickable { viewModel.promptEnableLocation() }
                                .border(
                                    2.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(0.66f),
                                    shape = RoundedCornerShape(25),
                                )
                                .padding(6.dp),
                            fontSize = 16.sp
                        )
                    }
                }
                if (!state.isWifiEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Wifi is Disabled")
                        Text(
                            text = "Enable",
                            modifier = Modifier
                                .clickable { viewModel.promptEnableWifi() }
                                .border(
                                    2.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(0.66f),
                                    shape = RoundedCornerShape(25),
                                )
                                .padding(6.dp),
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                onEvent(ConnectionsEvent.ScanForConnections)
                Text(
                    text = "Searching For Nearby Devices...",
                    fontSize = 22.sp,
                    modifier = Modifier.padding(vertical = verticalPadding)
                )
            }
            Text(
                text = "Open the app in the other device and make sure its Wifi is turned on!",
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5F),
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = verticalPadding)
            )
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(0.8f),
                color = MaterialTheme.colorScheme.primary.copy(0.66f)
            )
            LazyColumn(
                modifier = Modifier
                    .weight(1F)
                    .fillMaxWidth(),
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
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(0.5f),
                            color = MaterialTheme.colorScheme.primary.copy(0.5f)
                        )
                    }
                }

            }
        }


        AnimatedVisibility(
            visible = state.promptConnections.isNotEmpty(),
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
        ) {
            ConnectionPrompt(
                lastPromptConnection ?: Connection("", "", ""),
                onAccept = {
                    onEvent(ConnectionsEvent.MakeConnection(state.promptConnections.last()))
                    Toast.makeText(context, "Connection Made", Toast.LENGTH_SHORT)
                        .show()
                },
                onReject = {
                    onEvent(ConnectionsEvent.HidePrompt)
                    Toast.makeText(context, "Request Rejected", Toast.LENGTH_SHORT)
                        .show()
                }
            )
        }
    }
}