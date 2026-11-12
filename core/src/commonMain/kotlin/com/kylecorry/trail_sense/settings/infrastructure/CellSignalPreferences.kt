package com.kylecorry.trail_sense.settings.infrastructure

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.settings.PreferenceKeys

class CellSignalPreferences(private val cache: Preferences) {

    var populateCache by BooleanPreference(
        cache,
        PreferenceKeys.cellSignalRefreshCache,
        true
    )
}
