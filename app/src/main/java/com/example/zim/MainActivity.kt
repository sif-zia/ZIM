package com.example.zim

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.example.zim.data.room.ZIMDatabase
import com.example.zim.data.room.models.CurrentUser
import com.example.zim.data.room.models.UserWithCurrentUser
import com.example.zim.data.room.models.Users
import com.example.zim.ui.theme.ZIMTheme
import kotlinx.coroutines.launch

import java.time.LocalDate


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val user_dao= ZIMDatabase.getInstance(this).userDao
        val message_dao=ZIMDatabase.getInstance(this).messageDao
        val m : Users=Users(deviceName="Redmi 8",fName="Muaaz",lName="Aamer",DOB=LocalDate.of(2023, 5, 20))
        var currentUser : UserWithCurrentUser?=null

        lifecycleScope.launch{
            currentUser = user_dao.getCurrentUser()
            currentUser?.users?.let { Log.d("ZIM.MainActivity.User", it.fName) }
        }
        setContent {
            ZIMTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "ZIM",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ZIMTheme {
        Greeting("Android")
    }
}