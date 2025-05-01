package com.example.zim.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.zim.events.ProtocolEvent
import com.example.zim.states.ProtocolState
import com.example.zim.viewModels.ProtocolViewModel

@Composable
fun hardwareServicesCheck(
    protocolState: ProtocolState,
    protocolViewModel: ProtocolViewModel = hiltViewModel<ProtocolViewModel>(),
): Boolean{
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        if (!protocolState.isLocationEnabled) {
            Row(
                modifier = Modifier.fillMaxWidth(0.8f).padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Location is Disabled")
                Text(
                    text = "Enable",
                    modifier = Modifier
                        .clickable { protocolViewModel.onEvent(ProtocolEvent.LaunchEnableLocation) }
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
        if (!protocolState.isWifiEnabled) {
            Row(
                modifier = Modifier.fillMaxWidth(0.8f).padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Wifi is Disabled")
                Text(
                    text = "Enable",
                    modifier = Modifier
                        .clickable { protocolViewModel.onEvent(ProtocolEvent.LaunchEnableWifi) }
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
//        if (protocolState.isHotspotEnabled) {
//            Row(
//                modifier = Modifier.fillMaxWidth(0.8f).padding(vertical = 8.dp),
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                Text("Hotspot is Enabled")
//                Text(
//                    text = "Disable",
//                    modifier = Modifier
//                        .clickable { protocolViewModel.onEvent(ProtocolEvent.LaunchEnableHotspot) }
//                        .border(
//                            2.dp,
//                            color = MaterialTheme.colorScheme.primary.copy(0.66f),
//                            shape = RoundedCornerShape(25),
//                        )
//                        .padding(6.dp),
//                    fontSize = 16.sp
//                )
//            }
//        }

    }
//            && !protocolState.isHotspotEnabled
    return protocolState.isLocationEnabled && protocolState.isWifiEnabled
}