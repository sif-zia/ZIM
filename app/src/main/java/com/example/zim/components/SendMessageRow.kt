package com.example.zim.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SendMessageRow(
    message: String,
    onMessageChange: (String) -> Unit,
    hideKeyboard: Boolean,
    onHideKeyboardChange: (Boolean) -> Unit,
    lazyListState: LazyListState,
    size: Int,
    onMessageSend: () -> Unit,
    onImagePicked: (Uri) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }
    var showAttachMenu by remember { mutableStateOf(false) }

    // Register the image picker launcher
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            onImagePicked(it)
        }
        showAttachMenu = false
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .imePadding()
        ) {
            Box {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    CustomTextField(
                        text = message,
                        onValueChange = onMessageChange,
                        trailingIcon = {
                            IconButton(onClick = { showAttachMenu = !showAttachMenu }) {
                                Icon(
                                    imageVector = Icons.Outlined.AttachFile,
                                    contentDescription = "Attach File Button",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        },
                        placeholderText = "Message",
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .weight(1f)
                            .height(40.dp)
                            .onFocusChanged {
                                isFocused = it.isFocused
                            },
                        focusManager = focusManager,
                        onSend = onMessageSend
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    RoundButton(onClick = {
                        focusManager.clearFocus()
                        onMessageSend()
                    }, size = 40.dp ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Send,
                            contentDescription = "Send Message Button",
                            modifier = Modifier.size(25.dp),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                // Attachment dropdown menu
                DropdownMenu(
                    expanded = showAttachMenu,
                    onDismissRequest = { showAttachMenu = false },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    DropdownMenuItem(
                        text = { Text("Gallery", color = MaterialTheme.colorScheme.onBackground) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Image,
                                contentDescription = "Gallery"
                            )
                        },
                        onClick = {
                            // Launch image picker
                            imagePicker.launch("image/*")
                        }
                    )
                    // You can add more attachment options here if needed
                }
            }
        }
    }

    if (hideKeyboard) {
        focusManager.clearFocus()
        onHideKeyboardChange(false)
    }

    LaunchedEffect(isFocused) {
        delay(1000L)
        lazyListState.animateScrollToItem(size)
        isFocused = false
    }
}