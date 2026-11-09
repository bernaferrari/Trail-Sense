package com.kylecorry.trail_sense.shared.sensors.compass

import com.kylecorry.andromeda.core.sensors.SharedSensor
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.compass.CompassSensor
import com.kylecorry.andromeda.sense.magnetometer.Magnetometer
import com.kylecorry.sol.units.Bearing

class MagQualityCompassWrapper(
    private val compass: CompassSensor,
    private val magnetometer: Magnetometer
) :
    SharedSensor(), CompassSensor {
    override val bearing: Bearing
        get() = compass.bearing

    override var declination: Float
        get() = compass.declination
        set(value) {
            compass.declination = value
        }
    override val hasValidReading: Boolean
        get() = compass.hasValidReading

    override val rawBearing: Float
        get() = compass.rawBearing

    override val quality: Quality
        get() = Quality.entries[minOf(
            magnetometer.quality.ordinal,
            compass.quality.ordinal,
            magnetometer.getQualityFromFieldStrength().ordinal
        )]

    override fun startImpl() {
        compass.start(this::onReading)
        magnetometer.start(this::onReading)
    }

    override fun stopImpl() {
        compass.stop(this::onReading)
        magnetometer.stop(this::onReading)
    }

    private fun onReading(): Boolean {
        notifyListeners()
        return true
    }
}
