package com.example.zim.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.zim.components.ChatRow
import com.example.zim.components.LogoRow
import com.example.zim.components.Search
import com.example.zim.navigation.Navigation
import com.example.zim.ui.theme.Typography
import java.time.LocalDateTime

@Composable
fun ChatsScreen(navController: NavController) {

    val horizontalPadding: Dp = 16.dp
    val verticalPadding: Dp = 12.dp

    return Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 32.dp)
    ) {
        LogoRow(
            menu = true,
            modifier = Modifier.padding(
                horizontal = horizontalPadding,
                vertical = verticalPadding
            )
        )

        Search(
            modifier = Modifier.padding(
                horizontal = horizontalPadding,
                vertical = verticalPadding
            )
        )

        ChatRow(
            modifier = Modifier.padding(top = verticalPadding),
            name = "Zainab Bilal",
            lastMsg = "Dheet",
            isConnected = false,
            time = LocalDateTime.now(),
            isRead = false
        )
        ChatRow(
            name = "Zainab Bilal",
            lastMsg = "Dheet",
            isConnected = false,
            time = LocalDateTime.now(),
            isRead = false
        )
        ChatRow(
            name = "Zainab Bilal",
            lastMsg = "Dheet",
            isConnected = true,
            time = LocalDateTime.now(),
            isRead = true
        )
        ChatRow(
            name = "Zainab Bilal",
            lastMsg = "Dheet",
            isConnected = false,
            time = LocalDateTime.now(),
            isRead = false
        )
        ChatRow(
            name = "Zainab Bilal",
            lastMsg = "Dheet",
            isConnected = false,
            time = LocalDateTime.now(),
            isRead = false
        )

        Button(onClick = { navController.navigate(Navigation.Connections.route) }) {
            Text(text = "Connections")
        }
        Button(onClick = { navController.navigate(Navigation.Alerts.route) }) {
            Text(text = "Alerts")
        }
    };
}