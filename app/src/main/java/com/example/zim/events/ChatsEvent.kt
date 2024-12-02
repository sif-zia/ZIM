package com.example.zim.events

sealed interface ChatsEvent {
    data object ExpandMenu: ChatsEvent
    data object DismissMenu: ChatsEvent
    data class ChangeQuery(val newQuery: String): ChatsEvent
    data class UpdateStatus(val connectionStatus: Map<String, Boolean>): ChatsEvent;
}