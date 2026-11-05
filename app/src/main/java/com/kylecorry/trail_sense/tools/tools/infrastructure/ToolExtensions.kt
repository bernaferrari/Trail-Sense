package com.kylecorry.trail_sense.tools.tools.infrastructure

import androidx.lifecycle.lifecycleScope

import android.os.Bundle
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kylecorry.trail_sense.shared.FeatureState
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.LifecycleResumeEffect

fun ToolService.getFeatureState(): FeatureState {
    return if (isBlocked()) {
        FeatureState.Unavailable
    } else if (isRunning()) {
        FeatureState.On
    } else {
        FeatureState.Off
    }
}

fun Tools.observe(
    host: TrailSenseDestinationHost,
    toolBroadCastId: String,
    onReceiveBroadcast: suspend (data: Bundle) -> Unit
) {
    val callback: (Bundle) -> Unit = { data ->
        host.lifecycleOwner.lifecycleScope.launch {
            onReceiveBroadcast(data)
        }
    }
    host.lifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                subscribe(toolBroadCastId, callback)
            }

            Lifecycle.Event.ON_PAUSE -> {
                unsubscribe(toolBroadCastId, callback)
            }

            else -> {} // Do nothing
        }
    })
}

/**
 * Runs [onReceiveBroadcast] for each [toolBroadcastId] broadcast while the screen is resumed: the composable form of
 * [observe], for screens that no longer take the destination host.
 */
@Composable
fun ToolBroadcastEffect(toolBroadcastId: String, onReceiveBroadcast: suspend (data: Bundle) -> Unit) {
    val scope = rememberCoroutineScope()
    val onReceive by rememberUpdatedState(onReceiveBroadcast)
    LifecycleResumeEffect(toolBroadcastId) {
        val callback: (Bundle) -> Unit = { data -> scope.launch { onReceive(data) } }
        Tools.subscribe(toolBroadcastId, callback)
        onPauseOrDispose { Tools.unsubscribe(toolBroadcastId, callback) }
    }
}
