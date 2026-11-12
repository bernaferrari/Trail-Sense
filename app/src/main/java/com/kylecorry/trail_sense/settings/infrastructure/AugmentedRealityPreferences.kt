package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.StringEnumPreference
import com.kylecorry.andromeda.preferences.FloatPreference
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARPathAppearance

class AugmentedRealityPreferences(context: Context) : PreferenceRepo(context) {

    var beaconViewDistance by FloatPreference(
        cache,
        PreferenceKeys.augmentedRealityViewDistance,
        1000f
    )

    val maxRecommendedPathViewDistanceMeters by lazy {
        if (UserPreferences(context).baseDistanceUnits == DistanceUnits.Meters) {
            30f
        } else {
            Distance.feet(100f).meters().value
        }
    }

    var pathViewDistance by FloatPreference(
        cache,
        PreferenceKeys.augmentedRealityViewDistancePaths,
        20f
    )

    var useGyroOnlyAfterCalibration by BooleanPreference(
        cache,
        PreferenceKeys.arUseGyroOnlyAfterCalibration,
        false
    )

    var adjustForPathElevation by BooleanPreference(
        cache,
        PreferenceKeys.arAdjustForPathElevation,
        false
    )

    // Layer visibility
    var showBeaconLayer by BooleanPreference(
        cache,
        PreferenceKeys.showArBeaconLayer,
        true
    )

    var showPathLayer by BooleanPreference(
        cache,
        PreferenceKeys.showArPathLayer,
        true
    )

    val pathAppearance by StringEnumPreference(
        cache,
        PreferenceKeys.arPathAppearance,
        ARPathAppearance.entries.associateBy { it.id },
        ARPathAppearance.Ribbon
    )

    var showAstronomyLayer by BooleanPreference(
        cache,
        PreferenceKeys.showArAstronomyLayer,
        true
    )

    var showGridLayer by BooleanPreference(
        cache,
        PreferenceKeys.showArGridLayer,
        true
    )

    var showSatelliteLayer = false

    var showStars by BooleanPreference(
        cache,
        PreferenceKeys.showArStars,
        true
    )

    var showConstellations by BooleanPreference(
        cache,
        PreferenceKeys.showArConstellations,
        true
    )

    var showAstronomyBelowHorizon by BooleanPreference(
        cache,
        PreferenceKeys.showArAstronomyBelowHorizon,
        false
    )

    var showLowBrightnessObjects by BooleanPreference(
        cache,
        PreferenceKeys.showArLowBrightness,
        false
    )

    var increaseExposureAtNight by BooleanPreference(
        cache,
        PreferenceKeys.increaseArExposureAtNight,
        true
    )
}
