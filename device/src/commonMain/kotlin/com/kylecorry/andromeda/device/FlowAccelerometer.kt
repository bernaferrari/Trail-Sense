package com.kylecorry.andromeda.device

import com.kylecorry.andromeda.sense.accelerometer.IAccelerometer
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class FlowAccelerometer(readings: Flow<Reading<Vector3>>, scope: CoroutineScope) :
    FlowMeasurement<Vector3>(readings, scope, Vector3.zero), IAccelerometer {
    override val acceleration: Vector3 get() = value
    override val rawAcceleration: FloatArray get() = floatArrayOf(value.x, value.y, value.z)
}
