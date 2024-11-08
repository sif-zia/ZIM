package com.example.zim.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.zim.components.DateInput
import com.example.zim.components.TextInput
import com.example.zim.components.LogoRow
import com.example.zim.events.SignUpEvent
import com.example.zim.navigation.Navigation
import com.example.zim.states.SignUpState
import java.time.LocalDate

fun validateName(name: String, newName: String, context: Context): String {
    if (!newName.any { !it.isLetter() } && newName.length <= 30) if (newName.isNotEmpty()) return newName[0].uppercaseChar() + newName.substring(
        1
    );
    else return "";
    else if (name.length >= 30) Toast.makeText(
        context, "Only 30 characters allowed!", Toast.LENGTH_SHORT
    ).show();
    else Toast.makeText(
        context, "Only alphabets are allowed!", Toast.LENGTH_SHORT
    ).show();
    return name;
}

@Composable
fun SignUpScreen(
    navController: NavController,
    state: SignUpState,
    onEvent: (SignUpEvent) -> Unit
) {
    if(state.IsLoggedIn)
    {
        navController.navigate(Navigation.Chats.route)
    }

    val context = LocalContext.current;
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

        TextInput(label = "First Name", text = state.firstName, onTextChange = { newText ->
            onEvent(SignUpEvent.SetFirstName( validateName(state.firstName, newText, context)))
        })
        //same as above state updater
        TextInput(label = "Last Name", text = state.lastName) { newText ->
            onEvent(SignUpEvent.SetLastName(validateName(state.lastName, newText, context)))
        }

        DateInput(label = "Date of Birth", date = state.DOB) { newDate ->
            onEvent(SignUpEvent.SetDOB(newDate))

        }

        Button(
            modifier = Modifier.padding(top = 32.dp),
            onClick = { onEvent(SignUpEvent.SaveUser) },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        ) {
            Text(text = "Save")
        }
    };
}