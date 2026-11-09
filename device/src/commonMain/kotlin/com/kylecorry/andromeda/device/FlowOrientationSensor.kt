package com.kylecorry.andromeda.device

import com.kylecorry.andromeda.sense.orientation.OrientationSensor
import com.kylecorry.sol.math.Quaternion
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class FlowOrientationSensor(readings: Flow<Reading<DeviceAttitude>>, scope: CoroutineScope) :
    FlowMeasurement<DeviceAttitude>(readings, scope, DeviceAttitude(Quaternion.zero)), OrientationSensor {
    override val orientation: Quaternion get() = value.orientation
    override val rawOrientation: FloatArray get() = value.orientation.toFloatArray()
    override val headingAccuracy: Float? get() = value.headingAccuracyDegrees
}
