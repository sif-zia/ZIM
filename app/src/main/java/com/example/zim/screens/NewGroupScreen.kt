package com.example.zim.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NewGroupScreen() {
    return Column(modifier = Modifier.fillMaxSize().padding(top = 32.dp)) {
        Text(text = "NewGroupScreen");
    };
}