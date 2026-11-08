package com.kylecorry.andromeda.device

import com.kylecorry.andromeda.core.sensors.SharedSensor
import com.kylecorry.andromeda.sense.barometer.IBarometer
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** Adapts pressure readings in hectopascals to the shared barometer contract. */
class FlowBarometer(
    private val readings: Flow<Reading<Float>>,
    private val scope: CoroutineScope
) : SharedSensor(), IBarometer {
    override var pressure: Float = 0f
        private set
    override var hasValidReading: Boolean = false
        private set
    private var collection: Job? = null

    override fun startImpl() {
        collection = scope.launch(start = CoroutineStart.LAZY) {
            readings.collect { reading ->
                pressure = reading.value
                hasValidReading = true
                setEventTime(reading.time)
                notifyListeners()
            }
        }
        collection?.start()
    }

    override fun stopImpl() {
        collection?.cancel()
        collection = null
    }
}
