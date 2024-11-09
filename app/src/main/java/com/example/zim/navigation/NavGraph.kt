package com.example.zim.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.zim.events.SignUpEvent
import com.example.zim.screens.AlertsScreen
import com.example.zim.screens.ChatsScreen
import com.example.zim.screens.ConnectionsScreen
import com.example.zim.screens.GroupChat
import com.example.zim.screens.SignUpScreen
import com.example.zim.screens.SplashScreen
import com.example.zim.screens.UserChat
import com.example.zim.states.SignUpState
import kotlinx.coroutines.delay

@Composable
fun NavGraph(signUpState: SignUpState, onSignUpEvent: (SignUpEvent) -> Unit) {
    val navController = rememberNavController();

    val routesWithBottomNavBar: List<String> = listOf(
        Navigation.Chats.route,
        Navigation.Connections.route,
        Navigation.Alerts.route
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AnimatedVisibility(
                visible = currentRoute in routesWithBottomNavBar,
                enter = slideInVertically { it }, // Slide in from the bottom
                exit = slideOutVertically { it }, // Slide out to the bottom
            ) {
                BottomNavigationBar(navController = navController, currentRoute = currentRoute)
            }
        }
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
            composable(Navigation.UserChat.route + "/{userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId")?.toInt()
                if(userId != null)
                    UserChat(userId = userId)
            }
            composable(Navigation.GroupChat.route + "/{groupId}") { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId")?.toInt()
                if(groupId != null)
                    GroupChat(groupId = groupId)
            }
        }
    }
}