package com.kylecorry.andromeda.device

import com.kylecorry.andromeda.core.sensors.SharedSensor
import com.kylecorry.andromeda.sense.magnetometer.IMagnetometer
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class FlowMagnetometer(
    private val readings: Flow<Reading<Vector3>>,
    private val scope: CoroutineScope
) : SharedSensor(), IMagnetometer {
    override var magneticField: Vector3 = Vector3.zero
        private set
    override val rawMagneticField: FloatArray get() = floatArrayOf(magneticField.x, magneticField.y, magneticField.z)
    override var hasValidReading: Boolean = false
        private set
    private var collection: Job? = null

    override fun startImpl() {
        collection = scope.launch(start = CoroutineStart.LAZY) {
            readings.collect { reading ->
                magneticField = reading.value
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
