package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.sol.units.Coordinate

/**
 * Whether the app has no location at all: it doesn't use the GPS (manual source or no permission) and the manual
 * location was never set. Matches the GPS diagnostic's "location not set" error.
 */
fun isLocationUnset(
    source: GPSLocationSource,
    locationOverride: Coordinate,
    hasLocationPermission: Boolean
): Boolean {
    val usesOverride = source == GPSLocationSource.Manual || !hasLocationPermission
    return usesOverride && locationOverride == Coordinate.zero
}
