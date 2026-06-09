package com.kylecorry.trail_sense.shared.extensions

import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.R

fun TrailSenseDestinationHost.promptIfUnsavedChanges(
    hasChanges: () -> Boolean
): OnBackPressedCallback {
    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (hasChanges()) {
                Alerts.dialog(
                    andromedaActivity,
                    getString(R.string.unsaved_changes),
                    getString(R.string.unsaved_changes_message),
                    okText = getString(R.string.dialog_leave)
                ) { cancelled ->
                    if (!cancelled) {
                        remove()
                        andromedaActivity.onBackPressedDispatcher.onBackPressed()
                    }
                }
            } else {
                remove()
                andromedaActivity.onBackPressedDispatcher.onBackPressed()
            }
        }
    }
    andromedaActivity.onBackPressedDispatcher.addCallback(lifecycleOwner, callback)
    return callback
}

fun TrailSenseDestinationHost.getMarkdown(@StringRes resId: Int, vararg formatArgs: Any?): CharSequence {
    val service = AppServiceRegistry.get<MarkdownService>()
    return service.toMarkdown(getString(resId, *formatArgs))
}
