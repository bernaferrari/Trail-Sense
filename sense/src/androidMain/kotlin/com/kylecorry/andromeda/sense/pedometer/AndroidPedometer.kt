package com.kylecorry.andromeda.sense.pedometer
import com.kylecorry.andromeda.sense.pedometer.Pedometer

import com.kylecorry.andromeda.sense.BaseSensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import kotlin.time.Duration

public class AndroidPedometer(
    context: Context,
    sensorDelay: Int = SensorManager.SENSOR_DELAY_NORMAL,
    maxReportLatency: Duration? = null
) : Pedometer, BaseSensor(context, Sensor.TYPE_STEP_COUNTER, sensorDelay, maxReportLatency) {

    override val steps: Int
        get() = _steps
    private var _steps = 0

    private var _hasReading = false

    override val hasValidReading: Boolean
        get() = _hasReading

    override fun handleSensorEvent(event: SensorEvent) {
        _steps = event.values[0].toInt()
        _hasReading = true
    }
}