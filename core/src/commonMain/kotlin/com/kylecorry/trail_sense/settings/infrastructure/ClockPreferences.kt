package com.kylecorry.trail_sense.settings.infrastructure

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.settings.PreferenceKeys

class ClockPreferences(preferences: Preferences) : PreferenceRepo(preferences) {

    var showAnalogClock by BooleanPreference(
        cache,
        PreferenceKeys.enableAnalogClock,
        true
    )

}
