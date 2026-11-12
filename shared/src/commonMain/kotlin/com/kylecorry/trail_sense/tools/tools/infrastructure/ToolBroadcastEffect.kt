package com.kylecorry.trail_sense.tools.tools.infrastructure

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.kylecorry.trail_sense.shared.events.EventData
import com.kylecorry.trail_sense.shared.events.ToolBroadcasts
import kotlinx.coroutines.launch

/**
 * Runs [onReceiveBroadcast] for each [toolBroadcastId] broadcast while the screen is resumed.
 */
@Composable
fun ToolBroadcastEffect(toolBroadcastId: String, onReceiveBroadcast: suspend (data: EventData) -> Unit) {
    val scope = rememberCoroutineScope()
    val onReceive by rememberUpdatedState(onReceiveBroadcast)
    LifecycleResumeEffect(toolBroadcastId) {
        val callback: (EventData) -> Unit = { data -> scope.launch { onReceive(data) } }
        ToolBroadcasts.subscribe(toolBroadcastId, callback)
        onPauseOrDispose { ToolBroadcasts.unsubscribe(toolBroadcastId, callback) }
    }
}
