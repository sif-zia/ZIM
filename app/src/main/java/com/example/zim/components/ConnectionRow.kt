package com.example.zim.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material.icons.outlined.RememberMe
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.sharp.PhoneIphone
import androidx.compose.material.icons.sharp.RememberMe
import androidx.compose.material.icons.sharp.Smartphone
import androidx.compose.material.icons.twotone.PhoneIphone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zim.helperclasses.Connection

@Composable
fun ConnectionRow(
    modifier: Modifier = Modifier,
    phoneName: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth(0.8f)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

//        Icon(
//            imageVector = Icons.Outlined.Smartphone,
//            contentDescription = "Phone Icon",
//            modifier = Modifier.size(56.dp)
//        )
        PhoneIcon(color =MaterialTheme.colorScheme.primary.copy(0.66f))
        Spacer(modifier = Modifier.fillMaxHeight().width(12.dp))
        Column(
            modifier = Modifier
                .height(64.dp)
                .weight(3f),
            verticalArrangement = Arrangement.SpaceEvenly,
        )
        {
            Text(text = phoneName, fontSize = 18.sp)
            Text(
                text = description,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                fontSize = 16.sp
            )
        }
        Column(
            modifier = Modifier
                .height(64.dp)
                .weight(3f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "CONNECT",
                modifier = Modifier
                    .clickable { onClick() }
                    .border(
                        2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(25),
                    )
                    .padding(6.dp),
                fontSize = 16.sp
            )
        }
    }
}