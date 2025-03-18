package com.example.zim.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Search(modifier: Modifier = Modifier, query: String, onQueryChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current

    Row(modifier = modifier.fillMaxWidth()) {
        CustomTextField(
            text = query,
            onValueChange = onQueryChange,
            trailingIcon = {
                IconButton(onClick = { focusManager.clearFocus() }) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search Button",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            },
            placeholderText = "Search",
            fontSize = 11.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .weight(1f)
                .height(45.dp),
            focusManager = focusManager,
            onSend = { focusManager.clearFocus() }
        )
    }
}