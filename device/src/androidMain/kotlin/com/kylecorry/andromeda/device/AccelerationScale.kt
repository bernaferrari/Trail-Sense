package com.kylecorry.andromeda.device

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.ksensor.core.context.KSensorContext

internal actual fun accelerationScale(): Float {
    val manager = KSensorContext.get().getSystemService(Context.SENSOR_SERVICE) as SensorManager
    return manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.maximumRange ?: 1f
}
