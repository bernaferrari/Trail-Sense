package com.kylecorry.andromeda.sense.pedometer

import com.kylecorry.andromeda.core.sensors.Sensor

interface Pedometer: Sensor {
    val steps: Int
}