package com.kylecorry.trail_sense.shared.dialogs

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf

/**
 * A dialog the app's [TrailSenseDialogHost] is showing. Callers keep it to close the dialog from code:
 * [dismiss] closes it quietly, [cancel] closes it as if the person backed out (running its cancel handling).
 */
class TrailSenseDialog internal constructor(
    private val host: TrailSenseDialogHostState,
    internal val onCancel: () -> Unit,
    internal val content: @Composable (TrailSenseDialog) -> Unit
) {
    /** Whether the dialog is still on screen (or waiting to be). */
    val isShowing: Boolean
        get() = host.contains(this)

    fun dismiss() {
        host.remove(this)
    }

    fun cancel() {
        onMain {
            if (host.removeNow(this)) {
                onCancel()
            }
        }
    }
}

/**
 * The dialogs the activity draws, in the order they were opened (the newest on top). Any code can open one through
 * [show]; the activity's Compose tree renders them with [TrailSenseDialogHost].
 */
class TrailSenseDialogHostState {
    internal val dialogs = mutableStateListOf<TrailSenseDialog>()

    /** Opens [content] as a dialog; [onCancel] runs when it's cancelled (see [TrailSenseDialog.cancel]). */
    fun show(
        onCancel: () -> Unit = {},
        content: @Composable (dialog: TrailSenseDialog) -> Unit
    ): TrailSenseDialog {
        val dialog = TrailSenseDialog(this, onCancel, content)
        onMain { dialogs.add(dialog) }
        return dialog
    }

    internal fun contains(dialog: TrailSenseDialog): Boolean = dialogs.contains(dialog)

    internal fun remove(dialog: TrailSenseDialog) {
        onMain { removeNow(dialog) }
    }

    internal fun removeNow(dialog: TrailSenseDialog): Boolean = dialogs.remove(dialog)
}

/** Draws every open dialog of [state]. */
@Composable
fun TrailSenseDialogHost(state: TrailSenseDialogHostState) {
    state.dialogs.toList().forEach { dialog ->
        key(dialog) {
            dialog.content(dialog)
        }
    }
}

private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

/** Dialogs can be opened and closed from any thread; the state they live in changes on the main thread. */
internal fun onMain(action: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        action()
    } else {
        mainHandler.post(action)
    }
}
