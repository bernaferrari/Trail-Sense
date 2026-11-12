package com.kylecorry.trail_sense.tools.tools.infrastructure

import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.events.EventData
import com.kylecorry.trail_sense.shared.events.ToolBroadcasts
import com.kylecorry.trail_sense.shared.views.compose.AndroidDestinationHost
import kotlinx.coroutines.launch

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
    host: AndroidDestinationHost,
    toolBroadCastId: String,
    onReceiveBroadcast: suspend (data: EventData) -> Unit
) {
    val callback: (EventData) -> Unit = { data ->
        host.lifecycleOwner.lifecycleScope.launch {
            onReceiveBroadcast(data)
        }
    }
    host.lifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                ToolBroadcasts.subscribe(toolBroadCastId, callback)
            }

            Lifecycle.Event.ON_PAUSE -> {
                ToolBroadcasts.unsubscribe(toolBroadCastId, callback)
            }

            else -> {} // Do nothing
        }
    })
}
