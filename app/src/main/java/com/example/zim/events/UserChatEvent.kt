package com.example.zim.events

sealed interface UserChatEvent {
    data class LoadData(val userId: Int): UserChatEvent
}