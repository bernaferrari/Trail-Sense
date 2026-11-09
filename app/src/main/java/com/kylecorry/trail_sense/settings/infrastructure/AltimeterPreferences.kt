package com.kylecorry.trail_sense.settings.infrastructure

import kotlin.time.Duration.Companion.hours

import android.content.Context
import com.kylecorry.trail_sense.shared.andromeda_temp.preferences.BooleanPreference
import com.kylecorry.trail_sense.shared.andromeda_temp.preferences.DurationPreference
import com.kylecorry.trail_sense.R

class AltimeterPreferences(context: Context) : PreferenceRepo(context) {

    val useFusedAltimeterContinuousCalibration by BooleanPreference(
        cache,
        context.getString(R.string.pref_altimeter_continuous_calibration),
        true
    )

    var fusedAltimeterForcedRecalibrationInterval by DurationPreference(
        cache,
        context.getString(R.string.pref_altimeter_forced_recalibration_interval),
        2.hours
    )

    var isDigitalElevationModelLoaded by BooleanPreference(
        cache,
        context.getString(R.string.pref_altimeter_dem_loaded),
        false
    )

}
