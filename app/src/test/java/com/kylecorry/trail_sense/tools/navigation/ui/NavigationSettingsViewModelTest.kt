package com.kylecorry.trail_sense.tools.navigation.ui

import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.tools.navigation.infrastructure.NavigationPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class NavigationSettingsViewModelTest {

    private val keys = NavigationSettingsKeys(
        quickActionLeft = "left",
        quickActionRight = "right",
        showCalibrationDialog = "calibration",
        lockScreenPresence = "presence",
        keepUnlocked = "unlocked",
        speedometerType = "speedometer",
        showLinearCompass = "linear",
        showDialTicksWhenNoCompass = "ticks",
        showSunMoonCompass = "sun_moon",
        displayMultipleBeacons = "multi",
        numberOfVisibleBeacons = "visible",
        nearbyLinearOnly = "linear_only",
        showAzimuthIndicator = "azimuth",
        highDetailMode = "detail",
        dialLobes = "lobes"
    )

    private fun prefs(): NavigationPreferences {
        val prefs = mock<NavigationPreferences>()
        whenever(prefs.bearingDistance).thenReturn(Distance.meters(5000f))
        whenever(prefs.maxBeaconDistance).thenReturn(500f)
        whenever(prefs.showMultipleBeacons).thenReturn(false)
        whenever(prefs.numberOfVisibleBeacons).thenReturn(10)
        whenever(prefs.dialLobes).thenReturn(12)
        return prefs
    }

    private fun viewModel(cache: IPreferences, hasCompass: Boolean = true) = NavigationSettingsViewModel(
        prefs = prefs(),
        cache = cache,
        keys = keys,
        hasCompass = hasCompass,
        baseDistanceUnits = DistanceUnits.Meters
    )

    @Test
    fun withoutACompassNearbyBeaconsAreAlwaysShown() {
        val cache = mock<IPreferences>()
        val viewModel = viewModel(cache, hasCompass = false)
        assertTrue(viewModel.displayMultipleBeacons)
        verify(cache).putBoolean("multi", true)
    }

    @Test
    fun withACompassNearbyBeaconsFollowThePreference() {
        val viewModel = viewModel(mock())
        assertFalse(viewModel.displayMultipleBeacons)
    }

    @Test
    fun onlyTheStepSpeedometerAsksForActivityRecognition() {
        val cache = mock<IPreferences>()
        val viewModel = viewModel(cache)

        assertTrue(viewModel.updateSpeedometerType("instant_pedometer"))
        assertFalse(viewModel.updateSpeedometerType("average"))

        assertEquals("average", viewModel.speedometerType)
        verify(cache).putString("speedometer", "instant_pedometer")
        verify(cache).putString("speedometer", "average")
    }

    @Test
    fun theVisibleBeaconCountIsStoredAsText() {
        val cache = mock<IPreferences>()
        val viewModel = viewModel(cache)

        viewModel.updateNumberOfVisibleBeacons(7)

        assertEquals(7, viewModel.numberOfVisibleBeacons)
        verify(cache).putString("visible", "7")
    }
}
