package com.example.zim.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex

@Composable
fun DropDownInput(label: String, options: List<String>, modifier: Modifier = Modifier,selectedText:String,onSelect: (String) -> Unit) {
    var mExpanded by remember { mutableStateOf(false) }
    var mTextFieldSize by remember { mutableStateOf(Size.Zero) }

    val icon = if (mExpanded)
        Icons.Filled.KeyboardArrowUp
    else
        Icons.Filled.KeyboardArrowDown


    Column {


        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(33))
                .background(MaterialTheme.colorScheme.secondary),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = selectedText,
                onValueChange =  onSelect ,
                modifier = Modifier
                    .clickable { mExpanded = !mExpanded }
                    .onGloballyPositioned { coordinates ->
                        // This value is used to assign to
                        // the DropDown the same width
                        mTextFieldSize = coordinates.size.toSize()
                    }
                    .weight(1f),
                label = { Text(label, color = MaterialTheme.colorScheme.primary.copy(0.66f)) },
                readOnly = true,
                enabled = false,
                colors = TextFieldDefaults.colors(
                    disabledContainerColor = MaterialTheme.colorScheme.secondary,
                    disabledTextColor = MaterialTheme.colorScheme.primary,
                    disabledLabelColor = MaterialTheme.colorScheme.primary,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.primary
                ),
            )
            Icon(icon, "Drop Down Expand Icon",
                Modifier
                    .clickable { mExpanded = !mExpanded }
                    .padding(horizontal = 5.dp))
        }

        DropdownMenu(
            expanded = mExpanded,
            onDismissRequest = { mExpanded = false },
            modifier = Modifier
                .width(with(LocalDensity.current) { mTextFieldSize.width.toDp() })
        ) {
            options.forEach { itemLabel ->
                DropdownMenuItem(
                    onClick = {
                        mExpanded = false
                        onSelect(itemLabel)
                    },
                    text = { Text(text = itemLabel, color = MaterialTheme.colorScheme.primary) }
                )
            }
        }
    }

}