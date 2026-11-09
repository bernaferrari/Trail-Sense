package com.kylecorry.andromeda.sense.barometer
import com.kylecorry.andromeda.sense.barometer.Barometer

import com.kylecorry.andromeda.sense.BaseSensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import kotlin.time.Duration

class AndroidBarometer(
    context: Context,
    sensorDelay: Int = SensorManager.SENSOR_DELAY_NORMAL,
    maxReportLatency: Duration? = null
) : BaseSensor(context, Sensor.TYPE_PRESSURE, sensorDelay, maxReportLatency), Barometer {

    override val pressure: Float
        get() = _pressure

    override val hasValidReading: Boolean
        get() = gotReading

    private var _pressure = 0f
    private var gotReading = false

    override fun handleSensorEvent(event: SensorEvent) {
        gotReading = true
        _pressure = event.values[0]
    }

}