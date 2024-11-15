package com.example.zim.events

import java.time.LocalDate

sealed interface SignUpEvent {
    //stored information related to SignUp Event
    data class SetFirstName(val firstName: String) : SignUpEvent
    data class SetLastName(val lastName: String) : SignUpEvent
    data class SetDOB(val dob: LocalDate) : SignUpEvent
    data object SaveUser : SignUpEvent
}