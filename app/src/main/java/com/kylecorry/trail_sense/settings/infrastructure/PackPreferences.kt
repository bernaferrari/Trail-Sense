package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.StringPreference
import com.kylecorry.trail_sense.settings.PreferenceKeys

class PackPreferences(context: Context) : PreferenceRepo(context) {

    var packSort by StringPreference(
        cache,
        PreferenceKeys.packItemSort,
        "category"
    )

}
