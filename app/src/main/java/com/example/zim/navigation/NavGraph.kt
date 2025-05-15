package com.example.zim.navigation

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.zim.R
import com.example.zim.components.DropDown
import com.example.zim.components.LogoRow
import com.example.zim.events.ChatsEvent
import com.example.zim.events.ConnectionsEvent
import com.example.zim.events.GroupsEvent
import com.example.zim.events.ProtocolEvent
import com.example.zim.events.UpdateUserEvent
import com.example.zim.events.UserChatEvent
import com.example.zim.screens.AlertsScreen
import com.example.zim.screens.ChatsScreen
import com.example.zim.screens.ConnectionsScreen
import com.example.zim.screens.FallDetectionScreen
import com.example.zim.screens.GroupChat
import com.example.zim.screens.GroupsScreen
import com.example.zim.screens.NewGroupScreen
import com.example.zim.screens.ProfileScreen
import com.example.zim.screens.SettingsScreen
import com.example.zim.screens.SignUpScreen
import com.example.zim.screens.UserChat
import com.example.zim.states.ConnectionsState
import com.example.zim.viewModels.ChatsViewModel
import com.example.zim.viewModels.ConnectionsViewModel
import com.example.zim.viewModels.GroupsViewModel
import com.example.zim.viewModels.ProtocolViewModel
import com.example.zim.viewModels.SignUpViewModel
import com.example.zim.viewModels.UserChatViewModel

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

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun NavGraph(
    chatsViewModel: ChatsViewModel = hiltViewModel(),
    signUpViewModel: SignUpViewModel = hiltViewModel(),
    connectionsViewModel: ConnectionsViewModel = hiltViewModel(),
    userChatViewModel: UserChatViewModel = hiltViewModel(),
    protocolViewModel: ProtocolViewModel = hiltViewModel(),
    groupsViewModel: GroupsViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()

    val routesWithBottomNavBar: List<String> = listOf(
        Navigation.Chats.route,
        Navigation.Connections.route,
        Navigation.Alerts.route,
        Navigation.FallDetectionModel.route,
        Navigation.Groups.route
    )
    val routesWithLogoRow: List<String> = listOf(
        Navigation.Chats.route,
        Navigation.Connections.route,
        Navigation.Alerts.route,
        Navigation.FallDetectionModel.route,
        Navigation.Groups.route,
    )


    val horizontalPadding: Dp = 16.dp
//    val verticalPadding: Dp = 12.dp

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val chatsState by chatsViewModel.state.collectAsState()
    val onChatsEvent = chatsViewModel::onEvent

    val signUpState by signUpViewModel.state.collectAsState()
    val onSignUpEvent = signUpViewModel::onEvent

    val onUpdateUserEvent = signUpViewModel::onUpdateEvent

    val connectionsState by connectionsViewModel.state.collectAsState()
    val connectionOnEvent = connectionsViewModel::onEvent

    val userChatState by userChatViewModel.state.collectAsState()
    val userChatOnEvent = userChatViewModel::onEvent

    val protocolState by protocolViewModel.state.collectAsState()

    if (signUpState.IsLoggedIn == null)
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.zim_splash_screen),
                contentDescription = "Splash Screen"
            )
        }
    else
        Scaffold(
            topBar = {
                AnimatedVisibility(
                    visible = currentRoute in routesWithLogoRow,
                    enter = slideInVertically { -it },
                    exit = slideOutVertically { -it },
                ) {
                    LogoRow(
                        modifier = Modifier
                            .padding(top = 32.dp)
                            .padding(
                                horizontal = horizontalPadding,
                            ),
                        expandMenu = { onChatsEvent(ChatsEvent.ExpandMenu) },
                        navController = navController,  // Make sure to pass the navController here
                    ) {
                        DropDown(
                            dropDownMenu = DropDownMenus.ChatsScreen(),
                            navController = navController,
                            expanded = chatsState.menuExpanded,
                            dismissMenu = {onChatsEvent(ChatsEvent.DismissMenu)}
                        )
                    }
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = currentRoute in routesWithBottomNavBar,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it },
                ) {
                    BottomNavigationBar(
                        navController = navController,
                        currentRoute = currentRoute,
                        chatsState = chatsState
                    )
                }
            }
        ) { paddingValues ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),

                ) {
                val startDestination = if (signUpState.IsLoggedIn == true)
                    Navigation.Chats.route
                else
                    Navigation.SignUp.route



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
                        SwipeNavigation(navController) {
                            ConnectionsScreen(
                                navController = navController,
                                state = connectionsState,
                                onEvent = connectionOnEvent,
                                protocolState = protocolState
                            )
                        }
                    }
                    composable(Navigation.Alerts.route) {
                        SwipeNavigation(navController) {
                            AlertsScreen(navController = navController)
                        }
                    }
                    composable(Navigation.Groups.route) {
                        SwipeNavigation(navController) {
                            GroupsScreen(navController = navController)
                        }
                    }
                    composable(Navigation.UserChat.route + "/{userId}") { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId")?.toInt()
                        if (userId != null) {
                            userChatOnEvent(UserChatEvent.LoadData(userId))
                            UserChat(userId = userId, onEvent = userChatOnEvent, state = userChatState, navController = navController)

                        }
                    }
                    composable(Navigation.GroupChat.route + "/{groupId}") { backStackEntry ->
                        val groupId = backStackEntry.arguments?.getString("groupId")?.toInt()
                        if (groupId != null) {
                            groupsViewModel.onEvent(GroupsEvent.LoadGroupData(groupId))
                            GroupChat(groupId = groupId, navController = navController)
                        }
                    }
                    composable(Navigation.NewGroup.route) {
                        NewGroupScreen(navController)
                    }
                    composable(Navigation.Settings.route) {
                        SettingsScreen()
                    }
                    composable(Navigation.Profile.route) {
                        ProfileScreen(
                            navController= navController,
                            state = signUpState,
                            onEvent = onUpdateUserEvent ,
                        )
                    }
                    composable(Navigation.FallDetectionModel.route){
                        FallDetectionScreen(navController)
                    }
                }


            }
        }
}