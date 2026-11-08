package com.kylecorry.andromeda.sense.barometer

import com.kylecorry.andromeda.core.sensors.Sensor

interface IBarometer: Sensor {
    val pressure: Float
}