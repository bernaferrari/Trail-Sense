package com.kylecorry.trail_sense.shared.extensions

import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost

fun TrailSenseDestinationHost.promptIfUnsavedChanges(
    hasChanges: () -> Boolean
): OnBackPressedCallback {
    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val leave = {
                remove()
                mainActivity.onBackPressedDispatcher.onBackPressed()
            }
            if (hasChanges()) {
                CustomUiUtils.confirmDiscardChanges(mainActivity, leave)
            } else {
                leave()
            }
        }
    }
    mainActivity.onBackPressedDispatcher.addCallback(lifecycleOwner, callback)
    return callback
}

fun TrailSenseDestinationHost.getMarkdown(@StringRes resId: Int, vararg formatArgs: Any?): CharSequence {
    val service = DependencyRegistry.get<MarkdownService>()
    return service.toMarkdown(getString(resId, *formatArgs))
}
