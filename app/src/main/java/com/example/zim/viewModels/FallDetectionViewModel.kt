package com.example.zim.viewModels

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.activity.ComponentActivity.SENSOR_SERVICE
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zim.api.ActiveUserManager
import com.example.zim.api.AlertData
import com.example.zim.api.ClientRepository
import com.example.zim.data.room.Dao.AlertDao
import com.example.zim.data.room.Dao.UserDao
import com.example.zim.data.room.models.Alerts
import com.example.zim.helperclasses.AlertType
import com.example.zim.states.FallDetectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt


@HiltViewModel
class FallDetectionViewModel @Inject constructor(
    val application: Application,
    val userDao: UserDao,
    val alertDao: AlertDao,
    val clientRepository: ClientRepository,
    val activeUserManager: ActiveUserManager
) : ViewModel(), SensorEventListener {


    private val _state = MutableStateFlow(FallDetectionState())

    val state: StateFlow<FallDetectionState> = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        FallDetectionState()
    )

    private val TAG = "FallDetectionViewModel"

    private val meanArray = floatArrayOf(
        1.6418100929257502f, 2.3303889437215637f, 1.7351686581527097f, 13.772294131027596f,
        3.4684344498347697f, 7.9804239066585705f, 3.7797699809394287f, -0.04116200113241134f,
        0.11533739479469848f, 0.33092778310414195f, 1.5691070700922725f, 1.401081694244498f,
        2.1160457712040177f, 0.6910203050534983f, 0.6691304873199629f, 0.4410601416658173f,
        1.394856546949643f, 0.7000793124819371f, 0.6866497132689007f, 0.44741325068853394f,
        -0.17941408160642625f, -0.010643754971751719f, -0.09020619194043708f, 1.7887618232469864f,
        2.65081253242727f, 2.1718262630631986f
    )
    private val stdArray = floatArrayOf(

        1.6872932284731876f, 2.598433417936135f, 1.6873538275304052f, 1.796950977081903f,
        2.331569517026537f, 3.489721469206532f, 2.6472993056228797f, 0.858984890019558f,
        0.8413814331048338f, 1.0141025414050826f, 3.908550681184858f, 4.774124117892085f,
        5.350656253553282f, 0.696575879979791f, 0.7019355572988154f, 0.4819861926660918f,
        1.4243852968758546f, 0.6981842495329723f, 0.7131424334371129f, 0.48298061740698567f,
        0.9940775503671047f, 1.0069765786917517f, 1.0634343246311944f, 6.986622691612793f,
        10.26473555499198f, 11.401863715217308f
    )

    private var acc_window_x = emptyList<Float>()
    private var acc_window_y = emptyList<Float>()
    private var acc_window_z = emptyList<Float>()
    private var acc_timestamps = emptyList<Long>()

    private var gyro_window_x = emptyList<Float>()
    private var gyro_window_y = emptyList<Float>()
    private var gyro_window_z = emptyList<Float>()
    private var gyro_timestamps = emptyList<Long>()

    private var isAccWindowFull = false
    private var isGyroWindowFull = false

    private var prediction = -1
    private var predictionCooldown = false

    private var print: Int = 0

    private var predictionQue: MutableList<Int> = mutableListOf()
    private var threshold: Int = 4
    val duration = 60

    // Initialize sensorManager but don't register listeners yet
    private var sensorManager: SensorManager = application.getSystemService(SENSOR_SERVICE) as SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var sensorsRegistered = false

    init {
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        viewModelScope.launch {
            _state.collect { fallState ->
                // Handle fall detection state changes
                if (fallState.fallAlertStatus == "detected") {
                    _state.update { it.copy(fallAlertStatus = "countDown", countDown = duration) }

                    while (true) {
                        if (_state.value.fallAlertStatus == "safe") break
                        if (_state.value.countDown == 0) {
                            sendFallAlert()
                            _state.update { it.copy(fallAlertStatus = "safe", countDown = -1) }
                            break
                        }
                        delay(1000)
                        _state.update { it.copy(countDown = it.countDown - 1) }
                    }
                }
                Log.d("FallPrediction", predictionQue.toString())

                // Handle sensor registration/unregistration based on fall detection being enabled
                if (fallState.isFallDetectionEnabled && !sensorsRegistered) {
                    registerSensorListeners()
                } else if (!fallState.isFallDetectionEnabled && sensorsRegistered) {
                    unregisterSensorListeners()
                }
            }
        }
    }

    // Register sensor listeners
    private fun registerSensorListeners() {
        if (sensorsRegistered) return

        accelerometer?.also { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_FASTEST,
                SensorManager.SENSOR_DELAY_UI
            )
        }

        gyroscope?.also { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_FASTEST,
                SensorManager.SENSOR_DELAY_UI
            )
        }

        sensorsRegistered = true
        Log.d(TAG, "Sensor listeners registered")
    }

    // Unregister sensor listeners
    private fun unregisterSensorListeners() {
        if (!sensorsRegistered) return

        sensorManager.unregisterListener(this)
        sensorsRegistered = false

        // Clear data buffers when sensors are disabled
        clearSensorBuffers()

        Log.d(TAG, "Sensor listeners unregistered")
    }

    // Clear all sensor data buffers
    private fun clearSensorBuffers() {
        acc_window_x = emptyList()
        acc_window_y = emptyList()
        acc_window_z = emptyList()
        acc_timestamps = emptyList()

        gyro_window_x = emptyList()
        gyro_window_y = emptyList()
        gyro_window_z = emptyList()
        gyro_timestamps = emptyList()

        isAccWindowFull = false
        isGyroWindowFull = false
    }

    fun onToggleFallDetection() {
        viewModelScope.launch {
            _state.update { it.copy(isFallDetectionEnabled = !it.isFallDetectionEnabled) }
        }
    }

    private fun getPrediction(
        acc_window_x: List<Float>, acc_window_y: List<Float>, acc_window_z: List<Float>,
        gyro_window_x: List<Float>, gyro_window_y: List<Float>, gyro_window_z: List<Float>
    ) {
        predictionCooldown = true
        CoroutineScope(Dispatchers.Main).launch {
            delay(1000)
            predictionCooldown = false
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val rawInput =
                    (summarizeWindow(acc_window_x, acc_window_y, acc_window_z) + summarizeWindow(
                        gyro_window_x,
                        gyro_window_y,
                        gyro_window_z
                    )).toMutableList()

                // Standardize the input data
                val standardizedInput = rawInput.mapIndexed { index, value ->
                    (value - meanArray[index]) / stdArray[index]
                }.toFloatArray()
                if (print <= 2) {
                    Log.d("features", "Standardize: " + standardizedInput.joinToString(", "))
                    print = print + 1;
                }
                // Load model
                val modelPath = application.filesDir.absolutePath + "/lightgbm_model.onnx"
                application.assets.open("lightgbm_model.onnx").use { inputStream ->
                    val modelFile = application.filesDir.resolve("lightgbm_model.onnx")
                    modelFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                val env = OrtEnvironment.getEnvironment()
                val session = env.createSession(modelPath, OrtSession.SessionOptions())

                // Run inference
                val inputName = session.inputNames.iterator().next()
                val inputTensor = OnnxTensor.createTensor(env, arrayOf(standardizedInput))
                val results = session.run(mapOf(inputName to inputTensor))

                // Get prediction
                val output = results[0].value as LongArray
                val modelPrediction = output.firstOrNull() ?: -1

                // Update prediction
                CoroutineScope(Dispatchers.Main).launch {
                    prediction = modelPrediction.toInt()
                    _state.update { it.copy(prediction = prediction) }
                    predictionQue.add(prediction)
                    if (predictionQue.size >= threshold) {
                        predictionQue.removeAt(0)
                    }
                    if (isFall(predictionQue)) {
                        _state.update { it.copy(fallAlertStatus = "detected") }
                    }
                }

                // Clean up resources
                results.close()
                session.close()
                env.close()
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    Log.d(TAG, "Error: ${e.message}")
                }
            }
        }
    }

    fun isFall(list: List<Int>): Boolean {
        if (list.size == 0) return false
        var fall: Boolean = true
        list.forEach { prediction ->
            if (prediction != 3 && prediction != 4 && prediction != 8) {
                fall = false
            }
        }
        return fall
    }

    fun onCancel() {
        viewModelScope.launch {
            _state.update { it.copy(fallAlertStatus = "safe", countDown = -1) }
        }
    }

    private suspend fun sendFallAlert() {
        val currentUser = userDao.getCurrentUser()?.users

        val alertId = alertDao.insertAlert(
            Alerts(
                description = "Automatic Fall Detection",
                type = AlertType.FALL.toName(),
                sentTime = LocalDateTime.now(),
                isSent = true,
            )
        )

        if (currentUser != null) {
            activeUserManager.activeUsers.value.values.forEach { ip ->
                clientRepository.sendAlert(
                    AlertData(
                        alertType = AlertType.FALL.toName(),
                        alertDescription = "Automatic Fall Detection",
                        alertTime = LocalDateTime.now(),
                        alertSenderFName = currentUser.fName,
                        alertSenderLName = currentUser.lName ?: "",
                        alertSenderPuKey = currentUser.UUID,
                        alertHops = 0,
                        alertId = alertId.toInt()
                    ),
                    neighborIp = ip
                )
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        // Only process sensor data if fall detection is enabled
        if (!_state.value.isFallDetectionEnabled) return

        when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                _state.update { it.copy(accReadings = event.values.clone()) }

                if (acc_window_x.isEmpty() || !isAccWindowFull) {
                    acc_window_x += event.values[0]
                    acc_window_y += event.values[1]
                    acc_window_z += event.values[2]
                    acc_timestamps += event.timestamp
                }
                if (acc_window_x.isNotEmpty() && event.timestamp - acc_timestamps[0] >= 4000000000) {
                    isAccWindowFull = true
                }
            }

            Sensor.TYPE_GYROSCOPE -> {
                _state.update { it.copy(gyroReadings = event.values.clone()) }
                if (gyro_window_x.isEmpty() || !isGyroWindowFull) {
                    gyro_window_x += event.values[0]
                    gyro_window_y += event.values[1]
                    gyro_window_z += event.values[2]
                    gyro_timestamps += event.timestamp
                }
                if (gyro_window_x.isNotEmpty() && event.timestamp - gyro_timestamps[0] >= 4000000000) {
                    isGyroWindowFull = true
                }
            }
        }

        if (isAccWindowFull && isGyroWindowFull) {
            if (!predictionCooldown) {
                getPrediction(
                    acc_window_x.toList(), acc_window_y.toList(), acc_window_z.toList(),
                    gyro_window_x.toList(), gyro_window_y.toList(), gyro_window_z.toList()
                )
            }
            acc_window_x = acc_window_x.drop(1)
            acc_window_y = acc_window_y.drop(1)
            acc_window_z = acc_window_z.drop(1)
            acc_timestamps = acc_timestamps.drop(1)

            gyro_window_x = gyro_window_x.drop(1)
            gyro_window_y = gyro_window_y.drop(1)
            gyro_window_z = gyro_window_z.drop(1)
            gyro_timestamps = gyro_timestamps.drop(1)

            isAccWindowFull = false
            isGyroWindowFull = false
        }
    }

    private fun summarizeWindow(
        windowX: List<Float>,
        windowY: List<Float>,
        windowZ: List<Float>
    ): List<Float> {
        val features = mutableListOf<Float>()
        if (windowX.isNotEmpty()) {
            val stdX = windowX.std()
            val stdY = windowY.std()
            val stdZ = windowZ.std()
            val rmsX = windowX.rms()
            val rmsY = windowY.rms()
            val rmsZ = windowZ.rms()
            val skewX = windowX.skewness()
            val skewY = windowY.skewness()
            val skewZ = windowZ.skewness()
            val kurtX = windowX.kurtosis()
            val kurtY = windowY.kurtosis()
            val kurtZ = windowZ.kurtosis()
            val sma =
                (windowX.sumOf { abs(it.toDouble()) } + windowY.sumOf { abs(it.toDouble()) } + windowZ.sumOf {
                    abs(
                        it.toDouble()
                    )
                }) / windowX.size


            features.add(stdX)
            features.add(stdY)
            features.add(stdZ)
            features.add(sma.toFloat())
            features.add(rmsX)
            features.add(rmsY)
            features.add(rmsZ)
            features.add(skewX.toFloat())
            features.add(skewY.toFloat())
            features.add(skewZ.toFloat())
            features.add(kurtX.toFloat())
            features.add(kurtY.toFloat())
            features.add(kurtZ.toFloat())

        }
        if (print < 2) {
            Log.d("features", "features$features")
            print += 1
        }
        return features
    }

    // Extension function to calculate standard deviation for Float
    private fun List<Float>.std(): Float {
        val mean = this.average().toFloat()
        return sqrt(this.sumOf { (it - mean).toDouble().pow(2.0) } / this.size).toFloat()
    }

    // Extension function to calculate RMS for Float
    private fun List<Float>.rms(): Float {
        return sqrt(this.map { it.toDouble().pow(2.0) }.average()).toFloat()
    }

    fun List<Float>.mean(): Double {
        if (this.isEmpty()) throw IllegalArgumentException("Empty list")
        return this.sum().toDouble() / this.size
    }

    fun List<Float>.popStd(): Double {
        val n = this.size
        if (n == 0) throw IllegalArgumentException("Empty list")
        val m = this.mean()
        val variance = this.fold(0.0) { acc, x -> acc + (x - m).pow(2) } / n
        return sqrt(variance)
    }

    fun List<Float>.sampleStd(): Double {
        val n = this.size
        if (n < 2) throw IllegalArgumentException("At least 2 data points required")
        val m = this.mean()
        val variance = this.fold(0.0) { acc, x -> acc + (x - m).pow(2) } / (n - 1)
        return sqrt(variance)
    }

    fun List<Float>.skewness(bias: Boolean = true): Double {
        val n = this.size
        if (n < 2) throw IllegalArgumentException("At least 2 data points required for skewness")
        val m = this.mean()

        return if (bias) {
            // Use population standard deviation (ddof=0)
            val s = this.popStd()
            if (s == 0.0) 0.0
            else {
                val m3 = this.fold(0.0) { acc, x -> acc + (x - m).pow(3) } / n
                m3 / s.pow(3)
            }
        } else {
            if (n < 3) throw IllegalArgumentException("At least 3 data points required for unbiased skewness")
            // Use sample standard deviation (ddof=1)
            val s = this.sampleStd()
            if (s == 0.0) 0.0
            else {
                val m3 = this.fold(0.0) { acc, x -> acc + (x - m).pow(3) } / n
                val skewPop = m3 / s.pow(3)
                // Apply the unbiased adjustment factor.
                sqrt(n.toDouble() * (n - 1)) / (n - 2) * skewPop
            }
        }
    }

    fun List<Float>.kurtosis(bias: Boolean = true, fisher: Boolean = true): Double {
        val n = this.size
        if (n < 2) throw IllegalArgumentException("At least 2 data points required for kurtosis")
        val m = this.mean()

        return if (bias) {
            // Use population standard deviation (ddof=0)
            val s = this.popStd()
            if (s == 0.0) 0.0
            else {
                val m4 = this.fold(0.0) { acc, x -> acc + (x - m).pow(4) } / n
                val kurt = m4 / s.pow(4)
                if (fisher) kurt - 3 else kurt
            }
        } else {
            if (n < 4) throw IllegalArgumentException("At least 4 data points required for unbiased kurtosis")
            // Use sample standard deviation (ddof=1)
            val s = this.sampleStd()
            if (s == 0.0) 0.0
            else {
                val m4 = this.fold(0.0) { acc, x -> acc + (x - m).pow(4) } / n
                // Compute excess kurtosis (using sample std but 1/n divisor for the moment)
                val kurtPop = m4 / s.pow(4) - 3
                // Apply the unbiased correction factor (per Joanes and Gill, 1998)
                val G2 = ((n - 1).toDouble() / ((n - 2) * (n - 3))) * ((n + 1) * kurtPop + 6)
                if (fisher) G2 else G2 + 3
            }
        }
    }


    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // No implementation needed
    }

    // Clean up when viewModel is destroyed
    override fun onCleared() {
        super.onCleared()
        // Make sure to unregister listeners to prevent memory leaks
        unregisterSensorListeners()
    }
}