package com.kylecorry.trail_sense.settings.infrastructure

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.IntPreference
import com.kylecorry.andromeda.preferences.StringEnumPreference
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.shared.device.DeviceProfile
import com.kylecorry.trail_sense.shared.sensors.compass.CompassSource

class CompassPreferences(
    preferences: Preferences,
    private val device: DeviceProfile
) : PreferenceRepo(preferences), ICompassPreferences {

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
        get() = !device.hasCompass || _useTrueNorthPref
        set(value) {
            _useTrueNorthPref = value
        }

    override var source by StringEnumPreference(
        cache,
        PreferenceKeys.compassSource,
        CompassSource.entries.associateBy { it.id },
        CompassSource.CustomRotationVector
    )
}
