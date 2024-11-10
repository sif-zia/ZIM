package com.example.zim.navigation

enum class Screen {
    SIGNUP,
    CHATS,
    CONNECTIONS,
    ALERTS,
    GROUP_CHAT,
    USER_CHAT,
    NEW_GROUP,
    PROFILE,
    SETTINGS
}
sealed class Navigation(val route: String) {
    object SignUp : Navigation(Screen.SIGNUP.name)
    object Chats : Navigation(Screen.CHATS.name)
    object Connections : Navigation(Screen.CONNECTIONS.name)
    object Alerts : Navigation(Screen.ALERTS.name)
    object UserChat: Navigation(Screen.USER_CHAT.name)
    object GroupChat: Navigation(Screen.GROUP_CHAT.name)
    object NewGroup: Navigation(Screen.NEW_GROUP.name)
    object Profile: Navigation(Screen.PROFILE.name)
    object Settings: Navigation(Screen.SETTINGS.name)
}