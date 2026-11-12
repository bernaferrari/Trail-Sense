package com.kylecorry.trail_sense.settings.infrastructure

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.settings.PreferenceKeys

class TidePreferences(preferences: Preferences) : PreferenceRepo(preferences), ITidePreferences {

    override var showNearestTide by BooleanPreference(
        cache,
        PreferenceKeys.showNearestTide,
        false
    )

    override var lastTide: Long?
        get() = cache.getLong(PreferenceKeys.lastTideId)
        set(value) {
            if (value != null) {
                cache.putLong(PreferenceKeys.lastTideId, value)
            } else {
                cache.remove(PreferenceKeys.lastTideId)
            }
        }
}
