package com.example.zim.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.zim.R
import com.example.zim.navigation.Navigation
import com.example.zim.ui.theme.Typography
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

fun formatDateTime(dateTime: LocalDateTime?): String {
    if (dateTime == null) return ""

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
    lastMsg: String? = null,
    isConnected: Int? = 0,
    unReadMsgs: Int = 0,
    time: LocalDateTime? = null,
    dpUri: Uri? = null,
    isDM: Boolean = true,
    id: Int,
    navController: NavController,
    lastMsgType: String? = null,
    isSent: Boolean? = false,
    isSelectionModeActive: Boolean,
    isSelected: Boolean,
    onLongClick: () -> Unit,
    onSelectToggle: () -> Unit
) {
    // This state will be used to trigger recomposition
    var refreshTrigger by remember { mutableIntStateOf(0) }

    // Launch a coroutine to update the refresh trigger every minute
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000) // 60000 milliseconds = 1 minute
            refreshTrigger += 1 // Increment to trigger recomposition
        }
    }

    // Every time refreshTrigger changes, formattedTime will be recalculated
    val formattedTime = remember(time, refreshTrigger) {
        formatDateTime(time)
    }

    // Set background color based on selection state
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.background
    }

    Box(modifier = modifier
        .pointerInput(Unit) {
            detectTapGestures(
                onLongPress = { onLongClick() },
                onTap = {
                    if (isSelectionModeActive) {
                        onSelectToggle()
                    } else {
                        if (isDM)
                            navController.navigate(Navigation.UserChat.route + "/${id}")
                        else
                            navController.navigate(Navigation.GroupChat.route + "/${id}")
                    }
                }
            )
        }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .background(backgroundColor) // Apply the background color here
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
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(3F),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = name,
                            style = Typography.bodyMedium
                        )

                        // Unread Messages Badge
                        if (unReadMsgs > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = unReadMsgs.toString(),
                                modifier = Modifier
                                    .clip(shape = RoundedCornerShape(100))
                                    .background(MaterialTheme.colorScheme.tertiary)
                                    .defaultMinSize(minWidth = 15.dp),
                                textAlign = TextAlign.Center,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onTertiary,
                            )
                        }
                    }


                    val msgColor = if (unReadMsgs == 0) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }

                    // Last Message
                    if (lastMsg != null && lastMsgType != null) {
                        val text: String =
                            if (lastMsgType == "Text") {
                                lastMsg
                            } else if (lastMsgType == "Image" && isSent == true) {
                                "Sent an image."
                            } else if (lastMsgType == "Image" && isSent == false) {
                                "Received an image."
                            } else {
                                "Unknown"
                            }
                        Text(
                            modifier = Modifier,
                            text = text,
                            style = if (lastMsgType == "Text") Typography.labelSmall else Typography.labelSmall.copy(
                                fontStyle = FontStyle.Italic
                            ),
                            color = msgColor,
                            maxLines = 1
                        )
                    } else
                        Text(
                            modifier = Modifier,
                            text = "(Start ZIMing...)",
                            style = Typography.labelSmall.copy(
                                fontStyle = FontStyle.Italic
                            ),
                            color = msgColor,
                            maxLines = 1
                        )
                }

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1F),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    if(isConnected != null) {
                        val connectedColor = if (isConnected == 1) {
                            Color.Green
                        } else if (isConnected == 2) {
                            Color.Yellow
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) // Faded primary color
                        }
                        // Connected Status
                        Box(
                            modifier = Modifier
                                .clip(shape = RoundedCornerShape(100))
                                .background(color = connectedColor)
                                .height(10.dp)
                                .width(10.dp)
                        )
                    }
                    // Last Message time - now using the auto-updating formattedTime
                    if (time != null)
                        Text(
                            text = formattedTime,
                            modifier = Modifier.padding(top = 5.dp),
                            style = Typography.labelSmall.copy(fontSize = 10.sp)
                        )
                }
            }
        }
    }
}