package com.example.zim.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.zim.components.DateInput
import com.example.zim.components.TextInput
import com.example.zim.events.UpdateUserEvent
import com.example.zim.states.SignUpState
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

fun validateNameProfile(name: String, newName: String, context: Context): String {
    if (newName.isEmpty() || newName.all { it.isLetter() || it.isWhitespace() }) {
        return if (newName.isNotEmpty()) {
            if (newName.length <= 30) {
                newName[0].uppercaseChar() + newName.substring(1)
            } else {
                Toast.makeText(context, "Only 30 characters allowed!", Toast.LENGTH_SHORT).show()
                name
            }
        } else ""
    } else {
        Toast.makeText(context, "Only alphabets are allowed!", Toast.LENGTH_SHORT).show()
        return name
    }
}

/**
 * Handles the persistence of content URIs by making a local copy in app's storage
 */
fun persistImageUri(context: Context, imageUri: Uri): String {
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
        Log.e("ProfileScreen", "Failed to persist image: ${e.message}", e)
        // Return original URI as fallback
        return imageUri.toString()
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    state: SignUpState,
    onEvent: (UpdateUserEvent) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var saveSuccess by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Create temporary states to track edited values
    var tempFirstName by remember { mutableStateOf("") }
    var tempLastName by remember { mutableStateOf("") }
    var tempDOB by remember { mutableStateOf(LocalDate.now().minusYears(16)) }
    var tempProfilePicture by remember { mutableStateOf("") }

    // Initialize the form values when the state is ready or changes
    LaunchedEffect(state.User) {
        state.User?.let { user ->
            tempFirstName = user.fName
            tempLastName = user.lName ?: ""
            tempDOB = user.DOB ?: LocalDate.now().minusYears(16)
            tempProfilePicture = user.cover?.toString() ?: ""
        } ?: run {
            // Fallback to state values if User is null
            tempFirstName = state.firstName
            tempLastName = state.lastName
            tempDOB = state.DOB
            tempProfilePicture = ""
        }
    }

    // Track if any changes have been made by comparing with the original values
    val changesMade = remember(tempFirstName, tempLastName, tempDOB, tempProfilePicture, state.User) {
        val userCoverString = state.User?.cover?.toString() ?: ""

        state.User != null && (
                tempFirstName != state.User.fName ||
                        tempLastName != (state.User.lName ?: "") ||
                        tempDOB != (state.User.DOB ?: LocalDate.now().minusYears(16)) ||
                        (tempProfilePicture != userCoverString && tempProfilePicture.isNotEmpty())
                )
    }

    // Image picker launcher with proper permissions
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Use the ViewModel's method to persist the image
            val persistedUri = persistImageUri(context, it)

            // Update the profile picture
            tempProfilePicture = persistedUri
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Profile",
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                modifier = Modifier.height(40.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Profile Picture Section
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier.padding(bottom = 18.dp)
            ) {
                // Profile Image
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(BorderStroke(1.dp, Color.LightGray), CircleShape)
                ) {
                    val imageUri = remember(tempProfilePicture, state.User) {
                        tempProfilePicture.takeIf { it.isNotEmpty() }
                            ?: state.User?.cover?.toString() ?: ""
                    }

                    if (imageUri.isNotEmpty()) {
                        // Create a more robust image loading configuration
                        val imageRequest = remember(imageUri) {
                            ImageRequest.Builder(context)
                                .data(imageUri)
                                .memoryCachePolicy(CachePolicy.DISABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .build()
                        }

                        // Try loading the image with the improved request
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = imageRequest
                            ),
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(100.dp)
                                .align(Alignment.Center),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Camera Icon for Image Selection
                FloatingActionButton(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier.size(50.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change Profile Picture",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // Profile Information
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                TextInput(
                    label = "First Name",
                    text = tempFirstName,
                    onTextChange = { newText ->
                        tempFirstName = validateNameProfile(tempFirstName, newText, context)
                    }
                )

                TextInput(
                    label = "Last Name",
                    text = tempLastName,
                    onTextChange = { newText ->
                        tempLastName = validateNameProfile(tempLastName, newText, context)
                    }
                )

                DateInput(
                    label = "Date of Birth",
                    date = tempDOB,
                    onDateChange = { newDate ->
                        tempDOB = newDate
                    }
                )

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = {
                        if (!isSaving && changesMade) {
                            isSaving = true

                            // Get the current user ID from state
                            val userId = state.User?.id ?: 0

                            // Apply changes to the state - make sure we're passing the raw URI string
                            onEvent(UpdateUserEvent.UpdateUser(
                                id = userId,
                                deviceName = state.User?.deviceName,
                                deviceAddress = state.User?.deviceAddress,
                                fName = tempFirstName,
                                lName = tempLastName,
                                DOB = tempDOB.toString(),
                                cover = tempProfilePicture,  // This is the persisted URI string
                                puKey = state.User?.UUID
                            ))

                            // Show success message
                            scope.launch {
                                delay(800) // Short delay to simulate saving
                                saveSuccess = true
                                delay(2000) // Show success icon for 2 seconds
                                saveSuccess = false
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = MaterialTheme.shapes.medium,
                    enabled = changesMade && !isSaving
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isSaving && !saveSuccess) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else if (saveSuccess) {
                            // Show tick icon when save is successful
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Saved",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SAVED",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "SAVE CHANGES",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}