package com.example.zim.navigation

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.zim.R
import com.example.zim.components.DropDown
import com.example.zim.components.LogoRow
import com.example.zim.events.ChatsEvent
import com.example.zim.events.ConnectionsEvent
import com.example.zim.screens.AlertsScreen
import com.example.zim.screens.ChatsScreen
import com.example.zim.screens.ConnectionsScreen
import com.example.zim.screens.GroupChat
import com.example.zim.screens.NewGroupScreen
import com.example.zim.screens.ProfileScreen
import com.example.zim.screens.SettingsScreen
import com.example.zim.screens.SignUpScreen
import com.example.zim.screens.UserChat
import com.example.zim.viewModels.ChatsViewModel
import com.example.zim.viewModels.ConnectionsViewModel
import com.example.zim.viewModels.SignUpViewModel

fun getEnterAnimation(sourceRoute: String?, destinationRoute: String?): EnterTransition {
    val sourceIndex = routeToNav(sourceRoute)?.index ?: -1
    val destinationIndex = routeToNav(destinationRoute)?.index ?: -1

    return when {
        sourceIndex == -1 || destinationIndex == -1 -> fadeIn(animationSpec = tween(700))
        sourceIndex < destinationIndex -> slideInHorizontally { it }
        sourceIndex > destinationIndex -> slideInHorizontally { -it }
        else -> fadeIn(animationSpec = tween(700))
    }
}

fun getExitAnimation(sourceRoute: String?, destinationRoute: String?): ExitTransition {
    val sourceIndex = routeToNav(sourceRoute)?.index ?: -1
    val destinationIndex = routeToNav(destinationRoute)?.index ?: -1

    return when {
        sourceIndex == -1 || destinationIndex == -1 -> fadeOut(animationSpec = tween(700))
        sourceIndex < destinationIndex -> slideOutHorizontally { -it }
        sourceIndex > destinationIndex -> slideOutHorizontally { it }
        else -> fadeOut(animationSpec = tween(700))
    }
}

@Composable
fun NavGraph(
    chatsViewModel: ChatsViewModel,
    signUpViewModel: SignUpViewModel,
    connectionsViewModel: ConnectionsViewModel
) {
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
//    val verticalPadding: Dp = 12.dp

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val chatsState by chatsViewModel.state.collectAsState()
    val onChatsEvent = chatsViewModel::onEvent

    val signUpState by signUpViewModel.state.collectAsState()
    val onSignUpEvent = signUpViewModel::onEvent

    val connectionsState by connectionsViewModel.state.collectAsState()
    val connectionOnEvent = connectionsViewModel::onEvent

    if (signUpState.IsLoggedIn == null)
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.splash_screen),
                contentDescription = "Splash Screen"
            )
        }
    else
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 32.dp),

            ) {
            val startDestination = if (signUpState.IsLoggedIn == true)
                Navigation.Chats.route
            else
                Navigation.SignUp.route

            AnimatedVisibility(
                visible = currentRoute in routesWithLogoRow,
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it },
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
                NavHost(
                    modifier = Modifier
                        .weight(1f),
                    navController = navController,
                    startDestination = startDestination,
                    enterTransition = {
                        getEnterAnimation(
                            initialState.destination.route,
                            targetState.destination.route
                        )
                    },
                    exitTransition = {
                        getExitAnimation(
                            initialState.destination.route,
                            targetState.destination.route
                        )

                    }
                ) {
                    composable(Navigation.SignUp.route) {
                        SignUpScreen(
                            navController = navController,
                            onEvent = onSignUpEvent,
                            state = signUpState
                        )
                    }
                    composable(Navigation.Chats.route) {
                        SwipeNavigation(navController) {
                            ChatsScreen(
                                navController = navController,
                                state = chatsState,
                                onEvent = onChatsEvent
                            )
                        }
                    }
                    composable(Navigation.Connections.route) {

                        connectionOnEvent(ConnectionsEvent.ScanForConnections)
                        SwipeNavigation(navController) {
                            ConnectionsScreen(
                                navController = navController,
                                state = connectionsState,
                                onEvent = connectionOnEvent
                            )
                        }
                    }
                    composable(Navigation.Alerts.route) {
                        SwipeNavigation(navController) {
                            AlertsScreen(navController = navController)
                        }
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

            AnimatedVisibility(
                visible = currentRoute in routesWithBottomNavBar,
                enter = slideInVertically { it }, // Slide in from the bottom
                exit = slideOutVertically { it }, // Slide out t other bottom
            ) {
                BottomNavigationBar(
                    navController = navController,
                    currentRoute = currentRoute,
                    chatsState = chatsState
                )
            }
        }
}