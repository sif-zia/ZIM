package com.example.zim.navigation

enum class Screen {
    SIGNUP,
    CHATS,
    CONNECTIONS,
    ALERTS,
    SPLASH_SCREEN
}
sealed class Navigation(val route: String) {
    object SignUp : Navigation(Screen.SIGNUP.name)
    object Chats : Navigation(Screen.CHATS.name)
    object Connections : Navigation(Screen.CONNECTIONS.name)
    object Alerts : Navigation(Screen.ALERTS.name)
    object SplashScreen: Navigation(Screen.SPLASH_SCREEN.name)
}