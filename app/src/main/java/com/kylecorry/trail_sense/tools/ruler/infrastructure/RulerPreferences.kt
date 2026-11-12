package com.kylecorry.trail_sense.tools.ruler.infrastructure

import android.content.Context
import com.kylecorry.luna.text.toFloatCompat
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem

class RulerPreferences(private val context: Context) {

    private val cache by lazy { PreferencesSubsystem.getInstance(context).preferences }

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
