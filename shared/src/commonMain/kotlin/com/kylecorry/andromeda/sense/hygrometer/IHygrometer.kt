package com.kylecorry.andromeda.sense.hygrometer

import com.kylecorry.andromeda.core.sensors.Sensor

interface IHygrometer: Sensor {
    val humidity: Float
}