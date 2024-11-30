package com.example.zim.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun formatDate(dateTime: LocalDate?): String {
    if (dateTime == null) return ""

    val now = LocalDate.now()

    return when {
        dateTime.isEqual(now.minusDays(1)) -> {
            // If the date is yesterday, return "Yesterday"
            "Yesterday"
        }

        dateTime.isAfter(now.minusWeeks(1)) -> {
            // If the date is within the last week, return the day of the week (e.g., "Monday")
            dateTime.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        }

        else -> {
            // Otherwise, return the date in the format "dd/MM/yyyy"
            dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        }
    }

}

@Composable
fun DateChip(date: LocalDate) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = formatDate(date),
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(7.dp),
                fontSize = 18.sp
            )
        }
    }
}