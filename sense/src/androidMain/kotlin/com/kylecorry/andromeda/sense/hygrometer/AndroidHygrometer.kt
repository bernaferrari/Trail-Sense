package com.kylecorry.andromeda.sense.hygrometer
import com.kylecorry.andromeda.sense.hygrometer.Hygrometer

import com.kylecorry.andromeda.sense.BaseSensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager

public class AndroidHygrometer(context: Context, sensorDelay: Int = SensorManager.SENSOR_DELAY_NORMAL) :
    BaseSensor(context, Sensor.TYPE_RELATIVE_HUMIDITY, sensorDelay),
    Hygrometer {

    override val hasValidReading: Boolean
        get() = gotReading
    private var gotReading = false

    private var _humidity = 0f

    override val humidity: Float
        get() = _humidity

    override fun handleSensorEvent(event: SensorEvent) {
        _humidity = event.values[0]
        gotReading = true
    }

}