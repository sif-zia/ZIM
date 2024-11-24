package com.example.zim.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PhoneIcon(
    height: Dp = 56.dp,
    width: Dp = 32.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    thickness: Dp = 2.dp,
    roundness: Int = 25
) {
    Column(
        modifier = Modifier
            .height(height)
            .width(width)
            .border(thickness, color, RoundedCornerShape(roundness)),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    )
    {
        Spacer(modifier = Modifier.fillMaxHeight(0.05f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center

        ) {

            Box(
                modifier = Modifier
                    .fillMaxHeight(0.5f)
                    .fillMaxWidth(0.3f)
                    .border(thickness, color, RoundedCornerShape(roundness))
            )
            Spacer(modifier = Modifier.fillMaxWidth(0.1f))
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.5f)
                    .fillMaxWidth(0.15f)
                    .border(thickness, color, RoundedCornerShape(100))
            )

        }
        Spacer(modifier = Modifier.fillMaxHeight(0.04f))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .fillMaxHeight(0.85f)
                .border(
                    thickness,
                    color,
                    RoundedCornerShape(roundness)
                )
        )
    }
}