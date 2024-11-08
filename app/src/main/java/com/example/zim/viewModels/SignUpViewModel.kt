package com.example.zim.viewModels

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zim.data.room.Dao.UserDao
import com.example.zim.data.room.models.CurrentUser
import com.example.zim.data.room.models.Users
import com.example.zim.events.SignUpEvent
import com.example.zim.states.SignUpState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignUpViewModel(
    private val userDao: UserDao
) : ViewModel() {
    private val _state = MutableStateFlow(SignUpState())
    val state: StateFlow<SignUpState> = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        SignUpState()
    )

    fun onEvent(event: SignUpEvent) {
        when (event) {
            is SignUpEvent.SaveUser -> {
                val firstName=state.value.firstName
                val lastName=state.value.lastName
                val DOB=state.value.DOB
                //viewmodelscope allows calling Async await
                if (firstName.isBlank() || lastName.isBlank())
                    return;


                try {
                    // Log inputs for debugging
                    Log.d("Save User", "$firstName $lastName")

                    // Insert the user into the database and get the row ID
                    viewModelScope.launch {
                    val userId: Long = userDao.insertUser(
                        Users(fName = firstName, lName = lastName, DOB = DOB)
                    )
                        Log.d("Save User", "User inserted with ID: $userId")
                    }


                    // Insert into CurrentUser (uncomment if needed)
                    // userDao.insertCurrUser(CurrentUser(userIDFK = userId.toInt()))

                    // Update state to reflect user is logged in
                    _state.update { it.copy(IsLoggedIn = true) }
                } catch (e: Exception) {
                    Log.e("Save User", "Error saving user: ${e.message}")
                }
//                    userDao.insertCurrUser(
//                        CurrentUser(
//                            userIDFK = ID.toInt()
//                        )
//                    )
                _state.update { it.copy(IsLoggedIn = true) }
            }

            is SignUpEvent.SetDOB -> {
                _state.update { it.copy(DOB = event.dob) }
            }

            is SignUpEvent.SetFirstName -> {
                _state.update { it.copy(firstName = event.firstName) }
            }

            is SignUpEvent.SetLastName -> {
                _state.update { it.copy(lastName = event.lastName) }
            }

            is SignUpEvent.CheckLogin -> {
                viewModelScope.launch {
                    _state.update { it.copy(IsLoggedIn = userDao.doesCurrentUserExist()) }
                }

            }
        }

    }
}