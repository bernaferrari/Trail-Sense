package com.kylecorry.andromeda.sense.compass

import com.kylecorry.andromeda.core.sensors.Sensor
import com.kylecorry.sol.units.Bearing

interface CompassSensor: Sensor {
    val bearing: Bearing
    val rawBearing: Float
    var declination: Float
}