package com.kylecorry.trail_sense.shared

import android.content.Context
import android.widget.Toast
import com.kylecorry.andromeda.clipboard.Clipboard

/**
 * Copies [text] and shows [toastMessage] when it is not null.
 */
fun Clipboard.copy(context: Context, text: String, toastMessage: String?) {
    copy(context, text)
    if (toastMessage != null) {
        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
    }
}
