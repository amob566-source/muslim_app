package com.universalazan.app.sensors

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.view.Display
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlin.math.sqrt

/** Which sensors the compass uses on this device. */
enum class CompassSensorMode {
    /** Android's fused rotation vector (accelerometer + magnetometer + gyroscope). Smoothest. */
    RotationVector,

    /** Accelerometer and magnetometer combined directly. Works, but jitters more. */
    AccelerometerMagnetometer,

    /** No magnetometer (or no accelerometer): a compass is not possible on this device. */
    None,
}

/** Sensor-reported reliability of the magnetometer. */
enum class CompassAccuracy { Unknown, Unreliable, Low, Medium, High }

/** One compass reading, relative to MAGNETIC north. */
data class CompassSample(
    val magneticHeading: Double,
    val accuracy: CompassAccuracy,
    /** Strength of the measured magnetic field, or null if not measured yet. */
    val fieldStrengthMicroTesla: Float?,
)

/**
 * Reads the phone's orientation sensors. Sensors are only switched on while [samples] is being
 * collected (the Qibla screen is visible) and are switched off as soon as collection stops.
 */
class CompassSensor(context: Context) {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val displayManager = context.getSystemService(DisplayManager::class.java)

    private val magnetometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val rotationVector: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    /** A rotation vector is only north-referenced if the device really has a magnetometer. */
    val mode: CompassSensorMode = when {
        sensorManager == null || magnetometer == null -> CompassSensorMode.None
        rotationVector != null -> CompassSensorMode.RotationVector
        accelerometer != null -> CompassSensorMode.AccelerometerMagnetometer
        else -> CompassSensorMode.None
    }

    fun samples(): Flow<CompassSample> = callbackFlow {
        val manager = sensorManager
        if (manager == null || mode == CompassSensorMode.None) {
            close()
            return@callbackFlow
        }
        val rotationMatrix = FloatArray(9)
        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        var hasGravity = false
        var hasGeomagnetic = false
        var accuracy = CompassAccuracy.Unknown
        var fieldStrength: Float? = null
        val smoother = AngleSmoother()

        fun displayRotation(): Int =
            displayManager?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: 0

        fun emitHeading() {
            val heading = HeadingMath.heading(rotationMatrix, displayRotation()) ?: return
            trySend(CompassSample(smoother.add(heading), accuracy, fieldStrength))
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        val (x, y, z) = Triple(event.values[0], event.values[1], event.values[2])
                        fieldStrength = sqrt(x * x + y * y + z * z)
                        accuracy = event.accuracy.toCompassAccuracy()
                        if (mode == CompassSensorMode.AccelerometerMagnetometer) {
                            lowPass(event.values, geomagnetic, hasGeomagnetic)
                            hasGeomagnetic = true
                        }
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        lowPass(event.values, gravity, hasGravity)
                        hasGravity = true
                    }
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        emitHeading()
                        return
                    }
                }
                if (mode == CompassSensorMode.AccelerometerMagnetometer && hasGravity && hasGeomagnetic &&
                    SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
                ) {
                    emitHeading()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, newAccuracy: Int) {
                if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD) accuracy = newAccuracy.toCompassAccuracy()
            }
        }

        // The magnetometer is always read too: it reports calibration accuracy and field strength.
        manager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)
        when (mode) {
            CompassSensorMode.RotationVector ->
                manager.registerListener(listener, rotationVector, SensorManager.SENSOR_DELAY_UI)
            CompassSensorMode.AccelerometerMagnetometer ->
                manager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            CompassSensorMode.None -> Unit
        }
        awaitClose { manager.unregisterListener(listener) }
    }.conflate()

    private fun lowPass(input: FloatArray, output: FloatArray, initialised: Boolean) {
        for (i in 0..2) {
            output[i] = if (initialised) output[i] + LOW_PASS_ALPHA * (input[i] - output[i]) else input[i]
        }
    }

    private fun Int.toCompassAccuracy(): CompassAccuracy = when (this) {
        SensorManager.SENSOR_STATUS_UNRELIABLE -> CompassAccuracy.Unreliable
        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> CompassAccuracy.Low
        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> CompassAccuracy.Medium
        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> CompassAccuracy.High
        else -> CompassAccuracy.Unknown
    }

    private companion object {
        const val LOW_PASS_ALPHA = 0.15f
    }
}

/**
 * Magnetic declination (degrees, east positive) at a place and time, from Android's built-in
 * World Magnetic Model. True heading = magnetic heading + declination. Works offline.
 */
fun magneticDeclination(latitude: Double, longitude: Double, timeMillis: Long): Double =
    GeomagneticField(latitude.toFloat(), longitude.toFloat(), 0f, timeMillis).declination.toDouble()
