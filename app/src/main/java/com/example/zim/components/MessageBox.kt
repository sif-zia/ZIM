package com.example.zim.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MessageBox(
    text: String,
    time: Int,
    type: String
) {
    var boxtype: Color
    if (type == "recieve") {
        boxtype = Color.White  // Use Color.White instead of "white"
    } else {
        boxtype = Color(0xFF800080)  // Use the hexadecimal value for purple
    }
    Box(
        modifier = Modifier
            .background(color = boxtype)

    ) {
        Row {
            Text(text = text)
        }

    }
}