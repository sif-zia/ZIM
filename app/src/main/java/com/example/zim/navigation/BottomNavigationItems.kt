package com.example.zim.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LeakAdd
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.zim.states.ChatsState

data class BottomNavigationItems(
    val label : String = "",
    val icon : ImageVector = Icons.Filled.Home,
    val route : String = "",
    val notificationCount: Int = 0
) {

    fun getBottomNavigationItems(chatsState: ChatsState) : List<BottomNavigationItems> {
        return listOf(
            BottomNavigationItems(
                label = "Chats",
                icon = Icons.Filled.ChatBubble,
                route = Navigation.Chats.route,
                notificationCount = chatsState.unReadMsgs
            ),
            BottomNavigationItems(
                label = "Connections",
                icon = Icons.Filled.LeakAdd,
                route = Navigation.Connections.route
            ),
            BottomNavigationItems(
                label = "Alert",
                icon = Icons.Filled.NotificationsNone,
                route = Navigation.Alerts.route
            ),
        )
    }
}