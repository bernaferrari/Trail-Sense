package com.kylecorry.trail_sense.shared.views.compose

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kylecorry.trail_sense.shared.dialogs.TrailSenseDialogHost
import com.kylecorry.trail_sense.shared.dialogs.TrailSenseDialogHostState
import com.kylecorry.trail_sense.shared.dialogs.onMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * What the activity draws above every screen and that any code can reach from a Context: dialogs, the snackbar and
 * overlays (such as the quick ruler). The activity owns one ([AppHostOwner]) and renders it with [AppHostDialogs],
 * [AppHostSnackbar] and [AppHostOverlays].
 */
class AppHost {
    val dialogs = TrailSenseDialogHostState()
    val snackbars = SnackbarHostState()
    internal val overlays = mutableStateListOf<AppOverlay>()

    /** Snackbars outlive the screen that asked for them, like the old View snackbars did. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Shows a snackbar, replacing the current one; returns true when its action was pressed. */
    suspend fun showSnackbar(message: String, actionLabel: String? = null, long: Boolean = false): Boolean {
        snackbars.currentSnackbarData?.dismiss()
        val result = snackbars.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = if (long) SnackbarDuration.Long else SnackbarDuration.Short
        )
        return result == SnackbarResult.ActionPerformed
    }

    /** Shows a snackbar without waiting for it; [onAction] runs when its action is pressed. */
    fun snackbar(message: String, actionLabel: String? = null, long: Boolean = false, onAction: () -> Unit = {}) {
        scope.launch {
            if (showSnackbar(message, actionLabel, long)) {
                onAction()
            }
        }
    }

    /** Draws [content] above the screens (inside the area between the top and the navigation bar) until removed. */
    fun addOverlay(content: @Composable BoxScope.() -> Unit): AppOverlay {
        val overlay = AppOverlay(this, content)
        onMain { overlays.add(overlay) }
        return overlay
    }

    internal fun close() {
        scope.cancel()
    }

    companion object {
        private var current: AppHost? = null
        private val detached by lazy { AppHost() }

        /** The host of the activity behind [context], or the running activity's when [context] isn't one. */
        fun from(context: Context): AppHost {
            var c: Context? = context
            while (c != null) {
                if (c is AppHostOwner) return c.appHost
                c = (c as? ContextWrapper)?.baseContext
            }
            return current ?: detached
        }

        /** Called by the owning activity while it's alive. */
        fun attach(host: AppHost) {
            current = host
        }

        fun detach(host: AppHost) {
            if (current === host) {
                current = null
            }
            host.close()
        }
    }
}

/** An Activity that draws an [AppHost]. */
interface AppHostOwner {
    val appHost: AppHost
}

/** Content drawn above the screens; [remove] takes it away. */
class AppOverlay internal constructor(
    private val host: AppHost,
    internal val content: @Composable BoxScope.() -> Unit
) {
    fun remove() {
        onMain { host.overlays.remove(this) }
    }
}

@Composable
fun AppHostDialogs(host: AppHost) {
    TrailSenseDialogHost(host.dialogs)
}

@Composable
fun AppHostOverlays(host: AppHost, modifier: Modifier = Modifier) {
    host.overlays.toList().forEach { overlay ->
        key(overlay) {
            Box(modifier.fillMaxSize()) {
                overlay.content(this)
            }
        }
    }
}

@Composable
fun BoxScope.AppHostSnackbar(host: AppHost, modifier: Modifier = Modifier) {
    SnackbarHost(host.snackbars, modifier.align(Alignment.BottomCenter))
}
