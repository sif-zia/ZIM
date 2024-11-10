package com.example.zim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.zim.data.room.ZIMDatabase
import com.example.zim.data.room.schema.Schema
import com.example.zim.navigation.NavGraph
import com.example.zim.ui.theme.ZIMTheme
import com.example.zim.viewModels.ChatsViewModel
import com.example.zim.viewModels.SignUpViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val signUpViewModel: SignUpViewModel by viewModels()
    private val chatsViewModel: ChatsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ZIMTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background

                ) {
                    val signUpState by signUpViewModel.state.collectAsState()
                    val chatsState by chatsViewModel.state.collectAsState()
                    NavGraph(
                        signUpState=signUpState,
                        onSignUpEvent = signUpViewModel::onEvent,
                        chatsState = chatsState,
                        onChatsEvent = chatsViewModel::onEvent
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