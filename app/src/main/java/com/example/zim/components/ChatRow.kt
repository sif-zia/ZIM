package com.example.zim.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import coil.compose.rememberAsyncImagePainter
import com.example.zim.R
import com.example.zim.ui.theme.Typography
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

fun formatDateTime(dateTime: LocalDateTime): String {
    val now = LocalDate.now()
    val datePart = dateTime.toLocalDate()

    return when {
        Duration.between(dateTime, LocalDateTime.now()).toMinutes() < 1 -> {
            // If the time difference is less than a minute, return "Just Now"
            "Just Now"
        }

        datePart.isEqual(now) -> {
            // If the date is today, return the time in the format "hh:mm a"
            dateTime.toLocalTime().format(DateTimeFormatter.ofPattern("hh:mm a"))
        }

        datePart.isEqual(now.minusDays(1)) -> {
            // If the date is yesterday, return "Yesterday"
            "Yesterday"
        }

        datePart.isAfter(now.minusWeeks(1)) -> {
            // If the date is within the last week, return the day of the week (e.g., "Monday")
            datePart.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        }

        else -> {
            // Otherwise, return the date in the format "dd/MM/yyyy"
            datePart.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        }
    }
}

@Composable
fun ChatRow(
    modifier: Modifier = Modifier,
    name: String,
    lastMsg: String,
    isConnected: Boolean,
    isRead: Boolean,
    time: LocalDateTime,
    dpUri: Uri? = null
) {
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3F)
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .background(MaterialTheme.colorScheme.background)
                .drawBehind {
                    drawLine(
                        color = borderColor,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx()
                    )

                    drawLine(
                        color = borderColor,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Display Picture
            Image(
                painter = rememberAsyncImagePainter(
                    model = dpUri,
                    placeholder = painterResource(R.drawable.dp_icon),
                    error = painterResource(id = R.drawable.dp_icon)
                ),
                contentDescription = "Display Picture",
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(100))
                    .height(50.dp)
                    .width(50.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Name
                    Text(text = name, style = Typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold))


                    val msgColor = if (isRead) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.primary
                    }

                    // Last Message
                    Text(
                        text = lastMsg,
                        style = Typography.bodyMedium,
                        color = msgColor
                    )
                }

                Column(
                    modifier = Modifier.fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    val connectedColor = if (isConnected) {
                        Color.Green
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) // Faded primary color
                    }
                    // Connected Status
                    Box(
                        modifier = Modifier
                            .clip(shape = RoundedCornerShape(100))
                            .background(color = connectedColor)
                            .height(10.dp)
                            .width(10.dp)
                    )

                    // Last Message time
                    Text(
                        text = formatDateTime(time),
                        modifier = Modifier.padding(top = 5.dp),
                        style = Typography.bodyMedium
                    )
                }
            }
        }
    }
}