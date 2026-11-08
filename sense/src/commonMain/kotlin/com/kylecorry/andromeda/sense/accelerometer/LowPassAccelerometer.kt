package com.kylecorry.andromeda.sense.accelerometer

import com.kylecorry.andromeda.core.sensors.SharedSensor
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.math.filters.LowPassFilter

/** Filters each axis of the supplied sensor independently. */
class LowPassAccelerometer(
    private val sensor: IAccelerometer,
    private val filterSize: Float = 0.05f,
    private val maintainStateOnRestart: Boolean = false
) : SharedSensor(), IAccelerometer {
    private var filters = createFilters()
    private var gotReading = false

    override val rawAcceleration = FloatArray(3)
    override val acceleration: Vector3
        get() = Vector3(rawAcceleration[0], rawAcceleration[1], rawAcceleration[2])
    override val hasValidReading: Boolean
        get() = gotReading && sensor.hasValidReading
    override val quality get() = sensor.quality

    private fun createFilters() = List(3) { LowPassFilter(filterSize) }

    override fun startImpl() {
        gotReading = false
        if (!maintainStateOnRestart) {
            filters = createFilters()
            rawAcceleration.fill(0f)
        }
        resetSessionMetadata()
        sensor.start(this::onReading)
    }

    override fun stopImpl() = sensor.stop(this::onReading)

    private fun onReading(): Boolean {
        if (!sensor.hasValidReading) return true
        for (axis in 0..2) {
            rawAcceleration[axis] = filters[axis].filter(sensor.rawAcceleration[axis])
        }
        gotReading = true
        setEventTimeFrom(sensor)
        notifyListeners()
        return true
    }
}
