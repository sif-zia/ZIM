package com.example.zim.screens

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.zim.components.ChatRow
import com.example.zim.components.Search
import com.example.zim.events.ChatsEvent
import com.example.zim.events.GroupsEvent
import com.example.zim.viewModels.GroupsViewModel

@Composable
fun GroupsScreen(navController: NavController, groupsViewModel: GroupsViewModel = hiltViewModel()) {
    val horizontalPadding: Dp = 16.dp
    val verticalPadding: Dp = 12.dp
    val state by groupsViewModel.state.collectAsState()

    Column(modifier = Modifier
        .fillMaxSize()) {
        Search(
            modifier = Modifier.padding(
                horizontal = horizontalPadding,
                vertical = verticalPadding
            ),
            query = state.searchQuery,
            onQueryChange = { groupsViewModel.onEvent(GroupsEvent.UpdateSearchQuery(it)) },
        )

        val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3F)

        LazyColumn(
            modifier = Modifier.padding(top = verticalPadding),
            state = rememberLazyListState()
        ) {
            items(state.groups) { group ->

                HorizontalDivider(color = borderColor)
                ChatRow(
                    name = group.groupName,
                    lastMsg = group.lastMessage,
                    time = group.lastMessageTime,
                    unReadMsgs = group.unreadMessages,
                    id = group.groupId,
                    navController = navController,
                    isConnected = null,
                    lastMsgType = group.lastMessageType,
                    isSent = null,
                    isSelectionModeActive = false,
                    isSelected = false,
                    onLongClick = {  },
                    onSelectToggle = {  },
                    isDM = false
                )
            }
        }

        if(state.groups.isEmpty()) {
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
    }


}