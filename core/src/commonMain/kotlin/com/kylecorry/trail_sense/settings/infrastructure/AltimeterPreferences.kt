package com.kylecorry.trail_sense.settings.infrastructure

import com.kylecorry.trail_sense.settings.PreferenceKeys
import kotlin.time.Duration.Companion.hours
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.DurationPreference

class AltimeterPreferences(preferences: Preferences) : PreferenceRepo(preferences) {

    val useFusedAltimeterContinuousCalibration by BooleanPreference(
        cache,
        PreferenceKeys.altimeterContinuousCalibration,
        true
    )

    var fusedAltimeterForcedRecalibrationInterval by DurationPreference(
        cache,
        PreferenceKeys.altimeterForcedRecalibrationInterval,
        2.hours
    )

    var isDigitalElevationModelLoaded by BooleanPreference(
        cache,
        PreferenceKeys.altimeterDemLoaded,
        false
    )

}
