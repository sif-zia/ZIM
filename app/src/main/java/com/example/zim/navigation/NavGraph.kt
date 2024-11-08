package com.example.zim.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.zim.events.SignUpEvent
import com.example.zim.screens.AlertsScreen
import com.example.zim.screens.ChatsScreen
import com.example.zim.screens.ConnectionsScreen
import com.example.zim.screens.SignUpScreen
import com.example.zim.states.SignUpState

@Composable
fun NavGraph(state: SignUpState, onEvent: (SignUpEvent) -> Unit) {
    val navController = rememberNavController();
    NavHost(navController = navController, startDestination = Navigation.SignUp.route) {
        composable(Navigation.SignUp.route) {
            SignUpScreen(navController = navController, onEvent = onEvent, state = state)
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