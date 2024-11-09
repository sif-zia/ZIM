package com.example.zim.navigation

enum class Screen {
    SIGNUP,
    CHATS,
    CONNECTIONS,
    ALERTS,
    SPLASH_SCREEN,
    GROUP_CHAT,
    USER_CHAT
}
sealed class Navigation(val route: String) {
    object SignUp : Navigation(Screen.SIGNUP.name)
    object Chats : Navigation(Screen.CHATS.name)
    object Connections : Navigation(Screen.CONNECTIONS.name)
    object Alerts : Navigation(Screen.ALERTS.name)
    object SplashScreen: Navigation(Screen.SPLASH_SCREEN.name)
    object UserChat: Navigation(Screen.USER_CHAT.name)
    object GroupChat: Navigation(Screen.GROUP_CHAT.name)
}