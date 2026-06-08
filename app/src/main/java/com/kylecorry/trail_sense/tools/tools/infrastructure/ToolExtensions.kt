package com.kylecorry.trail_sense.tools.tools.infrastructure

import android.os.Bundle
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
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
    fragment: TrailSenseComposeDestinationFragment,
    toolBroadCastId: String,
    onReceiveBroadcast: suspend (data: Bundle) -> Unit
) {
    val callback: (Bundle) -> Unit = { data ->
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            onReceiveBroadcast(data)
        }
    }
    fragment.viewLifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
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
