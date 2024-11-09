package com.example.zim.screens

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
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager

data class Chat(
    val name: String,
    val lastMsg: String,
    val isConnected: Boolean,
    val time: LocalDateTime,
    val isRead: Boolean
)

val chatList = listOf(
    Chat("Zainab Bilal", "Dheet", false, LocalDateTime.now().minusMinutes(5), false),
    Chat("Ali Raza", "Hey there!", true, LocalDateTime.now().minusHours(1), true),
    Chat("Sara Ahmed", "Good morning!", true, LocalDateTime.now().minusHours(3), false),
    Chat("John Doe", "Let's catch up later.", false, LocalDateTime.now().minusDays(1), true),
    Chat("Emily Clark", "Miss you!", true, LocalDateTime.now().minusDays(2), false),
    Chat("James Smith", "See you soon!", false, LocalDateTime.now().minusDays(3), true),
    Chat("Lily Evans", "Got it!", true, LocalDateTime.now().minusWeeks(1), false),
    Chat("Robert Brown", "Thanks!", false, LocalDateTime.now().minusDays(6), true),
    Chat("Sophia Wilson", "Good night!", true, LocalDateTime.now().minusDays(2), false),
    Chat("Isla Moore", "Bye!", false, LocalDateTime.now().minusDays(7), true),
    Chat("Liam Johnson", "Check this out", true, LocalDateTime.now().minusDays(5), false)
)


@Composable
fun ChatsScreen(navController: NavController) {

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
        LogoRow(
            menu = true,
            modifier = Modifier.padding(
                horizontal = horizontalPadding,
                vertical = verticalPadding
            )
        )

        Search(
            modifier = Modifier.padding(
                horizontal = horizontalPadding,
                vertical = verticalPadding
            )
        )

        LazyColumn(
            modifier = Modifier
                .padding(top = verticalPadding)
                .fillMaxHeight()
        ) {
            items(chatList) { chat ->
                ChatRow(
                    name = chat.name,
                    lastMsg = chat.lastMsg,
                    isConnected = chat.isConnected,
                    time = chat.time,
                    isRead = chat.isRead
                )
            }
        }
    };
}