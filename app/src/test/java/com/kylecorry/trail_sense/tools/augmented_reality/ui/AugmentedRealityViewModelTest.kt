package com.kylecorry.trail_sense.tools.augmented_reality.ui

import com.kylecorry.trail_sense.settings.infrastructure.AugmentedRealityPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.time.ZonedDateTime

class AugmentedRealityViewModelTest {

    private val prefs = mock<AugmentedRealityPreferences> {
        on { showBeaconLayer } doReturn true
        on { showPathLayer } doReturn false
        on { showAstronomyLayer } doReturn true
        on { showGridLayer } doReturn false
    }

    @Test
    fun startsFromTheRouteAndUsesItsTimeOnce() {
        val time = "2024-06-01T12:00-04:00"
        val viewModel = AugmentedRealityViewModel(ARMode.Astronomy.id, false, time, prefs)

        assertEquals(ARMode.Astronomy, viewModel.mode)
        assertFalse(viewModel.isCameraEnabled)
        assertEquals(ZonedDateTime.parse(time), viewModel.consumeInitialTime())
        assertNull(viewModel.consumeInitialTime())
    }

    @Test
    fun anUnknownModeIsNormal() {
        val viewModel = AugmentedRealityViewModel(99, true, null, prefs)
        assertEquals(ARMode.Normal, viewModel.mode)
    }

    @Test
    fun theLayersSheetShowsAndSavesTheLayers() {
        val viewModel = AugmentedRealityViewModel(ARMode.Normal.id, true, null, prefs)

        viewModel.openLayersSheet()
        assertTrue(viewModel.showLayersSheet)
        assertTrue(viewModel.showBeaconLayer)
        assertFalse(viewModel.showPathLayer)
        assertTrue(viewModel.showAstronomyLayer)
        assertFalse(viewModel.showGridLayer)

        viewModel.onShowGridLayerChange(true)
        assertTrue(viewModel.showGridLayer)
        verify(prefs).showGridLayer = true

        viewModel.dismissLayersSheet()
        assertFalse(viewModel.showLayersSheet)
    }
}
