package com.example.zim.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class DropDownMenuItem {
    data class NavigationItem(
        val label: String,
        val icon: ImageVector,
        val route: String
    ) : DropDownMenuItem()

    data class ToggleItem(
        val label: String,
        val icon: ImageVector,
        val isChecked: Boolean,
        val onToggle: (Boolean) -> Unit
    ) : DropDownMenuItem()
}

public interface DropDownMenus {

    class ChatsScreen: DropDownMenus {
        companion object {
            fun getItems(
                isFallDetectionEnabled: Boolean = false,
                onToggleFallDetection: (Boolean) -> Unit = {}
            ): List<DropDownMenuItem> {
                return listOf(
                    DropDownMenuItem.NavigationItem(
                        "Profile",
                        Icons.Filled.ManageAccounts,
                        route = Navigation.Profile.route
                    ),
                    DropDownMenuItem.ToggleItem(
                        "Fall Detection",
                        Icons.Filled.Settings,
                        isChecked = isFallDetectionEnabled,
                        onToggle = onToggleFallDetection
                    ),
                )
            }
        }
    }

    class UserChatScreen: DropDownMenus {
        companion object {
            fun getItems(): List<DropDownMenuItem> {
                return listOf(
                    DropDownMenuItem.NavigationItem(
                        "Search",
                        Icons.Filled.GroupAdd,
                        route = Navigation.NewGroup.route
                    ),
                    DropDownMenuItem.NavigationItem(
                        "Delete Chat",
                        Icons.Filled.Settings,
                        route = Navigation.Settings.route
                    ),
                    DropDownMenuItem.NavigationItem(
                        "Remove Connection",
                        Icons.Filled.ManageAccounts,
                        route = Navigation.Profile.route
                    )
                )
            }
        }
    }
}