package com.kylecorry.trail_sense.settings.infrastructure

import com.kylecorry.trail_sense.settings.PreferenceKeys
import kotlin.time.Duration.Companion.seconds

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.FloatPreference
import kotlin.time.Duration

class FlashlightPreferenceRepo(preferences: Preferences) : PreferenceRepo(preferences) {

    var toggleWithVolumeButtons by BooleanPreference(
        cache,
        PreferenceKeys.flashlightToggleWithVolume,
        false
    )

    var controlScreenFlashlightWithVolumeButtons by BooleanPreference(
        cache,
        PreferenceKeys.flashlightControlScreenWithVolume,
        false
    )

    var shouldTimeout by BooleanPreference(
        cache,
        PreferenceKeys.flashlightShouldTimeout,
        false
    )

    var brightness by FloatPreference(
        cache,
        PreferenceKeys.torchBrightness,
        1f
    )

    var timeout: Duration
        get() {
            val seconds =
                cache.getLong(PreferenceKeys.flashlightTimeout) ?: (5L * 60)
            return seconds.seconds
        }
        set(value) {
            cache.putLong(PreferenceKeys.flashlightTimeout, value.inWholeSeconds)
        }

}
