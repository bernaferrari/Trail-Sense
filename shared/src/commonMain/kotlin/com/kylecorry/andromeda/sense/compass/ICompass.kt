package com.kylecorry.andromeda.sense.compass

import com.kylecorry.andromeda.core.sensors.Sensor
import com.kylecorry.sol.units.Bearing

interface ICompass: Sensor {
    val bearing: Bearing
    val rawBearing: Float
    var declination: Float
}