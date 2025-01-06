package com.example.zim.viewModels

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import androidx.activity.ComponentActivity.SENSOR_SERVICE
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zim.data.room.Dao.UserDao
import com.example.zim.states.ConnectionsState
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
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt


@HiltViewModel
class FallDetectionViewModel @Inject constructor(
    val application: Application
) : ViewModel() , SensorEventListener{


    private val _state = MutableStateFlow(FallDetectionState())

    val state: StateFlow<FallDetectionState> = _state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        FallDetectionState()
    )
    private val meanArray = floatArrayOf(
        1.6411437639500814f, 2.329442950958595f, 1.7344645740077211f,
        13.772411123292091f, -0.3247140052799025f, -0.012802071346375144f,
        -0.3891220469775943f, 0.6908875484825284f, 0.6690009851283987f,
        0.44099485173912994f, 1.3946268419452752f, -0.5482552629797603f,
        -0.9213768361199486f, -0.5096713599133554f, 17.772831145380085f,
        10.0041227774441f, 3.8901220441320126f, 273.525340063708f,
        -9.307520476545048e-05f, -0.002259189061125025f, -0.0442445677925946f
    )
    private val stdArray = floatArrayOf(
        1.6872744013085512f, 2.5983295546287177f, 1.687372713177547f,
        1.7988122939458435f, 1.5776504702798262f, 0.22602281513976472f,
        1.7600016521963726f, 0.6965695086590631f, 0.7019387046472989f,
        0.4819937181719363f, 1.4243995830737786f, 1.6263181519574965f,
        2.543004523214312f, 1.564190890832942f, 35.19033620273425f,
        14.905911360405915f, 4.982734068909263f, 113.6407909718168f,
        0.00599611778048982f, 0.04455898831710484f, 0.2846988527358873f
    )

    private var acc_window_x = emptyList<Float>()
    private var acc_window_y = emptyList<Float>()
    private var acc_window_z = emptyList<Float>()
    private var acc_timestamps = emptyList<Long>()

    private var gyro_window_x = emptyList<Float>()
    private var gyro_window_y = emptyList<Float>()
    private var gyro_window_z = emptyList<Float>()
    private var gyro_timestamps = emptyList<Long>()

    private var ori_window_x = emptyList<Float>()
    private var ori_window_y = emptyList<Float>()
    private var ori_window_z = emptyList<Float>()
    private var ori_timestamps = emptyList<Long>()

    private var isAccWindowFull = false
    private var isGyroWindowFull = false
    private var isOriWindowFull = false

    private var prediction = -1
    private var predictionCooldown = false

    private lateinit var sensorManager: SensorManager


    private fun setUpSensorStuff() {
        sensorManager = application.getSystemService(SENSOR_SERVICE) as SensorManager

        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_FASTEST,
                SensorManager.SENSOR_DELAY_UI
            )
        }

        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.also { gyroscope ->
            sensorManager.registerListener(
                this,
                gyroscope,
                SensorManager.SENSOR_DELAY_FASTEST,
                SensorManager.SENSOR_DELAY_UI
            )
        }

        sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)?.also { orientation ->
            sensorManager.registerListener(
                this,
                orientation,
                SensorManager.SENSOR_DELAY_FASTEST,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    private fun getPrediction(
        acc_window_x: List<Float>, acc_window_y: List<Float>, acc_window_z: List<Float>,
        gyro_window_x: List<Float>, gyro_window_y: List<Float>, gyro_window_z: List<Float>,
        ori_window_x: List<Float>, ori_window_y: List<Float>, ori_window_z: List<Float>
    ) {
        predictionCooldown = true
        CoroutineScope(Dispatchers.Main).launch {
            delay(1000)
            predictionCooldown = false
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val rawInput = (summarizeWindow(acc_window_x, acc_window_y, acc_window_z) + summarizeWindow(gyro_window_x, gyro_window_y, gyro_window_z) + summarizeWindow(ori_window_x, ori_window_y, ori_window_z)).toMutableList()

                // Standardize the input data
                val standardizedInput = rawInput.mapIndexed { index, value ->
                    (value - meanArray[index]) / stdArray[index]
                }.toFloatArray()

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
                    prediction=modelPrediction.toInt()
                    _state.update { it.copy(prediction = prediction)}
                }

                // Clean up resources
                results.close()
                session.close()
                env.close()
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(application, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    override fun onSensorChanged(event: SensorEvent?) {
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

            Sensor.TYPE_ORIENTATION -> {
                _state.update { it.copy(oriReadings = event.values.clone()) }
                if (ori_window_x.isEmpty() || !isOriWindowFull) {
                    ori_window_x += event.values[0]
                    ori_window_y += event.values[1]
                    ori_window_z += event.values[2]
                    ori_timestamps += event.timestamp
                }
                if (ori_window_x.isNotEmpty() && event.timestamp - ori_timestamps[0] >= 4000000000) {
                    isOriWindowFull = true
                }
            }
        }

        if (isAccWindowFull && isGyroWindowFull && isOriWindowFull) {
            if (!predictionCooldown) {
                getPrediction(
                    acc_window_x.toList(), acc_window_y.toList(), acc_window_z.toList(),
                    gyro_window_x.toList(), gyro_window_y.toList(), gyro_window_z.toList(),
                    ori_window_x.toList(), ori_window_y.toList(), ori_window_z.toList()
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

            ori_window_x = ori_window_x.drop(1)
            ori_window_y = ori_window_y.drop(1)
            ori_window_z = ori_window_z.drop(1)
            ori_timestamps = ori_timestamps.drop(1)

            isAccWindowFull = false
            isGyroWindowFull = false
            isOriWindowFull = false
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
            val sma =
                (windowX.sumOf { abs(it.toDouble()) } + windowY.sumOf { abs(it.toDouble()) } + windowZ.sumOf {
                    abs(
                        it.toDouble()
                    )
                }) / windowX.size

            val samplingRate = windowX.size / 4.0f

            // Compute FFT for each axis (X, Y, Z)
            val (dfX, _) = computeFFT(windowX.toFloatArray(), samplingRate)
            val (dfY, _) = computeFFT(windowY.toFloatArray(), samplingRate)
            val (dfZ, _) = computeFFT(windowZ.toFloatArray(), samplingRate)

            features.add(stdX)
            features.add(stdY)
            features.add(stdZ)
            features.add(sma.toFloat())
            features.add(dfX.maxOrNull() ?: 0f) // max FFT value for X axis
            features.add(dfY.maxOrNull() ?: 0f) // max FFT value for Y axis
            features.add(dfZ.maxOrNull() ?: 0f) // max FFT value for Z axis
        }
        return features
    }

    // Extension function to calculate standard deviation for Float
    private fun List<Float>.std(): Float {
        val mean = this.average().toFloat()
        return sqrt(this.sumOf { (it - mean).toDouble().pow(2.0) } / this.size).toFloat()
    }

    // Function to compute FFT and return frequencies and magnitudes
    private fun computeFFT(signal: FloatArray, samplingRate: Float): Pair<FloatArray, FloatArray> {
        // Find the next power of 2 for the signal length
        val n = signal.size
        val nextPowerOf2 = Integer.highestOneBit(n - 1) shl 1

        // Pad the signal array with zeros if necessary
        val paddedSignal = signal.copyOf(nextPowerOf2)

        val transformer = FastFourierTransformer(DftNormalization.STANDARD)
        val complexData = transformer.transform(
            paddedSignal.map { it.toDouble() }.toDoubleArray(),
            TransformType.FORWARD
        )

        // Getting the magnitudes and frequencies
        val magnitudes = FloatArray(complexData.size)
        val freqs = FloatArray(complexData.size)

        for (i in complexData.indices) {
            magnitudes[i] =
                sqrt(complexData[i].real * complexData[i].real + complexData[i].imaginary * complexData[i].imaginary).toFloat()
            freqs[i] = (i * samplingRate / paddedSignal.size)
        }

        return freqs to magnitudes
    }


    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

    }

    init {
        setUpSensorStuff()
    }
}