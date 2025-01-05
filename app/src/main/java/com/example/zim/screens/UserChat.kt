package com.example.zim.screens

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.zim.components.DateChip
import com.example.zim.components.ReceivedChatBox
import com.example.zim.components.SendMessageRow
import com.example.zim.components.SentChatBox
import com.example.zim.components.UserInfoRow
import com.example.zim.events.UserChatEvent
import com.example.zim.helperclasses.ChatBox
import com.example.zim.states.UserChatState

@Composable
fun UserChat(
    userId: Int,
    onEvent: (UserChatEvent) -> Unit,
    state: UserChatState,
    navController: NavController
) {
    var message by remember {
        mutableStateOf("")
    }
    var hideKeyboard by remember {
        mutableStateOf(false)
    }
    val lazyListState = rememberLazyListState()

    onEvent(UserChatEvent.ReadAllMessages(userId))
//    onEvent(UserChatEvent.ConnectToUser(userId))

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
            UserInfoRow(
                username = state.username,
                status = state.connected,
                userDp = state.dpUri,
                navController = navController
            )
            if (state.messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No messages yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.66f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = lazyListState
                ) {
                    state.messages.map { message ->
                        when (message) {
                            is ChatBox.DateChip -> item { DateChip(date = message.date) }
                            is ChatBox.ReceivedMessage -> item { ReceivedChatBox(message) }
                            is ChatBox.SentMessage -> item { SentChatBox(message) }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }

                    item {
                        Spacer(modifier = Modifier.imePadding())
                    }
                }
            }
        }

        SendMessageRow(
            message = message,
            onMessageChange = { message = it },
            hideKeyboard,
            onHideKeyboardChange = { hideKeyboard = it },
            lazyListState = lazyListState,
            size = state.messages.size,
            onMessageSend = {
                onEvent(UserChatEvent.SendMessage(message))
                message = ""
            }
        )

        LaunchedEffect(state.messages.size, hideKeyboard) {
            lazyListState.scrollToItem(state.messages.size + 2)
        }
    }
}