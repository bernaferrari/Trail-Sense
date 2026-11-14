package com.kylecorry.andromeda.sense.compass

import com.kylecorry.andromeda.core.sensors.Sensor
import com.kylecorry.sol.units.Bearing

public interface CompassSensor: Sensor {
    public val bearing: Bearing
    public val rawBearing: Float
    public var declination: Float
}