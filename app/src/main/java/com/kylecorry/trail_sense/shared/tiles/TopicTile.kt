package com.kylecorry.trail_sense.shared.tiles

import android.service.quicksettings.Tile
import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.widgets.tiles.AndromedaTileService
import com.kylecorry.trail_sense.shared.FeatureState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** A quick settings tile that follows a feature's state and a subtitle while the tile is listening. */
abstract class TopicTile : AndromedaTileService() {
    abstract val stateTopic: StateFlow<FeatureState>
    abstract val subtitleTopic: Flow<String>
    abstract val icon: ImageVector

    abstract fun stop()
    abstract fun start()

    private var listeningScope: CoroutineScope? = null

    override fun onTileAdded() {
        super.onTileAdded()
        showIcon(icon)
    }

    override fun onClick() {
        super.onClick()

        when (stateTopic.value) {
            FeatureState.On -> stop()
            FeatureState.Off -> start()
            else -> {
                // Do nothing, the feature is disabled
            }
        }
    }

    override fun onStartListening() {
        showIcon(icon)
        listeningScope?.cancel()
        listeningScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { scope ->
            scope.launch { subtitleTopic.collect(::onSubtitleChanged) }
            scope.launch { stateTopic.collect(::onStateChanged) }
        }
    }

    override fun onStopListening() {
        listeningScope?.cancel()
        listeningScope = null
    }

    private fun onSubtitleChanged(subtitle: String) {
        tryOrLog {
            setSubtitle(subtitle)
        }
    }

    private fun onStateChanged(state: FeatureState) {
        tryOrLog {
            setState(
                when (state) {
                    FeatureState.On -> Tile.STATE_ACTIVE
                    FeatureState.Off -> Tile.STATE_INACTIVE
                    FeatureState.Unavailable -> Tile.STATE_UNAVAILABLE
                }
            )
        }
    }
}
