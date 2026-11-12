package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.sol.units.Coordinate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocationSetupTest {

    private val somewhere = Coordinate(42.0, -71.0)

    @Test
    fun unsetWhenManualWithoutALocation() {
        assertTrue(isLocationUnset(GPSLocationSource.Manual, Coordinate.zero, hasLocationPermission = true))
    }

    @Test
    fun unsetWhenThereIsNoPermissionAndNoLocation() {
        assertTrue(isLocationUnset(GPSLocationSource.GPS, Coordinate.zero, hasLocationPermission = false))
    }

    @Test
    fun setWhenTheGpsCanBeUsed() {
        assertFalse(isLocationUnset(GPSLocationSource.GPS, Coordinate.zero, hasLocationPermission = true))
    }

    @Test
    fun setWhenAManualLocationWasEntered() {
        assertFalse(isLocationUnset(GPSLocationSource.Manual, somewhere, hasLocationPermission = false))
    }
}
