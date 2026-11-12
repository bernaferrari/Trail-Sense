package com.kylecorry.trail_sense.tools.paths.infrastructure

import com.kylecorry.trail_sense.tools.paths.PathsToolServices
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCatalog
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.sensors.ILocationSubsystem
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.ShouldLoadPathSpecification
import com.kylecorry.trail_sense.tools.paths.domain.ShouldUnloadPathSpecification
import kotlin.time.Clock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PathLoader(
    private val pathService: IPathService,
    private val isBacktrackOn: () -> Boolean = {
        getAppService<ToolCatalog>().getService(PathsToolServices.BACKTRACK)?.isEnabled() == true
    }
) {

    val points: MutableMap<Long, List<PathPoint>> = mutableMapOf()
    val pointsLock = Mutex()
    private val updateLock = Mutex()

    suspend fun getPointsWithBacktrack(): Map<Long, List<PathPoint>> = onIO {
        val currentBacktrackPathId = pathService.getBacktrackPathId()
        if (currentBacktrackPathId == null || !isBacktrackOn()) {
            return@onIO pointsLock.withLock { points.toMap() }
        }

        val locationSubsystem = getAppService<ILocationSubsystem>()
        val location = locationSubsystem.location
        val altitude = locationSubsystem.elevation
        pointsLock.withLock {
            if (!points.containsKey(currentBacktrackPathId)) {
                return@onIO points.toMap()
            }
            val point = PathPoint(
                -1,
                currentBacktrackPathId,
                location,
                altitude.value,
                Clock.System.now()
            )

            points.mapValues { entry ->
                if (entry.key == currentBacktrackPathId) {
                    listOf(point) + entry.value
                } else {
                    entry.value
                }
            }
        }
    }

    suspend fun update(
        paths: List<Path>,
        load: CoordinateBounds,
        unload: CoordinateBounds,
        reload: Boolean = false
    ) = onIO {
        updateLock.withLock {
            val backtrackId = pathService.getBacktrackPathId()
            val shouldLoad = ShouldLoadPathSpecification(load, backtrackId)
            val shouldUnload = ShouldUnloadPathSpecification(unload, backtrackId)

            val visibleIds = paths.map { it.id }.toSet()
            val toUnload = mutableListOf<Long>()
            val toLoad = pointsLock.withLock {
                val ids = mutableListOf<Long>()
                for (path in paths) {
                    val isLoaded = points.containsKey(path.id)
                    val unloadPath = !reload && isLoaded && shouldUnload.isSatisfiedBy(path)
                    if (unloadPath) {
                        toUnload.add(path.id)
                    }

                    val loadPath = reload || !isLoaded
                    if (loadPath && shouldLoad.isSatisfiedBy(path)) {
                        ids.add(path.id)
                    }
                }
                ids
            }

            val loaded =
                pathService.getWaypoints(toLoad).mapValues { it.value.sortedByDescending { it.id } }
            pointsLock.withLock {
                points.keys.retainAll(visibleIds)
                toUnload.forEach { points.remove(it) }
                points.putAll(loaded)
            }
        }
    }

}
