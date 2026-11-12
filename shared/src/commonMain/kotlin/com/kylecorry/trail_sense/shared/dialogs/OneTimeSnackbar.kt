package com.kylecorry.trail_sense.shared.dialogs

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.views.compose.AppHost

/** A snackbar shown only the first time: once shown, [shownKey] is set and later calls do nothing. */
fun Alerts.oneTimeSnackbar(
    host: AppHost,
    message: String,
    shownKey: String,
    short: Boolean = true,
    preferences: Preferences = getAppService()
) {
    if (preferences.getBoolean(shownKey) != true) {
        snackbar(host, message, short)
        preferences.putBoolean(shownKey, true)
    }
}
