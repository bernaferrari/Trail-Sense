package com.kylecorry.trail_sense.tools.ruler.infrastructure

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.luna.text.toFloatCompat
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.settings.PreferenceKeys

class RulerPreferences(private val cache: Preferences) {


    var rulerScale: Float
        get() {
            val raw = cache.getString(PreferenceKeys.rulerCalibration) ?: "1"
            return raw.toFloatCompat() ?: 1f
        }
        set(value) {
            cache.putString(PreferenceKeys.rulerCalibration, value.toString())
        }

    var rulerFlipped by BooleanPreference(
        cache,
        PreferenceKeys.rulerFlipDirection,
        false
    )

}
