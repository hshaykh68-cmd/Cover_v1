package com.cover.app.data.security

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class ShakeDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val SHAKE_THRESHOLD = 12.0f // Acceleration threshold
        private const val SHAKE_COUNT_THRESHOLD = 2 // Number of shakes required
        private const val SHAKE_TIMEOUT_MS = 500 // Time between shakes
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _shakeEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val shakeEvents: SharedFlow<Unit> = _shakeEvents.asSharedFlow()

    private var lastShakeTime = 0L
    private var shakeCount = 0
    private var isListening = false

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Calculate acceleration magnitude
            val acceleration = sqrt(x * x + y * y + z * z)
            val delta = acceleration - SensorManager.GRAVITY_EARTH

            if (delta > SHAKE_THRESHOLD) {
                val currentTime = System.currentTimeMillis()
                
                // Check if within timeout window
                if (currentTime - lastShakeTime < SHAKE_TIMEOUT_MS) {
                    shakeCount++
                } else {
                    shakeCount = 1
                }
                
                lastShakeTime = currentTime

                // Trigger if enough shakes detected
                if (shakeCount >= SHAKE_COUNT_THRESHOLD) {
                    shakeCount = 0
                    _shakeEvents.tryEmit(Unit)
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    /**
     * Start listening for shake gestures
     */
    fun startListening() {
        if (isListening) return
        
        accelerometer?.let {
            sensorManager.registerListener(
                sensorListener,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
            isListening = true
        }
    }

    /**
     * Stop listening for shake gestures
     */
    fun stopListening() {
        if (!isListening) return
        
        sensorManager.unregisterListener(sensorListener)
        isListening = false
        shakeCount = 0
    }

    /**
     * Check if accelerometer is available
     */
    fun isAvailable(): Boolean {
        return accelerometer != null
    }

    /**
     * One-time check for shake during a specific operation
     */
    suspend fun detectShake(durationMs: Long): Boolean {
        startListening()
        
        return try {
            var detected = false
            withTimeoutOrNull(durationMs) {
                shakeEvents.collect { 
                    detected = true
                    return@collect 
                }
            }
            detected
        } finally {
            stopListening()
        }
    }
}
