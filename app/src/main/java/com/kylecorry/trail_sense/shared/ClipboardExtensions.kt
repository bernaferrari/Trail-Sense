package com.kylecorry.trail_sense.shared

import android.content.Context
import com.kylecorry.andromeda.clipboard.Clipboard
import com.kylecorry.trail_sense.shared.dialogs.Alerts

/**
 * Copies [text] and, when [message] is not null, says so in the app's snackbar.
 */
fun Clipboard.copy(context: Context, text: String, message: String?) {
    copy(context, text)
    if (message != null) {
        Alerts.snackbar(context, message)
    }
}
