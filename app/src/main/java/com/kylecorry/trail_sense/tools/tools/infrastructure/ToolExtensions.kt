package com.kylecorry.trail_sense.tools.tools.infrastructure

import android.os.Bundle
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kylecorry.trail_sense.shared.FeatureState
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
    fragment: TrailSenseDestinationHost,
    toolBroadCastId: String,
    onReceiveBroadcast: suspend (data: Bundle) -> Unit
) {
    val callback: (Bundle) -> Unit = { data ->
        fragment.lifecycleScope.launch {
            onReceiveBroadcast(data)
        }
    }
    fragment.lifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
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
