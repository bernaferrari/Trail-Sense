package com.kylecorry.andromeda.sense.orientation

import com.kylecorry.sol.math.Quaternion
import com.kylecorry.andromeda.core.sensors.Sensor

public interface OrientationSensor: Sensor {
    public val orientation: Quaternion
    public val rawOrientation: FloatArray
    public val headingAccuracy: Float?
}