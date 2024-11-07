package com.example.zim.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.zim.navigation.Navigation

@Composable
fun ChatsScreen(navController: NavController) {
    return Column(modifier = Modifier.fillMaxSize().padding(top = 32.dp)) {
        Text(text = "Chats Screen");
        Button(onClick = { navController.navigate(Navigation.Connections.route) }) {
            Text(text = "Connections")
        }
        Button(onClick = { navController.navigate(Navigation.Alerts.route) }) {
            Text(text = "Alerts")
        }
    };
}