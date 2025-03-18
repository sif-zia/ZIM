package com.example.zim.navigation

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.zim.states.ChatsState

@Composable
fun BottomNavigationBar(
    navController: NavController,
    currentRoute: String?,
    chatsState: ChatsState
) {

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        tonalElevation = 16.dp
    ) {
        BottomNavigationItems().getBottomNavigationItems(chatsState.unReadMsgs)
            .forEachIndexed { _, navigationItem ->
                NavigationBarItem(
                    selected = currentRoute == navigationItem.route,
                    label = {
                        Text(navigationItem.label)
                    },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (navigationItem.notificationCount > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.tertiary) {
                                        Text(
                                            navigationItem.notificationCount.toString(),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                navigationItem.icon,
                                contentDescription = navigationItem.label,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    onClick = {
                        navController.navigate(navigationItem.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                        unselectedTextColor = MaterialTheme.colorScheme.onPrimary,
                        indicatorColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
                    )
                )
            }
    }
}