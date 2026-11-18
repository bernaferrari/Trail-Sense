package com.kylecorry.trail_sense.tools.paths.infrastructure

import com.kylecorry.trail_sense.shared.events.ToolBroadcasts
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.tools.paths.PathsBroadcasts
import com.kylecorry.trail_sense.tools.paths.PathsToolServices
import com.kylecorry.trail_sense.tools.paths.domain.BacktrackRecorder
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Instant

/** Records platform location updates into the same paths and preferences used by Android Backtrack. */
internal class LocationBacktrackService(
    override val name: String,
    private val preferences: PathsPreferences,
    private val recorder: BacktrackRecorder,
    private val finishPath: suspend () -> Unit,
    private val scope: CoroutineScope,
    private val logger: Logger,
    private val locations: () -> Flow<PathPoint>,
    private val isLocationBlocked: () -> Boolean,
    private val recordingChanged: (Boolean) -> Unit = {}
) : ToolService {
    override val id = PathsToolServices.BACKTRACK
    override fun getFrequency() = preferences.backtrackRecordFrequency
    override fun isEnabled() = preferences.backtrackEnabled
    override fun isRunning() = recording?.isActive == true
    override fun isBlocked() = false

    private val changes = Mutex()
    private var recording: Job? = null

    override suspend fun enable() = changes.withLock {
        preferences.backtrackEnabled = true
        startRecording()
    }

    override suspend fun disable() = changes.withLock {
        preferences.backtrackEnabled = false
        stopRecording()
        finishPath()
    }

    override suspend fun restart() = changes.withLock { startRecording() }

    override suspend fun stop() = changes.withLock { stopRecording() }

    private fun startRecording() {
        if (!isEnabled() || isRunning() || isLocationBlocked()) return
        recording = scope.launch {
            var lastRecorded: Instant? = null
            try {
                locations().collect { point ->
                    val time = point.time ?: return@collect
                    val previous = lastRecorded
                    if (previous != null && time - previous < getFrequency()) return@collect
                    if (recorder.record(point, isEnabled(), preferences.backtrackMinimumDistance)) {
                        lastRecorded = time
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                logger.error("Backtrack", "Unable to record location updates", exception)
            }
        }.also {
            it.invokeOnCompletion {
                recordingChanged(false)
                notifyChanged()
            }
        }
        recordingChanged(true)
        notifyChanged()
    }

    private suspend fun stopRecording() {
        recording?.cancelAndJoin()
        recording = null
        notifyChanged()
    }

    private fun notifyChanged() = ToolBroadcasts.broadcast(PathsBroadcasts.BACKTRACK_STATE_CHANGED)
}
