package com.example.zim.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.NotificationAdd
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zim.R

@Composable
fun SendMessageRow(message: String, onMessageChange: (String) -> Unit, hideKeyboard: Boolean, onHideKeyboardChange: (Boolean) -> Unit) {
    val focusManager = LocalFocusManager.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .imePadding()
        ) {
            TextField(
                value = message,
                onValueChange = onMessageChange,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.AttachFile,
                        contentDescription = "Attach File Button",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                },
                placeholder = { Text(text = "Message") },
                modifier = Modifier.clip(RoundedCornerShape(50)).weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.primary.copy(0.9f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.primary.copy(0.9f),
                    cursorColor = MaterialTheme.colorScheme.secondary,
                    focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onPrimary,
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { focusManager.clearFocus() })
            )
            Spacer(modifier = Modifier.width(8.dp))
            RoundButton(modifier = Modifier.weight(1f), onClick = { /*TODO*/ }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "Send Message Button",
                    modifier = Modifier.size(25.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
    if (hideKeyboard) {
        focusManager.clearFocus()

        onHideKeyboardChange(false)
    }
}