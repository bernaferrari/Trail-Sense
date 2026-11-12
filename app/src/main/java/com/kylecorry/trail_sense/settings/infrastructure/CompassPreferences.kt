package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.IntPreference
import com.kylecorry.andromeda.preferences.StringEnumPreference
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.compass.CompassSource
import com.kylecorry.trail_sense.shared.sensors.compass.CompassUpdateFrequency

class CompassPreferences(context: Context) : PreferenceRepo(context), ICompassPreferences {

    private val sensors = SensorService(context)

    override var compassSmoothing by IntPreference(
        cache,
        PreferenceKeys.compassFilterAmt,
        1
    )

    private var _useTrueNorthPref by BooleanPreference(
        cache,
        PreferenceKeys.useTrueNorth,
        true
    )

    override var useTrueNorth
        get() = !sensors.hasCompass() || _useTrueNorthPref
        set(value) {
            _useTrueNorthPref = value
        }

    override var source by StringEnumPreference(
        cache,
        PreferenceKeys.compassSource,
        CompassSource.entries.associateBy { it.id },
        CompassSource.CustomRotationVector
    )

    val updateFrequency by StringEnumPreference(
        cache,
        PreferenceKeys.compassUpdateFrequency,
        CompassUpdateFrequency.entries.associateBy { it.id },
        CompassUpdateFrequency.Fast
    )
}
