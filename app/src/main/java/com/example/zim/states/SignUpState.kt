package com.example.zim.states

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.time.LocalDate

data class SignUpState(
    val firstName: String = "",
    val lastName: String = "",
    val DOB: LocalDate = LocalDate.now().minusYears(16),
    val IsLoggedIn: Boolean? = null
)