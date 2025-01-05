package com.example.zim.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.zim.states.FallDetectionState
import com.example.zim.viewModels.FallDetectionViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay

val predictionMap = mapOf(
    -1 to "Unknown",
    0 to "BSC: Backward Sitting Chair Falling",
    1 to "CSI: Car Step In",
    2 to "CSO: Car Step Out",
    3 to "FKL: Forward Knees Lying Falling",
    4 to "FOL: Forward Lying Falling",
    5 to "JOG: Jogging",
    6 to "JUM: Jumping",
    7 to "SCH: Sit Chair",
    8 to "SDL: Sidewards Lying Falling",
    9 to "STD: Standing",
    10 to "STN: Stairs Down",
    11 to "STU: Stairs Up",
    12 to "WAL: Walking"
)
@Composable
fun FallDetectionScreen(navController: NavController, fallDetectionViewModel: FallDetectionViewModel = hiltViewModel())
{
    val fallDetectionState by fallDetectionViewModel.state.collectAsState()

    // Extract the latest readings and prediction from the state
    val latestAccReadings = fallDetectionState.accReadings
    val latestGyroReadings = fallDetectionState.gyroReadings
    val latestOriReadings = fallDetectionState.oriReadings
    val latestPrediction = fallDetectionState.prediction

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Display Prediction
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Prediction")
            Text(predictionMap[latestPrediction] ?: "Unknown")
        }

        // Display Accelerometer Readings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Accelerometer")
            Text(
                "${"%.2f".format(latestAccReadings[0])}, ${
                    "%.2f".format(latestAccReadings[1])
                }, ${"%.2f".format(latestAccReadings[2])}",
                maxLines = 1
            )
        }

        // Display Gyroscope Readings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Gyroscope")
            Text(
                "${"%.2f".format(latestGyroReadings[0])}, ${
                    "%.2f".format(latestGyroReadings[1])
                }, ${"%.2f".format(latestGyroReadings[2])}",
                maxLines = 1
            )
        }

        // Display Orientation Readings
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Orientation")
            Text(
                "${"%.2f".format(latestOriReadings[0])}, ${
                    "%.2f".format(latestOriReadings[1])
                }, ${"%.2f".format(latestOriReadings[2])}",
                maxLines = 1
            )
        }
    }
}