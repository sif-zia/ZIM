package com.example.zim.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import com.example.zim.screens.UserChat
import com.example.zim.states.SignUpState
import kotlinx.coroutines.delay
import com.example.zim.R
import com.example.zim.components.DropDown
import com.example.zim.components.LogoRow
import com.example.zim.events.ChatsEvent
import com.example.zim.screens.NewGroupScreen
import com.example.zim.screens.ProfileScreen
import com.example.zim.screens.SettingsScreen
import com.example.zim.states.ChatsState

@Composable
fun NavGraph(signUpState: SignUpState, onSignUpEvent: (SignUpEvent) -> Unit, chatsState: ChatsState, onChatsEvent: (ChatsEvent) -> Unit) {
    val navController = rememberNavController();

    val routesWithBottomNavBar: List<String> = listOf(
        Navigation.Chats.route,
        Navigation.Connections.route,
        Navigation.Alerts.route,
    )
    val routesWithLogoRow: List<String> = listOf(
        Navigation.Chats.route,
        Navigation.Connections.route,
        Navigation.Alerts.route,
    )

    val horizontalPadding: Dp = 16.dp
    val verticalPadding: Dp = 12.dp

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route


    if (signUpState.IsLoggedIn == null)
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.splash_screen),
                contentDescription = "Splash Screen"
            )
        }
    else
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 32.dp),
            bottomBar = {
                AnimatedVisibility(
                    visible = currentRoute in routesWithBottomNavBar,
                    enter = slideInVertically { it }, // Slide in from the bottom
                    exit = slideOutVertically { it }, // Slide out t other bottom
                ) {
                    BottomNavigationBar(navController = navController, currentRoute = currentRoute, chatsState = chatsState)
                }

            },
            topBar = {
                AnimatedVisibility(
                    visible = currentRoute in routesWithLogoRow,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it },
                ) {
                    LogoRow(
                        modifier = Modifier.padding(
                            horizontal = horizontalPadding,
                        ),
                        expandMenu = { onChatsEvent(ChatsEvent.ExpandMenu) }
                    ) {
                        DropDown(
                            dropDownMenu = DropDownMenus.ChatsScreen(),
                            navController = navController,
                            expanded = chatsState.menuExpanded
                        ) {
                            onChatsEvent(ChatsEvent.DismissMenu)
                        }
                    }
                }
            }
        ) { paddingValues ->
            val startDestination = if (signUpState.IsLoggedIn == true)
                Navigation.Chats.route
            else
                Navigation.SignUp.route

            NavHost(
                modifier = Modifier.padding(paddingValues),
                navController = navController,
                startDestination = startDestination
            ) {
                composable(Navigation.SignUp.route) {
                    SignUpScreen(
                        navController = navController,
                        onEvent = onSignUpEvent,
                        state = signUpState
                    )
                }
                composable(Navigation.Chats.route) {
                    ChatsScreen(navController = navController, state = chatsState, onEvent = onChatsEvent)
                }
                composable(Navigation.Connections.route) {
                    ConnectionsScreen(navController = navController)
                }
                composable(Navigation.Alerts.route) {
                    AlertsScreen(navController = navController)
                }
                composable(Navigation.UserChat.route + "/{userId}") { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId")?.toInt()
                    if (userId != null)
                        UserChat(userId = userId)
                }
                composable(Navigation.GroupChat.route + "/{groupId}") { backStackEntry ->
                    val groupId = backStackEntry.arguments?.getString("groupId")?.toInt()
                    if (groupId != null)
                        GroupChat(groupId = groupId)
                }
                composable(Navigation.NewGroup.route) {
                    NewGroupScreen()
                }
                composable(Navigation.Settings.route) {
                    SettingsScreen()
                }
                composable(Navigation.Profile.route) {
                    ProfileScreen()
                }
            }
        }
}