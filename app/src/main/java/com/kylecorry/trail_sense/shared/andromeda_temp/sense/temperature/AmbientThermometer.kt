package com.kylecorry.trail_sense.shared.andromeda_temp.sense.temperature

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import com.kylecorry.andromeda.core.sensors.Thermometer
import com.kylecorry.trail_sense.shared.andromeda_temp.sense.BaseSensor

class AmbientThermometer(
    context: Context,
    sensorDelay: Int = SensorManager.SENSOR_DELAY_NORMAL
) :
    BaseSensor(context, Sensor.TYPE_AMBIENT_TEMPERATURE, sensorDelay),
    Thermometer {

    override val hasValidReading: Boolean
        get() = gotReading
    private var gotReading = false

    private var _temp = 0f

    override val temperature: Float
        get() = _temp

    override fun handleSensorEvent(event: SensorEvent) {
        _temp = event.values[0]
        gotReading = true
    }

}