package com.example.zim.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.zim.components.DateInput
import com.example.zim.components.TextInput
import com.example.zim.components.LogoRow
import com.example.zim.navigation.Navigation
import java.time.LocalDate

@Composable
fun SignUpScreen(navController: NavController) {

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var DOB by remember { mutableStateOf<LocalDate?>(null) }

    return Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .padding(top = 32.dp)
    ) {
        LogoRow();

        Text(
            modifier = Modifier.padding(vertical = 32.dp),
            text = "Tell Us About Yourself!",
            fontSize = 28.sp,
        )

        TextInput(label = "First Name", text = firstName) { newText ->
            firstName = newText
        }

        TextInput(label = "Last Name", text = lastName) { newText ->
            lastName = newText
        }

        DateInput(label = "Date of Birth", date = DOB) { newDate ->
            DOB = newDate
        }

        Button(
            modifier = Modifier.padding(top = 32.dp),
            onClick = { navController.navigate(Navigation.Chats.route)},
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
            ) {
            Text(text = "Save")
        }
    };
}