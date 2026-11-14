package com.kylecorry.andromeda.sense.orientation

import com.kylecorry.sol.math.Euler

public interface Gyroscope : OrientationSensor {

    /**
     * The angular rate of the device in degrees per second
     */
    public val angularRate: Euler

    /**
     * The raw angular rate of the device in degrees per second
     */
    public val rawAngularRate: FloatArray
}