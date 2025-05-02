package com.example.zim.screens

import android.net.wifi.p2p.WifiP2pDevice
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
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
import androidx.compose.runtime.collectAsState
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
import com.example.zim.components.hardwareServicesCheck
import com.example.zim.events.ConnectionsEvent
import com.example.zim.events.UserChatEvent
import com.example.zim.helperclasses.Connection
import com.example.zim.states.ConnectionsState
import com.example.zim.states.ProtocolState
import com.example.zim.viewModels.ConnectionsViewModel
import com.example.zim.viewModels.ProtocolViewModel
import com.example.zim.viewModels.UserChatViewModel
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight


@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun ConnectionsScreen(
    navController: NavController, state: ConnectionsState, onEvent: (ConnectionsEvent) -> Unit,
    viewModel: ConnectionsViewModel = hiltViewModel<ConnectionsViewModel>(),
    userChatViewModel: UserChatViewModel = hiltViewModel<UserChatViewModel>(),
    protocolState: ProtocolState
) {
    val networkPeers by viewModel.networkPeers.collectAsState()
    var lastPromptConnection by remember {
        mutableStateOf<WifiP2pDevice?>(null)
    }
    var everythingEnabled: Boolean by remember {
        mutableStateOf(false)
    }

    if (state.promptConnections.isNotEmpty())
        lastPromptConnection = state.promptConnections.last()

    val context = LocalContext.current

    val verticalPadding = 12.dp
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Connection icon with simplified design
            Icon(
                imageVector = Icons.Outlined.Sensors,
                contentDescription = "Connection Logo",
                modifier = Modifier
                    .size(140.dp)
                    .padding(top = 20.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            everythingEnabled = hardwareServicesCheck(protocolState)
            if (everythingEnabled) {
                onEvent(ConnectionsEvent.ScanForConnections)
                Text(
                    text = "Searching For Nearby Devices...",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),

                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Open the app in the other device and make sure its Wi-Fi is Turned on!",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 8.dp)
                )
            }
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(0.85f),
                color = MaterialTheme.colorScheme.primary.copy(0.5f)
            )

            Box(
                modifier = Modifier
                    .weight(1F)
                    .fillMaxSize()
                    .padding(top = 12.dp), contentAlignment = Alignment.Center
            ) {
                if (!everythingEnabled) {
                    Text(
                        text = "Please Enable Location and Wifi\nAnd Disable Hotspot",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary.copy(0.66f),
                        textAlign = TextAlign.Center
                    )
                } else if (state.connections.isEmpty()) {
                    Text(
                        text = "No Device(s) Found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary.copy(0.66f)
                    )
                } else {
                    val lastConnection = state.connections.last()
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        state = rememberLazyListState()
                    ) {
//                        state.connections.forEach { connection ->
//                            item {
//                                ConnectionRow(
//                                    phoneName = connection.deviceName,
//                                    description = connection.deviceAddress,
//                                    onClick = {
//                                        onEvent(ConnectionsEvent.ConnectToDevice(connection))
//                                    }
//                                )
//                                if(connection != lastConnection) {
//                                    HorizontalDivider(
//                                        modifier = Modifier.fillMaxWidth(0.66f),
//                                        color = MaterialTheme.colorScheme.primary.copy(0.33f)
//                                    )
//                                }
//                            }
//                        }

                        items(networkPeers.size) { index ->
                            val networkPeer = networkPeers[index]
                            if(networkPeer.deviceName.isNullOrEmpty()) return@items
                            ConnectionRow(
                                phoneName = networkPeer.deviceName,
                                description = networkPeer.ipAddress,
                                onClick = {
                                    onEvent(ConnectionsEvent.ConnectToDevice(networkPeer.ipAddress))
                                }
                            )
                            if (index != state.connections.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.fillMaxWidth(0.66f),
                                    color = MaterialTheme.colorScheme.primary.copy(0.33f)
                                )
                            }
                        }
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
                lastPromptConnection ?: WifiP2pDevice(),
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