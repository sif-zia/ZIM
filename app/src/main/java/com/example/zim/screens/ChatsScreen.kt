package com.example.zim.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.zim.components.ChatRow
import com.example.zim.components.DeleteTopBar
import com.example.zim.components.Search
import com.example.zim.events.ChatsEvent
import com.example.zim.states.ChatsState
import com.example.zim.viewModels.ChatsViewModel
import com.example.zim.viewModels.ProtocolViewModel

fun checkIsInNetwork(routedUsers: Map<String, String>, uuid: String): Boolean {
    return routedUsers.containsKey(uuid)
}

@Composable
fun ChatsScreen(
    navController: NavController,
    state: ChatsState,
    onEvent: (ChatsEvent) -> Unit,
    protocolViewModel: ProtocolViewModel = hiltViewModel(),
    chatsViewModel: ChatsViewModel = hiltViewModel()
) {
    val logs by chatsViewModel.logs.collectAsState()
    val routedUsers by protocolViewModel.routedUsers.collectAsState()

    val horizontalPadding: Dp = 16.dp
    val verticalPadding: Dp = 12.dp
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val activeUsers by protocolViewModel.activeUsers.collectAsState()

    return Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null, // Removes the click animation
                interactionSource = interactionSource, // Required for clickable without animation
                onClick = {
                    focusManager.clearFocus()
                    if (state.isSelectionModeActive) {
                        onEvent(ChatsEvent.ExitSelectionMode)
                    }
                }
            )
    ) {
        if (state.isSelectionModeActive) {
            DeleteTopBar(
                selectedCount = state.selectedChatIds.size,
                onBackPressed = { onEvent(ChatsEvent.ExitSelectionMode) },
                onDeletePressed = { onEvent(ChatsEvent.DeleteSelectedChats) }
            )
        } else {
            Search(
                modifier = Modifier.padding(
                    horizontal = horizontalPadding,
                    vertical = verticalPadding
                ),
                query = state.query,
                onQueryChange = { onEvent(ChatsEvent.ChangeQuery(newQuery = it)) }
            )
        }

        val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3F)

        LazyColumn(
            modifier = Modifier.padding(top = verticalPadding),
            state = rememberLazyListState()
        ) {
            items(state.chats) { chat ->
                val isConnected = if (activeUsers[chat.UUID] != null) 1 else if (checkIsInNetwork(routedUsers, chat.UUID)) 2 else 0

                HorizontalDivider(color = borderColor)
                ChatRow(
                    name = "${chat.fName} ${chat.lName}",
                    lastMsg = chat.lastMsg,
                    time = chat.time,
                    unReadMsgs = chat.unReadMsgs,
                    id = chat.id,
                    navController = navController,
                    isConnected = isConnected,
                    lastMsgType = chat.lastMsgType,
                    isSent = chat.isSent,
                    isSelectionModeActive = state.isSelectionModeActive,
                    isSelected = state.selectedChatIds.contains(chat.id),
                    onLongClick = { onEvent(ChatsEvent.EnterSelectionMode(chat.id)) },
                    onSelectToggle = { onEvent(ChatsEvent.ToggleChatSelection(chat.id)) }
                )
            }
        }

        if(state.chats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No Chat(s)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary.copy(0.66f)
                )
            }
        } else {
            HorizontalDivider(color = borderColor)
        }


//        LazyColumn {
//            items(logs.subList(maxOf(logs.size - 10, 0), logs.size)) { log ->
//                Text(
//                    text = "${log.timestamp} [${log.tag}] ${log.message}",
//                    color = when (log.type) {
//                        LogType.DEBUG -> Color.Gray
//                        LogType.ERROR -> Color.Red
//                        LogType.INFO -> Color.White
//                        LogType.WARNING -> Color.Yellow
//                    },
//                    modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding)
//                )
//            }
//        }
    }
}