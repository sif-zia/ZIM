package com.example.zim.screens

import android.net.Uri
import android.util.Log
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.zim.components.DateChip
import com.example.zim.components.ReceivedChatBox
import com.example.zim.components.ReceivedImageChatBox
import com.example.zim.components.SendMessageRow
import com.example.zim.components.SentChatBox
import com.example.zim.components.SentImageChatBox
import com.example.zim.components.UserInfoRow
import com.example.zim.events.ProtocolEvent
import com.example.zim.events.UserChatEvent
import com.example.zim.states.UserChatState
import com.example.zim.viewModels.ProtocolViewModel

@Composable
fun UserChat(
    userId: Int,
    onEvent: (UserChatEvent) -> Unit,
    state: UserChatState,
    navController: NavController,
    protocolViewModel: ProtocolViewModel = hiltViewModel()
) {
    var message by remember {
        mutableStateOf("")
    }
    var hideKeyboard by remember {
        mutableStateOf(false)
    }
    val lazyListState = rememberLazyListState()

    val activeUsers by protocolViewModel.activeUsers.collectAsState()

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
                status = activeUsers[state.uuid] != null,
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
                    state.messages.mapIndexed { index, message ->
                        var isFirst: Boolean = false
                        if (index == 0) {
                            isFirst = true
                            item {
                                DateChip(message.time.toLocalDate())
                            }
                        }
                        else if (state.messages[index - 1].isReceived != message.isReceived) {
                            isFirst = true
                        } else if (state.messages[index - 1].time.toLocalDate() < message.time.toLocalDate()) {
                            isFirst = true
                            item{
                                DateChip(message.time.toLocalDate())
                            }
                        }
                        if (message.isReceived && message.type=="Text") {
                            item {
                                ReceivedChatBox(message, isFirst)
                            }
                        } else if(!message.isReceived && message.type=="Text"){
                            item {
                                SentChatBox(message, isFirst)

                            }
                        }
                        else if (message.isReceived && message.type=="Image") {
                            item {
                                ReceivedImageChatBox(message, Uri.parse(message.message), isFirst)
                            }
                        } else if(!message.isReceived && message.type=="Image"){
                            item {
                                SentImageChatBox(message, Uri.parse(message.message), isFirst)
                            }
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
//                onEvent(UserChatEvent.SendMessage(message))
                protocolViewModel.onEvent(ProtocolEvent.SendMessage(message, userId))
                message = ""
            },
            onImagePicked =  { uri ->
                Log.d("UserChat", uri.toString())
                protocolViewModel.onEvent(ProtocolEvent.SendImage(uri, userId))

            }
        )

        LaunchedEffect(state.messages.size, hideKeyboard) {
            lazyListState.scrollToItem(state.messages.size + 2)
        }
    }
}