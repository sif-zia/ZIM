package com.example.zim.viewModels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zim.data.room.Dao.UserDao
import com.example.zim.data.room.models.CurrentUser
import com.example.zim.data.room.models.Users
import com.example.zim.events.SignUpEvent
import com.example.zim.events.UpdateUserEvent
import com.example.zim.states.SignUpState
import com.example.zim.utils.Crypto
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val userDao: UserDao,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _state = MutableStateFlow(SignUpState())
    val state: StateFlow<SignUpState> = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        SignUpState()
    )

    init {
        checkLogin()
        loadCurrentUser()
    }

    fun onEvent(event: SignUpEvent) {
        when (event) {
            is SignUpEvent.SaveUser -> {
                val firstName = state.value.firstName
                val lastName = state.value.lastName
                val DOB = state.value.DOB

                if (firstName.isBlank() || lastName.isBlank())
                    return

                try {
                    viewModelScope.launch {
                        val crypto = Crypto()
                        val keyPair = crypto.generateECKeyPair()
                        val encodedPublicKey = crypto.encodePublicKey(keyPair.public)
                        val encodedPrivateKey = crypto.encodePrivateKey(keyPair.private)
                        val userId: Long = userDao.insertUser(
                            Users(fName = firstName, lName = lastName, DOB = DOB, UUID = encodedPublicKey)
                        )
                        if (userId >= 0) {
                            userDao.insertCurrUser(
                                CurrentUser(
                                    userIDFK = userId.toInt(),
                                    prKey = encodedPrivateKey
                                )
                            )
                        }

                        loadCurrentUser()
                    }
                } catch (e: Exception) {
                    Log.d("Save User", "Error saving user: ${e.message}")
                }
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
        }
    }

    // Persist image URI by copying to app's internal storage
    fun persistImageUri(imageUri: Uri): String {
        try {
            // Create a unique filename based on timestamp
            val fileName = "profile_${System.currentTimeMillis()}.jpg"
            val outputFile = File(context.filesDir, fileName)

            // Copy the image data to our internal storage
            context.contentResolver.openInputStream(imageUri)?.use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Return the file URI that we can access anytime
            return Uri.fromFile(outputFile).toString()
        } catch (e: Exception) {
            Log.e("SignUpViewModel", "Failed to persist image: ${e.message}", e)
            // Return original URI as fallback
            return imageUri.toString()
        }
    }

    // Handle UpdateUserEvent
    fun onUpdateEvent(event: UpdateUserEvent) {
        when (event) {
            is UpdateUserEvent.UpdateUser -> {
                viewModelScope.launch {
                    try {
                        // Get the current user to preserve any fields not being updated
                        val currentUser = userDao.getUserById(event.id)

                        // Persist the cover image if a new URI is provided
                        val persistedCoverUri = if (!event.cover.isNullOrEmpty()) {
                            persistImageUri(Uri.parse(event.cover))
                        } else null

                        // Create updated user object with all fields
                        val updatedUser = Users(
                            id = event.id,
                            deviceName = event.deviceName ?: currentUser.deviceName,
                            deviceAddress = event.deviceAddress ?: currentUser.deviceAddress,
                            fName = event.fName ?: currentUser.fName,
                            lName = event.lName ?: currentUser.lName,
                            DOB = event.DOB?.let { LocalDate.parse(it) } ?: currentUser.DOB,
                            // Use persisted URI or keep existing one
                            cover = persistedCoverUri?.let { Uri.parse(it) } ?: currentUser.cover,
                            UUID = event.puKey ?: currentUser.UUID
                        )

                        // Update user in database
                        userDao.updateUser(updatedUser)

                        // Update the state with the new user information
                        _state.update { currentState ->
                            currentState.copy(
                                firstName = updatedUser.fName,
                                lastName = updatedUser.lName ?: "",
                                DOB = updatedUser.DOB ?: LocalDate.now().minusYears(16),
                                User = updatedUser
                            )
                        }

                        Log.d("UpdateUser", "User updated successfully")
                    } catch (e: Exception) {
                        Log.e("UpdateUser", "Error updating user: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun checkLogin() {
        viewModelScope.launch {
            _state.update { it.copy(IsLoggedIn = userDao.doesCurrentUserExist()) }
        }
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                val currentUser = userDao.getCurrentUser()
                if (currentUser != null) {
                    val userId = currentUser.currentUser.userIDFK
                    val user = userDao.getUserById(userId)

                    if (userId != -1) {
                        // Extensive logging for debugging
                        Log.d("SignUpViewModel", "Loaded user: ${user.id}, Name: ${user.fName} ${user.lName}")
                        Log.d("SignUpViewModel", "Cover URI object: ${user.cover}")
                        Log.d("SignUpViewModel", "Cover URI toString: ${user.cover?.toString()}")
                        Log.d("SignUpViewModel", "Cover URI path: ${user.cover?.path}")

                        _state.update {
                            it.copy(
                                firstName = user.fName,
                                lastName = user.lName ?: "",
                                DOB = user.DOB ?: LocalDate.now().minusYears(16),
                                IsLoggedIn = true,
                                User = user  // Important: store the full user object in state
                            )
                        }
                    } else {
                        Log.d("SignUpViewModel", "User not found")
                    }
                } else {
                    Log.d("SignUpViewModel", "No current user found")
                }
            } catch (e: Exception) {
                Log.e("SignUpViewModel", "Error loading current user: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}