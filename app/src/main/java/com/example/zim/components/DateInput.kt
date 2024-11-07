package com.example.zim.components

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateInput(
    label: String,
    date: LocalDate?,
    onDateChange: (LocalDate) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val formattedDate = date?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) ?: ""

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
            onDateChange(selectedDate)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )


    TextField(
        value = formattedDate,
        onValueChange = { },
        label = { Text(label) },
        modifier = Modifier
            .padding(vertical = 16.dp)
            .fillMaxWidth()
            .clickable { datePickerDialog.show() },
        colors = TextFieldDefaults.textFieldColors(
            focusedTextColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
            focusedLabelColor = MaterialTheme.colorScheme.onPrimary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.primary,
            focusedIndicatorColor = MaterialTheme.colorScheme.tertiary,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.onPrimary,
            disabledLabelColor = MaterialTheme.colorScheme.onPrimary,
            disabledTextColor = MaterialTheme.colorScheme.onPrimary,
        ),
        readOnly = true,
        enabled = false
    )
}
