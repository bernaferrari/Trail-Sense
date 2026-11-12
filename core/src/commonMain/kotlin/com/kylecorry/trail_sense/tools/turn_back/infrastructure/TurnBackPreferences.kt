package com.kylecorry.trail_sense.tools.turn_back.infrastructure

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.settings.infrastructure.PreferenceRepo

class TurnBackPreferences(preferences: Preferences) : PreferenceRepo(preferences) {
    var useAlarm by BooleanPreference(
        cache,
        PreferenceKeys.turnBackUseAlarm,
        false
    )
}
