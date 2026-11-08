package com.kylecorry.andromeda.device

import com.kylecorry.andromeda.core.sensors.SharedSensor
import com.kylecorry.andromeda.sense.accelerometer.IAccelerometer
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class FlowAccelerometer(
    private val readings: Flow<Reading<Vector3>>,
    private val scope: CoroutineScope
) : SharedSensor(), IAccelerometer {
    override var acceleration: Vector3 = Vector3.zero
        private set
    override val rawAcceleration: FloatArray get() = floatArrayOf(acceleration.x, acceleration.y, acceleration.z)
    override var hasValidReading: Boolean = false
        private set
    private var collection: Job? = null

    override fun startImpl() {
        collection = scope.launch(start = CoroutineStart.LAZY) {
            readings.collect { reading ->
                acceleration = reading.value
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
