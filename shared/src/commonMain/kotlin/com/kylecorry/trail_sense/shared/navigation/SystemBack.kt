package com.kylecorry.trail_sense.shared.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventState

/**
 * Goes back the way the system back gesture does: back handlers on the screen still get a say, and with none the
 * host's own back runs (on Android, the activity finishes). Null when there's no navigation event dispatcher.
 */
@Composable
fun rememberSystemBack(): (() -> Unit)? {
    val dispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher ?: return null
    val input = remember(dispatcher) { DirectNavigationEventInput() }
    DisposableEffect(dispatcher, input) {
        dispatcher.addInput(input)
        onDispose { dispatcher.removeInput(input) }
    }
    return remember(input) { { input.backCompleted() } }
}

/**
 * Runs [onBack] instead of the default back while [enabled]. The common counterpart of the activity BackHandler.
 */
@Composable
fun SystemBackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = enabled,
        onBackCompleted = onBack
    )
}
