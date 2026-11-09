package com.kylecorry.andromeda.sense.location

import com.kylecorry.andromeda.core.time.monotonicNanos
import com.kylecorry.andromeda.core.sensors.Altimeter
import com.kylecorry.andromeda.core.sensors.Sensor
import com.kylecorry.andromeda.core.sensors.Speedometer
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

public interface GpsSensor : Sensor, Altimeter, Speedometer {
    /**
     * The location
     */
    public val location: Coordinate

    /**
     * The vertical accuracy in meters at the 68% confidence level
     */
    public val verticalAccuracy: Float?

    /**
     * The horizontal accuracy in meters at the 68% confidence level
     */
    public val horizontalAccuracy: Float?

    /**
     * The altitude above MSL in meters
     */
    public val mslAltitude: Float?

    /**
     * The bearing in degrees (True North)
     */
    public val bearing: Bearing?

    /**
     * The bearing in degrees (True North)
     */
    public val rawBearing: Float?

    /**
     * The bearing accuracy in degrees at the 68% confidence level
     */
    public val bearingAccuracy: Float?

    /**
     * The speed accuracy in meters per second at the 68% confidence level
     */
    public val speedAccuracy: Float?
}

/**
 * Determines if there is a recent location fix
 * @param maxFixAge the age at which a fix is considered lost
 */
public fun GpsSensor.hasFix(maxFixAge: Duration = 30.seconds): Boolean {
    if (!hasValidReading) {
        return false
    }

    return monotonicNanos() - eventTimeElapsedNanos <= maxFixAge.inWholeNanoseconds
}