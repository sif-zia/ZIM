package com.example.zim.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.zim.components.SendMessageRow
import com.example.zim.components.UserInfoRow
import java.time.LocalDateTime


@Composable
fun UserChat(userId: Int) {
    var message by remember {
        mutableStateOf("")
    }
    Column(modifier = Modifier.fillMaxHeight(),verticalArrangement = Arrangement.SpaceBetween) {
        UserInfoRow()
        //chats
        SendMessageRow(message = message, onMessageChange = { message = it })
    }
}