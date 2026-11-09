package com.kylecorry.andromeda.sense.accelerometer

import com.kylecorry.sol.math.Vector3
import com.kylecorry.andromeda.core.sensors.Sensor

public interface Accelerometer: Sensor {
    public val acceleration: Vector3
    public val rawAcceleration: FloatArray
}