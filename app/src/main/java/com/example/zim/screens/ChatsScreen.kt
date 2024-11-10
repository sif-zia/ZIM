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

val chatList = listOf(
    Chat("Zainab Bilal", null, false, null, false, false, 1),
    Chat("Ali Raza", "Hey there!", true, LocalDateTime.now().minusHours(1), true, true, 1),
    Chat("Sara Ahmed", "Good morning!", true, LocalDateTime.now().minusHours(3), false, false, 2),
    Chat(
        "John Doe",
        "Let's catch up later.",
        false,
        LocalDateTime.now().minusDays(1),
        true,
        true,
        2
    ),
    Chat("Emily Clark", "Miss you!", true, LocalDateTime.now().minusDays(2), false, true, 3),
    Chat("James Smith", "See you soon!", false, LocalDateTime.now().minusDays(3), true, false, 3),
    Chat("Lily Evans", "Got it!", true, LocalDateTime.now().minusWeeks(1), false, true, 4),
    Chat("Robert Brown", "Thanks!", false, LocalDateTime.now().minusDays(6), true, false, 4),
    Chat("Sophia Wilson", "Good night!", true, LocalDateTime.now().minusDays(2), false, false, 5),
    Chat("Isla Moore", "Bye!", false, LocalDateTime.now().minusDays(7), true, false, 6),
    Chat("Liam Johnson", "Check this out", true, LocalDateTime.now().minusDays(5), false, true, 5)
)


@Composable
fun ChatsScreen(navController: NavController, state: ChatsState, onEvent: (ChatsEvent) -> Unit) {

    val horizontalPadding: Dp = 16.dp
    val verticalPadding: Dp = 12.dp
    val focusManager: FocusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }

    val chatList = remember { chatList }

    return Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null, // Removes the click animation
                interactionSource = interactionSource, // Required for clickable without animation
                onClick = { focusManager.clearFocus() }
            )
    ) {
        LogoRow(
            modifier = Modifier.padding(
                horizontal = horizontalPadding,
                vertical = verticalPadding
            ),
            expandMenu = { onEvent(ChatsEvent.ExpandMenu) }
        ) {
            DropDown(
                dropDownMenu = DropDownMenus.ChatsScreen(),
                navController = navController,
                expanded = state.menuExpanded
            ) {
                onEvent(ChatsEvent.DismissMenu)
            }
        }

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
                    name = chat.name,
                    lastMsg = chat.lastMsg,
                    isConnected = chat.isConnected,
                    time = chat.time,
                    isRead = chat.isRead,
                    isDM = chat.isDM,
                    id = chat.id,
                    navController = navController
                )
            }
        }
    };
}