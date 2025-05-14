package com.example.zim.screens

import android.net.Uri
import android.util.Log
import android.view.ViewTreeObserver
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.zim.components.DateChip
import com.example.zim.components.DeleteMessagesTopBar
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun UserChat(
    userId: Int,
    onEvent: (UserChatEvent) -> Unit,
    state: UserChatState,
    navController: NavController,
    protocolViewModel: ProtocolViewModel = hiltViewModel()
) {
    val routedUsers by protocolViewModel.routedUsers.collectAsState()
    var message by remember { mutableStateOf("") }
    var hideKeyboard by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val activeUsers by protocolViewModel.activeUsers.collectAsState()
    val messages = state.messages

    // Track if this is the first load
    var initialLoad by remember { mutableStateOf(true) }

    // Group messages by date
    val groupedMessages = remember(messages) {
        messages
            .groupBy { it.time.toLocalDate() }
            .toSortedMap()
    }

    onEvent(UserChatEvent.ReadAllMessages(userId))

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
            // Show different top bars based on selection mode
            if (state.isSelectionModeActive) {
                DeleteMessagesTopBar(
                    selectedCount = state.selectedMessageIds.size,
                    onBackPressed = { onEvent(UserChatEvent.ExitSelectionMode) },
                    onDeletePressed = { onEvent(UserChatEvent.DeleteSelectedMessages) },
                    onSelectAll = { onEvent(UserChatEvent.SelectAllMessages) },
                    onDeselectAll = { onEvent(UserChatEvent.DeselectAllMessages) }
                )
            } else {
                val status = if (activeUsers[state.uuid] != null) 1 else if (checkIsInNetwork(routedUsers, state.uuid)) 2 else 0
                UserInfoRow(
                    username = state.username,
                    status = status,
                    userDp = state.dpUri,
                    navController = navController
                )
            }

            if (messages.isEmpty()) {
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
                    state = lazyListState,
                    reverseLayout = false
                ) {
                    groupedMessages.forEach { (date, messagesForDate) ->
                        item {
                            DateChip(date)
                        }

                        items(messagesForDate.size) { index ->
                            val msg = messagesForDate[index]
                            val isNextSameSender = index < messagesForDate.size - 1 &&
                                    messagesForDate[index + 1].isReceived == msg.isReceived
                            val isFirst = index == 0 || messagesForDate[index - 1].isReceived != msg.isReceived

                            when {
                                msg.isReceived && msg.type == "Text" -> {
                                    ReceivedChatBox(
                                        message = msg,
                                        isFirst = isFirst,
                                        isSelectionModeActive = state.isSelectionModeActive,
                                        isSelected = state.selectedMessageIds.contains(msg.id),
                                        onLongClick = {
                                            if (!state.isSelectionModeActive) {
                                                onEvent(UserChatEvent.EnterSelectionMode)
                                                onEvent(UserChatEvent.ToggleMessageSelection(msg.id))
                                            }
                                        },
                                        onSelectToggle = { onEvent(UserChatEvent.ToggleMessageSelection(msg.id)) }
                                    )
                                }
                                !msg.isReceived && msg.type == "Text" -> {
                                    SentChatBox(
                                        message = msg,
                                        isFirst = isFirst,
                                        isSelectionModeActive = state.isSelectionModeActive,
                                        isSelected = state.selectedMessageIds.contains(msg.id),
                                        onLongClick = {
                                            if (!state.isSelectionModeActive) {
                                                onEvent(UserChatEvent.EnterSelectionMode)
                                                onEvent(UserChatEvent.ToggleMessageSelection(msg.id))
                                            }
                                        },
                                        onSelectToggle = { onEvent(UserChatEvent.ToggleMessageSelection(msg.id)) }
                                    )
                                }
                                msg.isReceived && msg.type == "Image" -> {
                                    ReceivedImageChatBox(
                                        message = msg,
                                        imageUri = Uri.parse(msg.message),
                                        isFirst = isFirst,
                                        isSelectionModeActive = state.isSelectionModeActive,
                                        isSelected = state.selectedMessageIds.contains(msg.id),
                                        onLongClick = {
                                            if (!state.isSelectionModeActive) {
                                                onEvent(UserChatEvent.EnterSelectionMode)
                                                onEvent(UserChatEvent.ToggleMessageSelection(msg.id))
                                            }
                                        },
                                        onSelectToggle = { onEvent(UserChatEvent.ToggleMessageSelection(msg.id)) }
                                    )
                                }
                                !msg.isReceived && msg.type == "Image" -> {
                                    SentImageChatBox(
                                        message = msg,
                                        imageUri = Uri.parse(msg.message),
                                        isFirst = isFirst,
                                        isSelectionModeActive = state.isSelectionModeActive,
                                        isSelected = state.selectedMessageIds.contains(msg.id),
                                        onLongClick = {
                                            if (!state.isSelectionModeActive) {
                                                onEvent(UserChatEvent.EnterSelectionMode)
                                                onEvent(UserChatEvent.ToggleMessageSelection(msg.id))
                                            }
                                        },
                                        onSelectToggle = { onEvent(UserChatEvent.ToggleMessageSelection(msg.id)) }
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(72.dp)) }
                    item { Spacer(modifier = Modifier.imePadding()) }
                }
            }
        }

        SendMessageRow(
            message = message,
            onMessageChange = { message = it },
            hideKeyboard = hideKeyboard,
            onHideKeyboardChange = {
                hideKeyboard = it
                if (!it) {
                    forceScrollToBottom(lazyListState, coroutineScope)
                }
            },
            lazyListState = lazyListState,
            size = messages.size,
            onMessageSend = {
                // Important: Don't set hideKeyboard to true when sending a message
                protocolViewModel.onEvent(ProtocolEvent.SendMessage(message, userId))
                message = ""
                // Just scroll to bottom without hiding keyboard
                forceScrollToBottom(lazyListState, coroutineScope)
            },
            onImagePicked = { uri ->
                Log.d("UserChat", uri.toString())
                protocolViewModel.onEvent(ProtocolEvent.SendImage(uri, userId))
                // For image selection, we may want to keep the keyboard open too
                forceScrollToBottom(lazyListState, coroutineScope)
            }
        )
    }

    // Keyboard listener
    val view = LocalView.current
    DisposableEffect(view) {
        val keyboardListener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = android.graphics.Rect()
            view.getWindowVisibleDisplayFrame(rect)
            val screenHeight = view.rootView.height
            val keypadHeight = screenHeight - rect.bottom

            if (keypadHeight > screenHeight * 0.15) {
                forceScrollToBottom(lazyListState, coroutineScope)
            }
        }

        view.viewTreeObserver.addOnGlobalLayoutListener(keyboardListener)
        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(keyboardListener)
        }
    }

    // Handle initial load and message updates
    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            // Use multiple delays to ensure layout completes
            delay(50)
            forceScrollToBottom(lazyListState, coroutineScope)
            delay(300)
            forceScrollToBottom(lazyListState, coroutineScope)
        }
    }

    // First-load special handling
    LaunchedEffect(Unit) {
        if (initialLoad && messages.isNotEmpty()) {
            delay(100)
            forceScrollToBottom(lazyListState, coroutineScope)
            delay(500)
            forceScrollToBottom(lazyListState, coroutineScope)
            initialLoad = false
        }
    }
}

// More robust bottom scrolling
fun forceScrollToBottom(lazyListState: LazyListState, coroutineScope: CoroutineScope) {
    coroutineScope.launch {
        try {
            val totalItems = lazyListState.layoutInfo.totalItemsCount
            if (totalItems > 0) {
                // First try direct scroll
                lazyListState.scrollToItem(index = totalItems - 1)

                // Then try with offset to make sure it's fully visible
                delay(50)
                lazyListState.scrollToItem(
                    index = totalItems - 1,
                    scrollOffset = 0
                )
            }
        } catch (e: Exception) {
            Log.e("UserChat", "Error scrolling to bottom: ${e.message}")
        }
    }
}

// Helper function to check if a user's UUID is in the routed users
fun checkIsInNetwork(routedUsers: Map<String, Any>, uuid: String): Boolean {
    return routedUsers.containsKey(uuid)
}