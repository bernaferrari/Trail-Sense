package com.kylecorry.trail_sense.settings.infrastructure

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.shared.ErrorBannerReason

class ErrorPreferences(private val cache: Preferences) : IErrorPreferences {


    override fun canShowError(error: ErrorBannerReason): Boolean {
        return cache.getBoolean("pref_can_show_error_${error.id}") ?: true
    }

    override fun setCanShowError(error: ErrorBannerReason, canShow: Boolean) {
        cache.putBoolean("pref_can_show_error_${error.id}", canShow)
    }
}
