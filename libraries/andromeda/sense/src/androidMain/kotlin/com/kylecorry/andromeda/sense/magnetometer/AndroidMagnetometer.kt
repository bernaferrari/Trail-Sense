package com.kylecorry.andromeda.sense.magnetometer
import com.kylecorry.andromeda.sense.magnetometer.Magnetometer

import com.kylecorry.andromeda.sense.BaseSensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import com.kylecorry.sol.math.Vector3

public class AndroidMagnetometer(context: Context, sensorDelay: Int = SensorManager.SENSOR_DELAY_GAME) :
    BaseSensor(context, Sensor.TYPE_MAGNETIC_FIELD, sensorDelay),
    Magnetometer {

    override val hasValidReading: Boolean
        get() = gotReading
    private var gotReading = false

    override val rawMagneticField: FloatArray = FloatArray(3)

    override val magneticField: Vector3
        get() = Vector3(rawMagneticField[0], rawMagneticField[1], rawMagneticField[2])

    override fun handleSensorEvent(event: SensorEvent) {
        event.values.copyInto(rawMagneticField, endIndex = 3)
        gotReading = true
    }

}