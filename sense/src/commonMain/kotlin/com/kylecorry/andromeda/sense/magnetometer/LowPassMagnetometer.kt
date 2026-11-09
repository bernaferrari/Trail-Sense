package com.kylecorry.andromeda.sense.magnetometer

import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.sensors.SharedSensor
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.math.filters.LowPassFilter

/** Filters each axis of the supplied sensor independently. */
public class LowPassMagnetometer(
    private val sensor: Magnetometer,
    private val filterSize: Float = 0.03f,
    private val maintainStateOnRestart: Boolean = false
) : SharedSensor(), Magnetometer {
    private var filters = createFilters()
    private var gotReading = false

    override val rawMagneticField: FloatArray = FloatArray(3)
    override val magneticField: Vector3
        get() = Vector3(rawMagneticField[0], rawMagneticField[1], rawMagneticField[2])
    override val hasValidReading: Boolean
        get() = gotReading && sensor.hasValidReading
    override val quality: Quality get() = sensor.quality

    private fun createFilters() = List(3) { LowPassFilter(filterSize) }

    override fun startImpl() {
        gotReading = false
        if (!maintainStateOnRestart) {
            filters = createFilters()
            rawMagneticField.fill(0f)
        }
        resetSessionMetadata()
        sensor.start(this::onReading)
    }

    override fun stopImpl(): Unit = sensor.stop(this::onReading)

    private fun onReading(): Boolean {
        if (!sensor.hasValidReading) return true
        for (axis in 0..2) {
            rawMagneticField[axis] = filters[axis].filter(sensor.rawMagneticField[axis])
        }
        gotReading = true
        setEventTimeFrom(sensor)
        notifyListeners()
        return true
    }
}
