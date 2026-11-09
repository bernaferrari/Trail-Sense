package com.kylecorry.trail_sense.settings.infrastructure

import kotlin.time.Duration.Companion.seconds

import android.content.Context
import com.kylecorry.trail_sense.shared.andromeda_temp.preferences.BooleanPreference
import com.kylecorry.trail_sense.shared.andromeda_temp.preferences.FloatPreference
import com.kylecorry.trail_sense.R
import kotlin.time.Duration

class FlashlightPreferenceRepo(context: Context) : PreferenceRepo(context) {

    var toggleWithVolumeButtons by BooleanPreference(
        cache,
        getString(R.string.pref_flashlight_toggle_with_volume),
        false
    )

    var controlScreenFlashlightWithVolumeButtons by BooleanPreference(
        cache,
        getString(R.string.pref_flashlight_control_screen_with_volume),
        false
    )

    var shouldTimeout by BooleanPreference(
        cache,
        getString(R.string.pref_flashlight_should_timeout),
        false
    )

    var brightness by FloatPreference(
        cache,
        getString(R.string.pref_torch_brightness),
        1f
    )

    var timeout: Duration
        get() {
            val seconds =
                cache.getLong(getString(R.string.pref_flashlight_timeout)) ?: (5L * 60)
            return seconds.seconds
        }
        set(value) {
            cache.putLong(getString(R.string.pref_flashlight_timeout), value.inWholeSeconds)
        }

}
