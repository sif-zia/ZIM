package com.example.zim.navigation

import com.example.zim.navigation.Navigation.Companion.values

enum class Screen {
    SIGNUP,
    CHATS,
    CONNECTIONS,
    ALERTS,
    GROUP_CHAT,
    USER_CHAT,
    NEW_GROUP,
    PROFILE,
    SETTINGS,
    FALLDETECTIONMODEL,
    GROUPS
}

sealed class Navigation(val route: String, val index: Int) {
    data object SignUp : Navigation(Screen.SIGNUP.name, -1)
    data object Chats : Navigation(Screen.CHATS.name, 0)
    data object Groups : Navigation(Screen.GROUPS.name, 1)
    data object Connections : Navigation(Screen.CONNECTIONS.name, 2)
    data object Alerts : Navigation(Screen.ALERTS.name, 3)
    data object UserChat : Navigation(Screen.USER_CHAT.name, -1)
    data object GroupChat : Navigation(Screen.GROUP_CHAT.name, -1)
    data object NewGroup : Navigation(Screen.NEW_GROUP.name, -1)
    data object Profile : Navigation(Screen.PROFILE.name, -1)
    data object Settings : Navigation(Screen.SETTINGS.name, -1)
    data object FallDetectionModel:Navigation(Screen.FALLDETECTIONMODEL.name,-1)

    companion object {
        fun values() = listOf(
            SignUp,
            Chats,
            Connections,
            Alerts,
            UserChat,
            GroupChat,
            NewGroup,
            Profile,
            Settings,
            FallDetectionModel,
            Groups
        )
    }
}

fun getRightScreen(source: Navigation): Navigation {
    if (source.index == -1) return source

    val totalScreens = BottomNavigationItems().getBottomNavigationItemsCount()
    val rightIndex = (source.index + 1) % totalScreens

    for (destination in values()) {
        if (destination.index == rightIndex)
            return destination
    }
    return source
}

fun getLeftScreen(source: Navigation): Navigation {
    if (source.index == -1) return source

    val totalScreens = BottomNavigationItems().getBottomNavigationItemsCount()
    val leftIndex = if (source.index == 0) totalScreens - 1 else (source.index - 1)

    for (destination in values()) {
        if (destination.index == leftIndex)
            return destination
    }
    return source
}

fun routeToNav(route: String?): Navigation? {
    for(destination in values())
        if(destination.route == route)
            return destination
    return null
}