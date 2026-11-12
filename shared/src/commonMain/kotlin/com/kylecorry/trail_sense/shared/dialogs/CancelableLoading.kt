package com.kylecorry.trail_sense.shared.dialogs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.AppHost

/** A spinner with a cancel button. */
fun Alerts.cancelableLoading(
    host: AppHost,
    title: String,
    cancelText: CharSequence = DialogLabels.Cancel,
    onCanceled: (() -> Unit)? = null
): TrailSenseDialog {
    return dialog(
        host,
        title,
        okText = null,
        cancelText = cancelText,
        cancelOnOutsideTouch = false,
        body = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    ) { cancelled ->
        if (cancelled) {
            onCanceled?.invoke()
        }
    }
}

/** Shows [cancelableLoading] while [action] runs; [onCancel] runs if the person cancels. */
inline fun Alerts.withCancelableLoading(
    host: AppHost,
    title: String,
    cancelText: CharSequence = DialogLabels.Cancel,
    noinline onCancel: () -> Unit,
    action: () -> Unit
) {
    val loadingAlert = cancelableLoading(host, title, cancelText, onCancel)
    try {
        action()
    } finally {
        loadingAlert.dismiss()
    }
}

/** Same dialog as [withCancelableLoading], but [action] can suspend while the dialog stays up. */
suspend fun Alerts.withSuspendableCancelableLoading(
    host: AppHost,
    title: String,
    cancelText: CharSequence = DialogLabels.Cancel,
    onCancel: () -> Unit,
    action: suspend () -> Unit
) {
    val loadingAlert = cancelableLoading(host, title, cancelText, onCancel)
    try {
        action()
    } finally {
        loadingAlert.dismiss()
    }
}
