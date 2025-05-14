package com.example.zim.events

sealed interface ChatsEvent {
    data object ExpandMenu: ChatsEvent
    data object DismissMenu: ChatsEvent
    data class ChangeQuery(val newQuery: String): ChatsEvent

    data class EnterSelectionMode(val chatId: Int? = null) : ChatsEvent
    data class ToggleChatSelection(val chatId: Int) : ChatsEvent
    data object ExitSelectionMode : ChatsEvent
    data object DeleteSelectedChats : ChatsEvent
}