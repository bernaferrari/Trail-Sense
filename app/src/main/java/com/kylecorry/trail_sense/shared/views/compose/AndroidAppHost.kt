package com.kylecorry.trail_sense.shared.views.compose

import android.content.Context
import android.content.ContextWrapper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner

/** The host of the activity behind [context], or the running activity's when [context] isn't one. */
fun AppHost.Companion.from(context: Context): AppHost {
    return attachedTo(context) ?: current()
}

/**
 * The host that is on screen right now (the activity behind [context], or the running one), or null while the app is
 * in the background and nothing would draw it.
 */
fun AppHost.Companion.visible(context: Context): AppHost? {
    val isForeground = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    return if (isForeground) attachedTo(context) else null
}

private fun attachedTo(context: Context): AppHost? {
    var c: Context? = context
    while (c != null) {
        if (c is AppHostOwner) return c.appHost
        c = (c as? ContextWrapper)?.baseContext
    }
    return AppHost.attachedHost
}
