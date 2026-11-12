package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.PreferenceKeys

class TidePreferences(context: Context) : PreferenceRepo(context), ITidePreferences {

    override var showNearestTide by BooleanPreference(
        cache,
        PreferenceKeys.showNearestTide,
        false
    )

    override var lastTide: Long?
        get() = cache.getLong(context.getString(R.string.last_tide_id))
        set(value) {
            if (value != null) {
                cache.putLong(context.getString(R.string.last_tide_id), value)
            } else {
                cache.remove(context.getString(R.string.last_tide_id))
            }
        }
}
