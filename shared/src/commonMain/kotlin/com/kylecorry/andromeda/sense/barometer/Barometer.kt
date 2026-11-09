package com.kylecorry.andromeda.sense.barometer

import com.kylecorry.andromeda.core.sensors.Sensor

interface Barometer: Sensor {
    val pressure: Float
}