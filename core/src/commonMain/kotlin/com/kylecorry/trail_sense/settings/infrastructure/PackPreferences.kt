package com.kylecorry.trail_sense.settings.infrastructure

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.preferences.StringPreference
import com.kylecorry.trail_sense.settings.PreferenceKeys

class PackPreferences(preferences: Preferences) : PreferenceRepo(preferences) {

    var packSort by StringPreference(
        cache,
        PreferenceKeys.packItemSort,
        "category"
    )

}
