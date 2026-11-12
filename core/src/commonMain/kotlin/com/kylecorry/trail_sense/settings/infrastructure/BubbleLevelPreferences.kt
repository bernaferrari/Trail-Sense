package com.kylecorry.trail_sense.settings.infrastructure

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.preferences.FloatPreference
import com.kylecorry.trail_sense.settings.PreferenceKeys

class BubbleLevelPreferences(preferences: Preferences) : PreferenceRepo(preferences) {

    var threshold by FloatPreference(
        cache,
        PreferenceKeys.bubbleLevelThreshold,
        2.0f
    )
}
