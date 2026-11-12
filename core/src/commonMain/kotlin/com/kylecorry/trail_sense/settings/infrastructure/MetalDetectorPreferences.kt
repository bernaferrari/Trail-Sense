package com.kylecorry.trail_sense.settings.infrastructure

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.shared.device.DeviceProfile
import com.kylecorry.trail_sense.settings.PreferenceKeys

class MetalDetectorPreferences(
    preferences: Preferences,
    private val device: DeviceProfile
) : PreferenceRepo(preferences) {

    val showMetalDirection: Boolean
        get() {
            val hasGyro = device.hasGyroscope
            val enabled =
                cache.getBoolean(PreferenceKeys.experimentalMetalDirection) ?: false
            return hasGyro && enabled
        }

    val showSinglePole: Boolean = false

    val directionSensitivity: Float = 0.6f

    var isMetalAudioEnabled by BooleanPreference(
        cache,
        PreferenceKeys.useMetalAudio,
        false
    )
    var isMetalVibrationDisabled by BooleanPreference(
        cache,
        PreferenceKeys.noUseMetalVibration,
        false
    )
}
