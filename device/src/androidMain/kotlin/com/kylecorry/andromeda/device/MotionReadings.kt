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
import kotlin.time.Clock
import kotlin.time.Duration

internal actual fun platformAcceleration(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>> =
    sensorReadings(Sensor.TYPE_ACCELEROMETER, interval, 1f)
internal actual fun platformAngularVelocity(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>> =
    sensorReadings(Sensor.TYPE_GYROSCOPE, interval, (180.0 / kotlin.math.PI).toFloat())
internal actual fun platformMagneticField(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>> =
    sensorReadings(Sensor.TYPE_MAGNETIC_FIELD, interval, 1f)

private fun sensorReadings(type: Int, interval: Duration, scale: Float): Flow<Reading<Vector3>> = callbackFlow {
    val manager = KSensorContext.get().getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val sensor = manager.getDefaultSensor(type) ?: run { close(); return@callbackFlow }
    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.values.size < 3) return
            val value = Vector3(event.values[0] * scale, event.values[1] * scale, event.values[2] * scale)
            trySend(Reading(value, Clock.System.now()))
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }
    val delay = interval.inWholeMicroseconds.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
    if (!manager.registerListener(listener, sensor, delay)) { close(); return@callbackFlow }
    awaitClose { manager.unregisterListener(listener) }
}
