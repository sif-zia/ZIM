package com.example.zim.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.zim.navigation.DropDownMenuItem
import com.example.zim.navigation.DropDownMenus
import com.example.zim.viewModels.FallDetectionViewModel

@Composable
fun DropDown(
    dropDownMenu: DropDownMenus,
    navController: NavController,
    expanded: Boolean,
    dismissMenu: () -> Unit,
    fallDetectionViewModel: FallDetectionViewModel = hiltViewModel()
) {
    val fallDetectionState by fallDetectionViewModel.state.collectAsState();
    val menuItems: List<DropDownMenuItem> = when (dropDownMenu) {
        is DropDownMenus.ChatsScreen -> DropDownMenus.ChatsScreen.getItems(
            isFallDetectionEnabled = fallDetectionState.isFallDetectionEnabled,
            onToggleFallDetection = { fallDetectionViewModel.onToggleFallDetection()}
        )
        is DropDownMenus.UserChatScreen -> DropDownMenus.UserChatScreen.getItems()
        else -> emptyList()
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = dismissMenu,
    ) {
        menuItems.forEach { item ->
            when (item) {
                is DropDownMenuItem.NavigationItem -> {
                    DropdownMenuItem(
                        text = { Text(text = item.label, color = MaterialTheme.colorScheme.primary) },
                        onClick = {
                            navController.navigate(item.route)
                            dismissMenu()
                        },
                        leadingIcon = { androidx.compose.material3.Icon(item.icon, contentDescription = item.label) }
                    )
                }
                is DropDownMenuItem.ToggleItem -> {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = item.label, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = item.isChecked,
                                    onCheckedChange = {
                                        item.onToggle(!item.isChecked)
                                    }
                                )
                            }
                        },
                        onClick = { item.onToggle(!item.isChecked) },
                        leadingIcon = { androidx.compose.material3.Icon(item.icon, contentDescription = item.label) }
                    )
                }
            }
        }
    }
}