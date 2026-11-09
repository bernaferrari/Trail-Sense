package com.kylecorry.andromeda.device

import com.kylecorry.andromeda.sense.compass.CompassSensor
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class FlowCompass(
    readings: Flow<Reading<Float>>,
    scope: CoroutineScope,
    var useTrueNorth: Boolean = false,
    var offset: Float = 0f,
) : FlowMeasurement<Float>(readings, scope, 0f), CompassSensor {
    override var declination: Float = 0f
    override val rawBearing: Float get() = Bearing.getBearing(value + offset + if (useTrueNorth) declination else 0f)
    override val bearing: Bearing get() = Bearing.from(rawBearing)
}
