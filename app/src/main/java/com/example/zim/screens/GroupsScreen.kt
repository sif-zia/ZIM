package com.example.zim.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.zim.components.Search

@Composable
fun GroupsScreen() {
    val horizontalPadding: Dp = 16.dp
    val verticalPadding: Dp = 12.dp

    var query by remember { mutableStateOf("") }

    Column(modifier = Modifier
        .fillMaxSize()) {
        Search(
            modifier = Modifier.padding(
                horizontal = horizontalPadding,
                vertical = verticalPadding
            ),
            query = query,
            onQueryChange = { query = it },
        )
    }


}