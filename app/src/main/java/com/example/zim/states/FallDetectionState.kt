package com.example.zim.states

data class FallDetectionState(
    val prediction: Int = -1,
    val accReadings: FloatArray = FloatArray(3),
    val gyroReadings: FloatArray = FloatArray(3),
    val fallAlertStatus: String = "safe",
    val countDown: Int = -1,
    val isFallDetectionEnabled: Boolean = false
)
