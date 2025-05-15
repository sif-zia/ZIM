package com.example.zim.screens

import android.net.Uri
import android.view.ViewTreeObserver
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.zim.components.DateChip
import com.example.zim.components.GroupReceivedChatBox
import com.example.zim.components.GroupSentChatBox
import com.example.zim.components.SendMessageRow
import com.example.zim.components.SentChatBox
import com.example.zim.components.SentImageChatBox
import com.example.zim.helperclasses.GroupChatContent
import com.example.zim.viewModels.GroupsViewModel
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun GroupChat(
    groupId: Int,
    groupUri: Uri? = null,
    navController: NavController,
    groupsViewModel: GroupsViewModel = hiltViewModel()
) {
    val groupsState by groupsViewModel.state.collectAsState()
    var message by remember { mutableStateOf("") }
    var hideKeyboard by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()


    // Track if this is the first load
    var initialLoad by remember { mutableStateOf(true) }

    // Group messages by date

    val groupedMessages = groupsState.currentGroupChats.groupBy { it.time.toLocalDate() }
        .mapValues { entry ->
            entry.value.sortedByDescending { it.time }
        }
        .toSortedMap(compareByDescending { it })


    // Read all messages when opening the chat
    LaunchedEffect(groupId) {
//        protocolViewModel.onEvent(ProtocolEvent.ReadAllGroupMessages(groupId))
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
            // Group info top bar
            GroupInfoRow(
                navController = navController
            )

            if (groupsState.currentGroupChats.isEmpty()) {
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
                                    messagesForDate[index + 1].isReceived == msg.isReceived &&
                                    messagesForDate[index + 1].senderFName == msg.senderFName
                            val isFirst = index == 0 ||
                                    messagesForDate[index - 1].isReceived != msg.isReceived ||
                                    messagesForDate[index - 1].senderFName != msg.senderFName

                            when {
                                msg.isReceived && msg.type == "Text" -> {
                                    GroupReceivedChatBox(
                                        message = msg,
                                        isFirst = isFirst,
                                        isSelectionModeActive = false,
                                        isSelected = false,
                                        onLongClick = { },
                                        onSelectToggle = { }
                                    )
                                }
                                !msg.isReceived && msg.type == "Text" -> {
                                    GroupSentChatBox(
                                        message = msg,
                                        isFirst = isFirst,
                                        isSelectionModeActive = false,
                                        isSelected = false,
                                        onLongClick = { },
                                        onSelectToggle = { }
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
            size = groupsState.currentGroupChats.size,
            onMessageSend = {
                // Send Meesage
//                protocolViewModel.onEvent(ProtocolEvent.SendGroupMessage(message, groupId))
                message = ""
                forceScrollToBottom(lazyListState, coroutineScope)
            },
            onImagePicked = null
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
    LaunchedEffect(groupsState.currentGroupChats) {
        if (groupsState.currentGroupChats.isNotEmpty()) {
            // Use multiple delays to ensure layout completes
            delay(50)
            forceScrollToBottom(lazyListState, coroutineScope)
            delay(300)
            forceScrollToBottom(lazyListState, coroutineScope)
        }
    }

    // First-load special handling
    LaunchedEffect(Unit) {
        if (initialLoad && groupsState.currentGroupChats.isNotEmpty()) {
            delay(100)
            forceScrollToBottom(lazyListState, coroutineScope)
            delay(500)
            forceScrollToBottom(lazyListState, coroutineScope)
            initialLoad = false
        }
    }
}

// Group info row composable
@Composable
fun GroupInfoRow(
    navController: NavController,
    groupsViewModel: GroupsViewModel = hiltViewModel(),
) {

    val groupsState by groupsViewModel.state.collectAsState()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(0.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }

            // Group profile picture
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {

                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = "Group",
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )

            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = groupsState.currentGroupName.ifEmpty { "Unknown" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Group Chat",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            IconButton(
                onClick = {

                }
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Group Info"
                )
            }
        }
    }
}