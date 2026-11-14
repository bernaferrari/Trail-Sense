package com.kylecorry.andromeda.sense.magnetometer

import com.kylecorry.sol.math.Vector3
import com.kylecorry.andromeda.core.sensors.Sensor

public interface Magnetometer: Sensor {
    public val magneticField: Vector3
    public val rawMagneticField: FloatArray
}