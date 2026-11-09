package com.kylecorry.andromeda.device

import com.kylecorry.andromeda.sense.magnetometer.Magnetometer
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class FlowMagnetometer(readings: Flow<Reading<Vector3>>, scope: CoroutineScope) :
    FlowMeasurement<Vector3>(readings, scope, Vector3.zero), Magnetometer {
    override val magneticField: Vector3 get() = value
    override val rawMagneticField: FloatArray get() = floatArrayOf(value.x, value.y, value.z)
}
