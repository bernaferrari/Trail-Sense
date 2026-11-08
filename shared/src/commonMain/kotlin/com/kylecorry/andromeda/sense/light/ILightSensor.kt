package com.kylecorry.andromeda.sense.light

import com.kylecorry.andromeda.core.sensors.Sensor

interface ILightSensor: Sensor {
    val illuminance: Float
}