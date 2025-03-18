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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun formatDateForChip(date: LocalDate): String {
    val now = LocalDate.now()

    return when {
        date.isEqual(now) -> {
            "Today"
        }
        date.isEqual(now.minusDays(1)) -> {
            "Yesterday"
        }
        date.isAfter(now.minusWeeks(1)) -> {
            // If the date is within the last week, return the day of the week
            date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        }
        else -> {
            // Otherwise, return the date in the format "dd/MM/yyyy"
            date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
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
                .background(color = Color.Gray.copy(alpha=0.66f))
        ) {
            Text(
                text = formatDateForChip(date),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(6.dp),
                fontSize = 12.sp
            )
        }
    }
}