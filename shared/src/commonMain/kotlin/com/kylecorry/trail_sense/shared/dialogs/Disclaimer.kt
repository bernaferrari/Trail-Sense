package com.kylecorry.trail_sense.shared.dialogs

import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.views.compose.AppHost

/**
 * A dialog shown until the user accepts it once: accepting stores [shownValue] under [shownKey], and later calls
 * skip straight to [onClose] with agreed = true. Cancelling counts as accepting when [considerShownIfCancelled].
 */
fun Alerts.disclaimer(
    host: AppHost,
    title: String,
    message: CharSequence,
    shownKey: String,
    okText: CharSequence = DialogLabels.Ok,
    cancelText: CharSequence? = DialogLabels.Cancel,
    considerShownIfCancelled: Boolean = true,
    shownValue: Boolean = true,
    preferences: Preferences = getAppService(),
    onClose: (cancelled: Boolean, agreed: Boolean) -> Unit = { _, _ -> }
) {
    if (preferences.getBoolean(shownKey) == shownValue) {
        onClose(false, true)
        return
    }

    dialog(host, title, message, okText = okText, cancelText = cancelText, allowLinks = true) { cancelled ->
        val agreed = !cancelled || considerShownIfCancelled
        if (agreed) {
            preferences.putBoolean(shownKey, shownValue)
        }
        onClose(cancelled, agreed)
    }
}
