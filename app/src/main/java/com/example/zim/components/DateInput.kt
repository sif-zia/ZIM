package com.example.zim.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateInput(
    label: String,
    date: LocalDate?,
    onDateChange: (LocalDate) -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val formattedDate = date?.format(formatter) ?: ""

    var showDatePicker by remember { mutableStateOf(false) }

    // Convert LocalDate to milliseconds
    val initialMillis = date?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

    Column {
        TextField(
            value = formattedDate,
            onValueChange = { }, // Read-only
            label = { Text(label) },
            modifier = Modifier
                .padding(vertical = 16.dp)
                .fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                cursorColor = MaterialTheme.colorScheme.onSurface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha=0.9f),
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha=0.9f),
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha=0.7f),
            ),
            readOnly = true,
            enabled = true,
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }, colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)) {
                    Icon(  // Wrap the vector asset in an Icon component
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = "Select Date"
                    )

                }
            }
        )

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = initialMillis,
                initialDisplayMode = DisplayMode.Picker
            )

            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            onDateChange(selectedDate)
                        }
                        showDatePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}