package com.example.zim.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.zim.screens.AlertsScreen
import com.example.zim.screens.ChatsScreen
import com.example.zim.screens.ConnectionsScreen
import com.example.zim.screens.SignUpScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController();
    NavHost(navController = navController, startDestination = Navigation.SignUp.route) {
        composable(Navigation.SignUp.route) {
            SignUpScreen(navController = navController)
        }
        composable(Navigation.Chats.route) {
            ChatsScreen(navController = navController)
        }
        composable(Navigation.Connections.route) {
            ConnectionsScreen(navController = navController)
        }
        composable(Navigation.Alerts.route) {
            AlertsScreen(navController = navController)
        }
    }
}