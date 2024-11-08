package com.example.zim.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import com.example.zim.R
import com.example.zim.events.SignUpEvent
import com.example.zim.navigation.Navigation
import com.example.zim.states.SignUpState
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController, state: SignUpState, onEvent: (SignUpEvent) -> Unit) {
    LaunchedEffect(Unit) {
        onEvent(SignUpEvent.CheckLogin)

        delay(500)

        if (state.IsLoggedIn)
            navController.navigate(Navigation.Chats.route) {
                popUpTo(Navigation.SplashScreen.route) {
                    inclusive = true
                }
            }
        else
            navController.navigate(Navigation.SignUp.route) {
                popUpTo(Navigation.SplashScreen.route) {
                    inclusive = true
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.splash_screen),
            contentDescription = "Splash Screen"
        )
    }
}