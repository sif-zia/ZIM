package com.example.zim.components

import android.net.wifi.p2p.WifiP2pDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon.Companion.Text
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.zim.helperclasses.Connection
import com.example.zim.states.ConnectionsState

@Composable
fun ConnectionPrompt(promptConnection: WifiP2pDevice, onAccept:()->Unit, onReject:()->Unit) {
    val horizontalPadding = 12.dp

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Card(
            shape =  RoundedCornerShape(50),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
        ) {
            Row(
                modifier = Modifier
                    .height(48.dp)
                    .clip(shape = RoundedCornerShape(50))
                    .background(color = MaterialTheme.colorScheme.secondary)
                    .border(
                        2.dp,
                        Color.LightGray,
                        shape = RoundedCornerShape(50)
                    ),// Adding elevation with shadow effect,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.width(horizontalPadding))
                Text(text = "${promptConnection.deviceName} wants to connect")
                Spacer(modifier = Modifier.width(horizontalPadding))
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Accept connection button",
                    tint = Color.Green,
                    modifier = Modifier.clickable { onAccept() }
                )
                Spacer(modifier = Modifier.width(horizontalPadding))
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Reject connection button",
                    tint = Color.Red,
                    modifier = Modifier.clickable { onReject() }
                )
                Spacer(modifier = Modifier.width(horizontalPadding))
            }
        }
    }
}