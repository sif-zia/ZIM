package com.example.zim.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

public data class DropDownMenuItem (
    public val label: String,
    public val icon: ImageVector,
    public val route: String
)

public interface DropDownMenus {

    class ChatsScreen: DropDownMenus {
        companion object {
            fun getItems(): List<DropDownMenuItem> {
                return listOf(
                    DropDownMenuItem(
                        "New Group",
                        Icons.Filled.GroupAdd,
                        route = Navigation.NewGroup.route
                    ),
                    DropDownMenuItem(
                        "Settings",
                        Icons.Filled.Settings,
                        route = Navigation.Settings.route
                    ),
                    DropDownMenuItem(
                        "Profile",
                        Icons.Filled.ManageAccounts,
                        route = Navigation.Profile.route
                    )
                )
            }
        }
    }

    class UserChatScreen: DropDownMenus {
        companion object {
            fun getItems(): List<DropDownMenuItem> {
                return listOf(
                    DropDownMenuItem(
                        "Search",
                        Icons.Filled.GroupAdd,
                        route = Navigation.NewGroup.route
                    ),
                    DropDownMenuItem(
                        "Delete Chat",
                        Icons.Filled.Settings,
                        route = Navigation.Settings.route
                    ),
                    DropDownMenuItem(
                        "Remove Connection",
                        Icons.Filled.ManageAccounts,
                        route = Navigation.Profile.route
                    )
                )
            }
        }
    }

}