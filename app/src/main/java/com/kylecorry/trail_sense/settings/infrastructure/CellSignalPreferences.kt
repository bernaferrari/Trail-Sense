package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem

class CellSignalPreferences(private val context: Context) {
    private val cache by lazy { PreferencesSubsystem.getInstance(context).preferences }

    var populateCache by BooleanPreference(
        cache,
        PreferenceKeys.cellSignalRefreshCache,
        true
    )
}
