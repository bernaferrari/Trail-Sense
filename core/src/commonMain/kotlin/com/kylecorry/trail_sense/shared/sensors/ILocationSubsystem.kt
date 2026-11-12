package com.kylecorry.trail_sense.shared.sensors

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import kotlin.time.Duration

/**
 * The last known location and elevation, from the GPS, the altimeter or the user's overrides.
 */
interface ILocationSubsystem {
    val location: Coordinate
    val locationAccuracy: Distance?
    val locationAge: Duration
    val isLocationStale: Boolean
    val elevation: Distance
    val elevationAge: Duration

    /**
     * Reads the location again instead of using the cached one.
     */
    suspend fun updateLocation()
}
