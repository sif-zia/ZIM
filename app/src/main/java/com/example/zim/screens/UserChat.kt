package com.example.zim.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime

data class Sent_Message(
    val status:String,
    val text:String,
)
data class Recieve_Message(
    val time:Int,
    val text:String,
)

val message = listOf(
    Sent_Message("sent","hello",),
    Sent_Message("sent","how are you"),
    Sent_Message("sent","i hope you are good"),

    Recieve_Message(10,"i am good"),
    Recieve_Message(20,"how are you"),
)
@Composable
fun UserChat(userId: Int) {
    return Column(modifier = Modifier.fillMaxSize().padding(top = 32.dp)) {
        Text(text = "Chat of User with id = ${userId}");
    };
}