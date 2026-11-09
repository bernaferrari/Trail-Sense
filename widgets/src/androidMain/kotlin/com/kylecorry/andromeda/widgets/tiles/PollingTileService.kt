package com.kylecorry.andromeda.widgets.tiles

import android.service.quicksettings.Tile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Refreshes a quick-settings tile while the system is listening. */
abstract class PollingTileService(private val interval: Duration = 1.seconds) : AndromedaTileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var polling: Job? = null

    init { require(interval.isFinite() && interval > Duration.ZERO) }

    abstract fun isOn(): Boolean
    open fun isDisabled(): Boolean = false
    open fun isOff(): Boolean = !isOn() && !isDisabled()
    open fun onInterval() = Unit

    override fun onStartListening() {
        super.onStartListening()
        polling?.cancel()
        polling = scope.launch {
            while (isActive) {
                val tile = qsTile
                val newState = when {
                    isOn() -> Tile.STATE_ACTIVE
                    isDisabled() -> Tile.STATE_UNAVAILABLE
                    isOff() -> Tile.STATE_INACTIVE
                    else -> tile.state
                }
                if (tile.state != newState) {
                    tile.state = newState
                    tile.updateTile()
                }
                onInterval()
                delay(interval)
            }
        }
    }

    override fun onStopListening() {
        polling?.cancel()
        polling = null
        super.onStopListening()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
