package com.example.zim.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zim.R

@Composable
fun LogoRow() {
    return Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier
                .height(64.dp)
                .width(64.dp)
                .offset(x = (-12).dp),
            painter = painterResource(id = R.drawable.zim_logo),
            contentDescription = "ZIM Logo"
        )
        Text(
            modifier = Modifier.offset(x = (-12).dp),
            text = "ZIM",
            fontSize = 32.sp
        )
    }
}