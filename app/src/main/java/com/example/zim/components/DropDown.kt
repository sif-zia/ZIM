package com.example.zim.components

import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.zim.navigation.DropDownMenuItem
import com.example.zim.navigation.DropDownMenus

@Composable
fun DropDown(
    dropDownMenu: DropDownMenus,
    navController: NavController,
    expanded: Boolean,
    dismissMenu: () -> Unit
) {
    val menuItems: List<DropDownMenuItem> = when (dropDownMenu) {
        is DropDownMenus.ChatsScreen -> DropDownMenus.ChatsScreen.getItems();
        else -> emptyList()
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = dismissMenu,
    ) {
        menuItems.forEach { item ->
            DropdownMenuItem(
                text = { Text(text = item.label, color = MaterialTheme.colorScheme.primary) },
                onClick = { navController.navigate(item.route) })
        }
    }
}