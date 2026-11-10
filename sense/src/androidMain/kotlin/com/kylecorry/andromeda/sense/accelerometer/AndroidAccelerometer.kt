package com.kylecorry.andromeda.sense.accelerometer
import com.kylecorry.andromeda.sense.accelerometer.Accelerometer

import com.kylecorry.andromeda.sense.BaseSensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import com.kylecorry.sol.math.Vector3

public class AndroidAccelerometer(context: Context, sensorDelay: Int = SensorManager.SENSOR_DELAY_GAME) :
    BaseSensor(context, Sensor.TYPE_ACCELEROMETER, sensorDelay),
    Accelerometer {

    override val hasValidReading: Boolean
        get() = gotReading
    private var gotReading = false

    override val acceleration: Vector3
        get() = Vector3(rawAcceleration[0], rawAcceleration[1], rawAcceleration[2])

    override val rawAcceleration: FloatArray = FloatArray(3)

    override fun handleSensorEvent(event: SensorEvent) {
        event.values.copyInto(rawAcceleration, endIndex = 3)
        gotReading = true
    }

}