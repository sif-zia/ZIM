package com.example.zim.navigation

import android.util.Log
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

@Composable
fun SwipeNavigation(navController: NavController, content: @Composable () -> Unit) {
    var dragDirection by remember {
        mutableStateOf<String?>(null)
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onHorizontalDrag = { change, dragAmount ->
                    change.consume()
                    dragDirection = if (dragAmount > 0) "Right" else "Left"
                },
                onDragEnd = {
                    val current = navController.currentBackStackEntry?.destination?.route
                    if (dragDirection == "Right") {
                        val currentNav = routeToNav(current)
                        if (currentNav != null && currentNav != Navigation.Chats) {
                            val destinationRoute = getLeftScreen(currentNav).route
                            if (destinationRoute != current)
                                navController.navigate(destinationRoute) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                        }
                    } else if (dragDirection == "Left") {
                        val currentNav = routeToNav(current)
                        if (currentNav != null && currentNav != Navigation.Alerts) {
                            val destinationRoute = getRightScreen(currentNav).route
                            if (destinationRoute != current)
                                navController.navigate(destinationRoute) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                        }
                    }
                    dragDirection = null
                }
            )
        }) {
        content()
    }
}