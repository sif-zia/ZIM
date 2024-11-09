package com.example.zim.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LeakAdd
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavigationItem(
    val label : String = "",
    val icon : ImageVector = Icons.Filled.Home,
    val route : String = ""
) {

    fun bottomNavigationItems() : List<BottomNavigationItem> {
        return listOf(
            BottomNavigationItem(
                label = "Chats",
                icon = Icons.Filled.ChatBubble,
                route = Navigation.Chats.route
            ),
            BottomNavigationItem(
                label = "Connections",
                icon = Icons.Filled.LeakAdd,
                route = Navigation.Connections.route
            ),
            BottomNavigationItem(
                label = "Alert",
                icon = Icons.Filled.NotificationsNone,
                route = Navigation.Alerts.route
            ),
        )
    }
}