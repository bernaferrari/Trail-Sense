package com.kylecorry.andromeda.device

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.ksensor.core.context.KSensorContext
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Duration

internal actual fun platformMagneticHeading(interval: Duration): Flow<Reading<Float>> =
    sensorEvents(Sensor.TYPE_ROTATION_VECTOR, interval).map { reading ->
        val matrix = FloatArray(9)
        val orientation = FloatArray(3)
        SensorManager.getRotationMatrixFromVector(matrix, reading.value)
        SensorManager.getOrientation(matrix, orientation)
        val degrees = orientation[0] * (180f / kotlin.math.PI.toFloat())
        Reading((degrees + 360f) % 360f, reading.time)
    }

internal actual fun platformAmbientTemperature(interval: Duration): Flow<Reading<Float>> =
    sensorEvents(Sensor.TYPE_AMBIENT_TEMPERATURE, interval).map { Reading(it.value[0], it.time) }
internal actual fun platformRelativeHumidity(interval: Duration): Flow<Reading<Float>> =
    sensorEvents(Sensor.TYPE_RELATIVE_HUMIDITY, interval).map { Reading(it.value[0], it.time) }

internal actual fun platformPressure(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Float>> =
    sensorEvents(Sensor.TYPE_PRESSURE, interval).map { Reading(it.value[0], it.time) }
internal actual fun platformAcceleration(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>> =
    sensorReadings(Sensor.TYPE_ACCELEROMETER, interval, 1f)
internal actual fun platformAngularVelocity(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>> =
    sensorReadings(Sensor.TYPE_GYROSCOPE, interval, (180.0 / kotlin.math.PI).toFloat())
internal actual fun platformMagneticField(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>> =
    sensorReadings(Sensor.TYPE_MAGNETIC_FIELD, interval, 1f)

private fun sensorReadings(type: Int, interval: Duration, scale: Float): Flow<Reading<Vector3>> =
    sensorEvents(type, interval).map { reading ->
        val values = reading.value
        Reading(Vector3(values[0] * scale, values[1] * scale, values[2] * scale), reading.time)
    }

private fun sensorEvents(type: Int, interval: Duration): Flow<Reading<FloatArray>> = callbackFlow {
    val manager = KSensorContext.get().getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val sensor = manager.getDefaultSensor(type) ?: run { close(); return@callbackFlow }
    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            trySend(Reading(event.values.copyOf(), Clock.System.now()))
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }
    val delay = interval.inWholeMicroseconds.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
    if (!manager.registerListener(listener, sensor, delay)) { close(); return@callbackFlow }
    awaitClose { manager.unregisterListener(listener) }
}
