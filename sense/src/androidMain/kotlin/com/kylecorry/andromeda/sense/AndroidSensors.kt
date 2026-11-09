package com.kylecorry.andromeda.sense

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.time.Clock
import kotlin.time.Instant

/** Android-specific sensor details for hardware not covered by DeviceSensors. */
public data class AndroidSensorInfo(val type: Int, val name: String, val vendor: String)
public data class AndroidSensorReading(val values: FloatArray, val accuracy: Int, val elapsedRealtimeNanos: Long, val time: Instant)

public fun androidSensors(context: Context): List<AndroidSensorInfo> {
    val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return emptyList()
    return manager.getSensorList(Sensor.TYPE_ALL).map { AndroidSensorInfo(it.type, it.name, it.vendor) }
}

/** Collects a raw Android sensor; cancellation unregisters the listener. */
public fun androidSensorReadings(
    context: Context,
    type: Int,
    samplingPeriodMicros: Int = SensorManager.SENSOR_DELAY_GAME,
    maxReportLatencyMicros: Int = 0,
): Flow<AndroidSensorReading> = callbackFlow {
    require(maxReportLatencyMicros >= 0)
    val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        ?: throw UnsupportedOperationException("Android sensors are unavailable")
    val sensor = manager.getDefaultSensor(type) ?: throw UnsupportedOperationException("Sensor $type is unavailable")
    val listener = object : SensorEventListener {
        private var last: AndroidSensorReading? = null
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            last?.copy(accuracy = accuracy)?.also { last = it; trySend(it.copy(values = it.values.copyOf())) }
        }
        override fun onSensorChanged(event: SensorEvent) {
            val reading = AndroidSensorReading(event.values.copyOf(), event.accuracy, event.timestamp, Clock.System.now())
            last = reading
            trySend(reading.copy(values = reading.values.copyOf()))
        }
    }
    check(manager.registerListener(listener, sensor, samplingPeriodMicros, maxReportLatencyMicros)) { "Could not register sensor $type" }
    awaitClose { manager.unregisterListener(listener) }
}
