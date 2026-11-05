package com.kylecorry.trail_sense.tools.augmented_reality.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.kylecorry.trail_sense.settings.infrastructure.AugmentedRealityPreferences
import com.kylecorry.trail_sense.shared.withId
import java.time.Instant
import java.time.ZonedDateTime

/**
 * The AR screen's own choices: the mode, whether the camera is on, the time the sky is shown at, and the layers
 * sheet. Starts from the route; the layers, guidance and drawing stay with the screen's rendering objects.
 */
internal class AugmentedRealityViewModel(
    initialModeId: Long,
    initialCameraEnabled: Boolean,
    initialTime: String?,
    private val prefs: AugmentedRealityPreferences
) : ViewModel() {

    var mode = ARMode.entries.withId(initialModeId) ?: ARMode.Normal

    /** The time the route asked for; only applied the first time the screen is created. */
    var initialTime: ZonedDateTime? = initialTime?.let { ZonedDateTime.parse(it) }
        private set

    var isCameraEnabled = initialCameraEnabled
    var timeOverride: Instant? = null

    var showLayersSheet by mutableStateOf(false)
        private set
    var showBeaconLayer by mutableStateOf(false)
        private set
    var showPathLayer by mutableStateOf(false)
        private set
    var showAstronomyLayer by mutableStateOf(false)
        private set
    var showGridLayer by mutableStateOf(false)
        private set

    /** Takes the route's time once; later creations of the screen keep the current state. */
    fun consumeInitialTime(): ZonedDateTime? {
        return initialTime.also { initialTime = null }
    }

    fun openLayersSheet() {
        showBeaconLayer = prefs.showBeaconLayer
        showPathLayer = prefs.showPathLayer
        showAstronomyLayer = prefs.showAstronomyLayer
        showGridLayer = prefs.showGridLayer
        showLayersSheet = true
    }

    fun dismissLayersSheet() {
        showLayersSheet = false
    }

    fun onShowBeaconLayerChange(show: Boolean) {
        showBeaconLayer = show
        prefs.showBeaconLayer = show
    }

    fun onShowPathLayerChange(show: Boolean) {
        showPathLayer = show
        prefs.showPathLayer = show
    }

    fun onShowAstronomyLayerChange(show: Boolean) {
        showAstronomyLayer = show
        prefs.showAstronomyLayer = show
    }

    fun onShowGridLayerChange(show: Boolean) {
        showGridLayer = show
        prefs.showGridLayer = show
    }
}
