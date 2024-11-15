package com.example.zim.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.zim.navigation.Navigation

@Composable
fun ConnectionsScreen(navController: NavController) {

    val verticalPadding = 12.dp;

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    )
    {
        Column(
            modifier=Modifier.weight(1F),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Sensors,
                contentDescription = "Connection Logo",
                modifier = Modifier.size(128.dp),
            )
            Text(text = "Searching For Nearby Devices...")
            Text(
                text = "Open the app in the other device and make sure its Wifi is turned on!",
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5F),
                textAlign = TextAlign.Center,
            )
        }
        HorizontalDivider()
        Column(
            modifier=Modifier.weight(1F),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ){


        }


    }
}