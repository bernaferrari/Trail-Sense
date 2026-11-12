package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.FloatPreference
import com.kylecorry.trail_sense.settings.PreferenceKeys

class BubbleLevelPreferences(context: Context) : PreferenceRepo(context) {

    var threshold by FloatPreference(
        cache,
        PreferenceKeys.bubbleLevelThreshold,
        2.0f
    )
}
