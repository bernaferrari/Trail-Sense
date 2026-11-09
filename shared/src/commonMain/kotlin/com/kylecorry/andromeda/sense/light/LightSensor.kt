package com.kylecorry.andromeda.sense.light

import com.kylecorry.andromeda.core.sensors.Sensor

interface LightSensor: Sensor {
    val illuminance: Float
}