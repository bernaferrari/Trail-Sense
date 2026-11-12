package com.kylecorry.trail_sense.shared.sensors

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import kotlin.time.Duration

/** How a one-off sensor read may answer. */
enum class SensorRefreshPolicy {
    /** Read the sensor only when the cached value is invalid. */
    RefreshIfInvalid,

    /** Always read the sensor. */
    Refresh,

    /** Never start the sensor; answer from the cache. */
    Cache
}

/**
 * One-off location and elevation reads, from the cache or by starting the sensors just long enough to get a fix.
 * Values may be Coordinate.zero / 0 m when unknown.
 */
interface ISensorSubsystem {
    /** The last known location without starting the GPS. May be stale. */
    val lastKnownLocation: Coordinate

    /** The accuracy of [lastKnownLocation], or null when unknown (ex. the location is overridden). */
    val lastKnownLocationAccuracy: Distance?

    /** The last known elevation without starting the altimeter. May be stale. */
    val lastKnownElevation: Distance

    suspend fun getLocation(
        preferredPolicy: SensorRefreshPolicy = SensorRefreshPolicy.RefreshIfInvalid,
        timeout: Duration = SensorService.GPS_READ_TIMEOUT
    ): Coordinate

    suspend fun getElevation(
        preferredPolicy: SensorRefreshPolicy = SensorRefreshPolicy.RefreshIfInvalid,
        timeout: Duration = SensorService.GPS_READ_TIMEOUT
    ): Distance

    /** The location and elevation from a single GPS reading. */
    suspend fun getLocationAndElevation(
        preferredPolicy: SensorRefreshPolicy = SensorRefreshPolicy.RefreshIfInvalid,
        timeout: Duration = SensorService.GPS_READ_TIMEOUT
    ): Pair<Coordinate, Distance>
}
