package com.example.zim.states

import com.example.zim.data.room.models.Users
import java.time.LocalDate

data class SignUpState(
    val firstName: String = "",
    val lastName: String = "",
    val DOB: LocalDate = LocalDate.now().minusYears(16),
    val IsLoggedIn: Boolean? = null,
    val User: Users? = null
)