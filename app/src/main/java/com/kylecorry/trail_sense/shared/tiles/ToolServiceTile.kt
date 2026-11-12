package com.kylecorry.trail_sense.shared.tiles

import android.service.quicksettings.Tile
import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.widgets.tiles.AndromedaTileService
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.events.EventData
import com.kylecorry.trail_sense.shared.events.ToolBroadcasts
import com.kylecorry.trail_sense.shared.getInstance
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.getFeatureState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class ToolServiceTile(
    private val serviceId: String,
    private val stateChangeBroadcastId: String,
    private val frequencyChangeBroadcastId: String? = null,
    private val icon: ImageVector,
    private val isForegroundService: Boolean = false
) : AndromedaTileService() {
    protected val service by lazy { Tools.getService(this, serviceId) }

    private val formatter by lazy { FormatService.getInstance(this) }

    open fun stop() {
        CoroutineScope(Dispatchers.Default).launch {
            service?.disable()
        }
    }

    open fun start() {
        if (isForegroundService) {
            startForegroundService {
                service?.enable()
            }
        } else {
            CoroutineScope(Dispatchers.Default).launch {
                service?.enable()
            }
        }
    }

    override fun onClick() {
        super.onClick()

        when (service?.getFeatureState()) {
            FeatureState.On -> stop()
            FeatureState.Off -> start()
            else -> {
                // Do nothing, the feature is disabled
            }
        }
    }

    override fun onTileAdded() {
        super.onTileAdded()
        showIcon(icon)
    }

    override fun onStartListening() {
        showIcon(icon)
        ToolBroadcasts.subscribe(stateChangeBroadcastId, this::onStateChanged)
        updateState()
        if (frequencyChangeBroadcastId != null) {
            ToolBroadcasts.subscribe(frequencyChangeBroadcastId, this::onFrequencyChanged)
            updateFrequency()
        }
    }

    override fun onStopListening() {
        ToolBroadcasts.unsubscribe(stateChangeBroadcastId, this::onStateChanged)
        if (frequencyChangeBroadcastId != null) {
            ToolBroadcasts.unsubscribe(frequencyChangeBroadcastId, this::onFrequencyChanged)
        }
    }

    private fun onFrequencyChanged(data: EventData) {
        updateFrequency()
    }

    private fun onStateChanged(data: EventData) {
        updateState()
    }

    private fun updateState() {
        setState(
            when (service?.getFeatureState()) {
                FeatureState.On -> Tile.STATE_ACTIVE
                FeatureState.Off -> Tile.STATE_INACTIVE
                else -> Tile.STATE_UNAVAILABLE
            }
        )
    }

    private fun updateFrequency() {
        val frequency = service?.getFrequency() ?: return
        val formatted = formatter.formatDuration(frequency, includeSeconds = true)
        tryOrLog {
            setSubtitle(formatted)
        }
    }
}
