package com.kylecorry.trail_sense.shared.extensions

import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.fragments.onBackPressed
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.R

fun TrailSenseComposeDestinationFragment.promptIfUnsavedChanges(
    hasChanges: () -> Boolean
): OnBackPressedCallback {
    val activity = requireActivity()
    return onBackPressed {
        if (hasChanges()) {
            Alerts.dialog(
                activity,
                getString(R.string.unsaved_changes),
                getString(R.string.unsaved_changes_message),
                okText = getString(R.string.dialog_leave)
            ) { cancelled ->
                if (!cancelled) {
                    remove()
                    activity.onBackPressedDispatcher.onBackPressed()
                }
            }
        } else {
            remove()
            activity.onBackPressedDispatcher.onBackPressed()
        }
    }
}

fun TrailSenseComposeDestinationFragment.getMarkdown(@StringRes resId: Int, vararg formatArgs: Any?): CharSequence {
    val service = AppServiceRegistry.get<MarkdownService>()
    return service.toMarkdown(getString(resId, *formatArgs))
}
