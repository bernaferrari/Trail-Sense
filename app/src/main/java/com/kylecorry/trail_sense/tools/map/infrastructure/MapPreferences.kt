package com.kylecorry.trail_sense.tools.map.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.settings.infrastructure.PreferenceRepo

class MapPreferences(context: Context) : PreferenceRepo(context) {

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
