package com.kylecorry.trail_sense.settings.infrastructure

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.settings.PreferenceKeys

class PrivacyPreferences(context: Context) : PreferenceRepo(context) {

    var isScreenshotProtectionOn by BooleanPreference(
        cache,
        PreferenceKeys.privacyScreenshotProtection,
        false
    )

}
