package com.kylecorry.trail_sense.settings.infrastructure

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.trail_sense.settings.PreferenceKeys

class PrivacyPreferences(preferences: Preferences) : PreferenceRepo(preferences) {

    var isScreenshotProtectionOn by BooleanPreference(
        cache,
        PreferenceKeys.privacyScreenshotProtection,
        false
    )

}
