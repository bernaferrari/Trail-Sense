package com.kylecorry.trail_sense.tools.map.infrastructure

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.settings.infrastructure.PreferenceRepo

class MapPreferences(preferences: Preferences) : PreferenceRepo(preferences) {

    var keepScreenUnlockedWhileOpen by BooleanPreference(
        cache,
        PreferenceKeys.mapKeepUnlocked,
        false
    )

    var saveMapState by BooleanPreference(
        cache,
        PreferenceKeys.saveMapState,
        false
    )

    var highDetailMode by BooleanPreference(
        cache,
        PreferenceKeys.mapHighDetailMode,
        false
    )
}
