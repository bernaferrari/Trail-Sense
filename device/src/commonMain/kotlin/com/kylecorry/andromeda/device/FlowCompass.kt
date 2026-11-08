package com.kylecorry.andromeda.device

import com.kylecorry.andromeda.core.sensors.SharedSensor
import com.kylecorry.andromeda.sense.compass.ICompass
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class FlowCompass(
    private val readings: Flow<Reading<Float>>,
    private val scope: CoroutineScope,
    var useTrueNorth: Boolean = false,
    var offset: Float = 0f,
) : SharedSensor(), ICompass {
    private var magneticHeading = 0f
    private var collection: Job? = null
    override var declination: Float = 0f
    override val rawBearing: Float get() = Bearing.getBearing(magneticHeading + offset + if (useTrueNorth) declination else 0f)
    override val bearing: Bearing get() = Bearing.from(rawBearing)
    override var hasValidReading: Boolean = false
        private set

    override fun startImpl() {
        collection = scope.launch(start = CoroutineStart.LAZY) {
            readings.collect { reading ->
                magneticHeading = reading.value
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
