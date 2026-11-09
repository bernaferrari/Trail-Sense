package com.kylecorry.andromeda.sense.hygrometer

import com.kylecorry.andromeda.core.sensors.Sensor

interface Hygrometer: Sensor {
    val humidity: Float
}