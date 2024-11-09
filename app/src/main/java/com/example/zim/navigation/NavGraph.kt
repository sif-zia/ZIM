package com.example.zim.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.zim.events.SignUpEvent
import com.example.zim.screens.AlertsScreen
import com.example.zim.screens.ChatsScreen
import com.example.zim.screens.ConnectionsScreen
import com.example.zim.screens.SignUpScreen
import com.example.zim.screens.SplashScreen
import com.example.zim.states.SignUpState

@Composable
fun NavGraph(signUpState: SignUpState, onSignUpEvent: (SignUpEvent) -> Unit) {
    val navController = rememberNavController();

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) {
        paddingValues ->
        NavHost(modifier = Modifier.padding(paddingValues), navController = navController, startDestination = Navigation.SplashScreen.route) {
            composable(Navigation.SplashScreen.route) {
                SplashScreen(navController, signUpState, onSignUpEvent)
            }
            composable(Navigation.SignUp.route) {
                SignUpScreen(navController = navController, onEvent = onSignUpEvent, state = signUpState)
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
}