package com.kylecorry.trail_sense.shared.sensors.speedometer

import android.content.Context
import com.kylecorry.andromeda.core.sensors.SharedSensor
import com.kylecorry.andromeda.core.sensors.Speedometer
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.ApproximateCoordinate
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.specifications.LocationChangedSpecification
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class BacktrackSpeedometer(private val context: Context) : SharedSensor(), Speedometer {

    private val pathService by lazy { getAppService<IPathService>() }
    private val prefs by lazy { UserPreferences(context) }

    private val scope = CoroutineScope(Dispatchers.Default)
    private var loadJob: Job? = null

    private var _speed: Speed? = null
    private var lastPointTime: Instant? = null

    private fun onPathChanged(points: List<PathPoint>) {
        val waypoints = points.sortedBy { point -> point.time }
        _speed = getSpeed(waypoints)
        lastPointTime = waypoints.lastOrNull()?.time
        notifyListeners()
    }

    override val hasValidReading: Boolean
        get() = prefs.paths.backtrackEnabled && _speed != null

    // Points are only recorded once the user has moved the minimum distance, so a missing point means they stopped
    override val speed: Speed
        get() {
            val stopped = Speed.from(0f, DistanceUnits.Meters, TimeUnits.Seconds)
            val lastTime = lastPointTime ?: return _speed ?: stopped
            val staleAfter = prefs.paths.backtrackRecordFrequency.times(2).plus(TIMING_BUFFER)
            return if ((Clock.System.now() - lastTime) > staleAfter) {
                stopped
            } else {
                _speed ?: stopped
            }
        }

    private fun getSpeed(waypoints: List<PathPoint>): Speed? {
        return if (waypoints.size < 2) {
            null
        } else {
            val last = waypoints.last()
            val secondLast = waypoints[waypoints.size - 2]
            val distance = secondLast.coordinate.distanceTo(last.coordinate)

            val defaultError = Distance.meters(10f)

            val locationIsTheSame = LocationChangedSpecification(
                ApproximateCoordinate.from(
                    secondLast.coordinate,
                    defaultError
                ),
                prefs.odometerDistanceThreshold
            ).not()

            if (locationIsTheSame.isSatisfiedBy(
                    ApproximateCoordinate.from(
                        last.coordinate,
                        defaultError
                    )
                )
            ) {
                return Speed.from(0f, DistanceUnits.Meters, TimeUnits.Seconds)
            }

            val lastTime = last.time
            val secondLastTime = secondLast.time
            if (lastTime == null || secondLastTime == null) {
                return Speed.from(0f, DistanceUnits.Meters, TimeUnits.Seconds)
            }
            val time = lastTime - secondLastTime
            Speed.from(
                distance / time.inWholeSeconds.coerceAtLeast(1).toFloat(),
                DistanceUnits.Meters,
                TimeUnits.Seconds
            )
        }
    }

    override fun startImpl() {
        // TODO: Listen for path changes for backtrack ID
        loadJob = scope.launch {
            val backtrack = pathService.getBacktrackPathId() ?: return@launch
            pathService.getWaypointsLive(backtrack).collect { points ->
                onMain { onPathChanged(points) }
            }
        }
    }

    override fun stopImpl() {
        loadJob?.cancel()
        loadJob = null
    }

    private companion object {
        val TIMING_BUFFER: Duration = 30.seconds
    }
}
