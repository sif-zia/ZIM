package com.example.zim.helperclasses

import java.time.LocalDate
import java.time.LocalTime

enum class Status {
    WAITING,
    SENT,
    RECEIVED,
    READ,
    FAILED
}

sealed interface ChatBox {
    data class SentMessage(
        val message: String,
        val time: LocalTime,
        val date: LocalDate,
        var isFirst: Boolean = true
    ) : ChatBox

    data class ReceivedMessage(
        val message: String,
        val time: LocalTime,
        val date: LocalDate,
        var isFirst: Boolean = true,
        val status: Status = Status.SENT
    ) : ChatBox

    data class DateChip(val date: LocalDate) : ChatBox
}