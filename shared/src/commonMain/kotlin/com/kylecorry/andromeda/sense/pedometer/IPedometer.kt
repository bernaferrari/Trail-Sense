package com.kylecorry.andromeda.sense.pedometer

import com.kylecorry.andromeda.core.sensors.Sensor

interface IPedometer: Sensor {
    val steps: Int
}