package com.example.zim.screens

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.example.zim.components.SendMessageRow
import com.example.zim.components.UserInfoRow
import com.example.zim.events.UserChatEvent
import com.example.zim.helperclasses.ChatBox
import com.example.zim.states.UserChatState


@Composable
fun UserChat(userId: Int, onEvent: (UserChatEvent) -> Unit, state: UserChatState) {
    var message by remember {
        mutableStateOf("")
    }
    var hideKeyboard by remember {
        mutableStateOf(false)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        hideKeyboard = true
                    })
                },
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            UserInfoRow()
            Text(text = userId.toString())
            Text(text = state.username)
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                state.messages.map { message ->
                    when(message) {
                        is ChatBox.DateChip -> item {Text(text = "Chip: ${message.date}")}
                        is ChatBox.ReceivedMessage -> item {Text(text = "R: ${message.message}")}
                        is ChatBox.SentMessage ->  item {Text(text = "S: ${message.message}")}
                    }
                }
            }
        }

        SendMessageRow(
            message = message,
            onMessageChange = { message = it },
            hideKeyboard,
            onHideKeyboardChange = { hideKeyboard = it })
    }
}