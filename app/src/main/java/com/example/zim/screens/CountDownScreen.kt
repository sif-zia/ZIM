package com.example.zim.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.zim.viewModels.FallDetectionViewModel
import java.time.Duration
import java.time.LocalDateTime

@Composable
fun CountDownScreen(fallDetectionViewModel: FallDetectionViewModel = hiltViewModel()) {
    val state by fallDetectionViewModel.state.collectAsState()
    val backgroundColor = MaterialTheme.colorScheme.secondary
    if (state.countDown >= 0) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(0.33f)
                ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center){
                //Outer Circle
                Box(
                    modifier = Modifier
                        .height(200.dp)
                        .width(200.dp)
                        .clip(RoundedCornerShape(100))
                        .background(color = MaterialTheme.colorScheme.primary)
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        // Draw a sector (a filled arc that looks like a pie slice)
                        drawArc(
                            color = backgroundColor, // Color of the sector
                            startAngle = -90f, // Start angle of the arc (in degrees)
                            sweepAngle = getCountDownAngle(
                                state.countDown,
                                fallDetectionViewModel.duration
                            ), // Sweep angle (extent of the arc, in degrees)
                            useCenter = true, // Connect the arc to the center to form a sector
                            style = Fill // Fill the sector with color
                        )
                    }
                }
                //Inner Circle
                Box(
                    modifier = Modifier
                        .height(150.dp)
                        .width(150.dp)
                        .clip(RoundedCornerShape(100))
                        .background(color = MaterialTheme.colorScheme.primary)
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        // Draw a sector (a filled arc that looks like a pie slice)
                        drawArc(
                            color = backgroundColor, // Color of the sector
                            startAngle = 0f, // Start angle of the arc (in degrees)
                            sweepAngle = 360f,
                            useCenter = true, // Connect the arc to the center to form a sector
                            style = Fill // Fill the sector with color
                        )
                    }
                }
                //Button
                Button(onClick = {fallDetectionViewModel.onCancel()}) {
                    Text(text = "Tap To Cancel")
                }
            }
        }
    }
}
fun getCountDownAngle(countDown:Int,duration:Int): Float {
    val fraction = countDown.toFloat() / duration.toFloat()
    val newAngle = fraction * -360f
    return newAngle
}