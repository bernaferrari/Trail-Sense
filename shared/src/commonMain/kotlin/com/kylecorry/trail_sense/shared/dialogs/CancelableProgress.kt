package com.kylecorry.trail_sense.shared.dialogs

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.AppHost

/** A progress bar with a cancel button; the returned function sets the progress (0 to 1) from any thread. */
fun Alerts.cancelableProgress(
    host: AppHost,
    title: String,
    cancelText: CharSequence = DialogLabels.Cancel,
    onCanceled: (() -> Unit)? = null
): Pair<TrailSenseDialog, (Float) -> Unit> {
    val progress = mutableFloatStateOf(0f)
    val dialog = dialog(
        host,
        title,
        okText = null,
        cancelText = cancelText,
        cancelOnOutsideTouch = false,
        body = {
            // The old bar sat 24 dp in from every edge of the dialog.
            ProgressBody(progress = { progress.floatValue }, modifier = Modifier.padding(vertical = 16.dp))
        }
    ) { cancelled ->
        if (cancelled) {
            onCanceled?.invoke()
        }
    }
    return dialog to { value -> onMain { progress.floatValue = value.coerceIn(0f, 1f) } }
}

/** Shows [cancelableProgress] while [action] runs, handing it the progress setter. */
inline fun Alerts.withCancelableProgress(
    host: AppHost,
    title: String,
    cancelText: CharSequence = DialogLabels.Cancel,
    noinline onCancel: () -> Unit,
    action: ((Float) -> Unit) -> Unit
) {
    val (progressAlert, setProgress) = cancelableProgress(host, title, cancelText, onCancel)
    try {
        action(setProgress)
    } finally {
        progressAlert.dismiss()
    }
}
