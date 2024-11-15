package com.example.zim.screens

import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.zim.components.ChatRow
import com.example.zim.components.LogoRow
import com.example.zim.components.Search
import java.time.LocalDateTime
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import com.example.zim.components.DropDown
import com.example.zim.events.ChatsEvent
import com.example.zim.helperclasses.Chat
import com.example.zim.navigation.DropDownMenus
import com.example.zim.states.ChatsState



@Composable
fun ChatsScreen(navController: NavController, state: ChatsState, onEvent: (ChatsEvent) -> Unit) {

    val horizontalPadding: Dp = 16.dp
    val verticalPadding: Dp = 12.dp
    val focusManager: FocusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }

    return Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null, // Removes the click animation
                interactionSource = interactionSource, // Required for clickable without animation
                onClick = { focusManager.clearFocus() }
            )
    ) {

        Search(
            modifier = Modifier.padding(
                horizontal = horizontalPadding,
                vertical = verticalPadding
            ),
            query = state.query,
            onQueryChange = {onEvent(ChatsEvent.ChangeQuery(newQuery = it))}
        )

        LazyColumn(
            modifier = Modifier
                .padding(top = verticalPadding)
                .fillMaxHeight(),
            state = rememberLazyListState()
        ) {
            items(state.chats) { chat ->
                ChatRow(
                    name = "${chat.fName} ${chat.lName}",
                    lastMsg = chat.lastMsg,
                    time = chat.time,
                    unReadMsgs = chat.unReadMsgs,
                    id = chat.id,
                    navController = navController
                )
            }
        }
    };
}