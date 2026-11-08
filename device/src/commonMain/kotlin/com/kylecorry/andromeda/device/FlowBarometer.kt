package com.kylecorry.andromeda.device

import com.kylecorry.andromeda.sense.barometer.IBarometer
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

/** Adapts pressure readings in hectopascals to the shared barometer contract. */
class FlowBarometer(readings: Flow<Reading<Float>>, scope: CoroutineScope) :
    FlowMeasurement<Float>(readings, scope, 0f), IBarometer {
    override val pressure: Float get() = value
}
