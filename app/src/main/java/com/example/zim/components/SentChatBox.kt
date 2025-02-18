package com.example.zim.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zim.helperclasses.ChatBox
import com.example.zim.helperclasses.ChatContent

@Composable
fun SentChatBox(message: ChatContent, isFirst: Boolean = true) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    Box(
        modifier = Modifier
            .padding(start = 5.dp)
            .padding(vertical = if (isFirst) 6.dp else 1.dp),
        contentAlignment = Alignment.TopEnd
    )
     {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(15.dp))
                    .background(MaterialTheme.colorScheme.tertiary)
                    .padding(8.dp)
                    .widthIn(max = screenWidth * 0.8f),
                horizontalAlignment = Alignment.End
            ) {
                Text(message.message, color = MaterialTheme.colorScheme.onTertiary,
                    fontSize = 18.sp)
                Text(
                    formatTime(message.time.toLocalTime()),
                    color = MaterialTheme.colorScheme.onTertiary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.End
                )
            }
        }
        Box(
            modifier = Modifier
                .height(30.dp)
                .width(40.dp)
                .offset(x = (15).dp, y = (-15).dp)
        ) {
            val backgroundColor = MaterialTheme.colorScheme.tertiary
            if(isFirst) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = backgroundColor,
                        startAngle = 180f,
                        sweepAngle = -70f,
                        useCenter = true,
                        style = Fill,
                    )
                }
            }
        }
    }
}