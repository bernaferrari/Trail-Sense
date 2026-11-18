package com.kylecorry.trail_sense.tools.paths.infrastructure.commands

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.commands.generic.CoroutineValueCommand
import com.kylecorry.trail_sense.shared.coroutines.ioDispatcher
import com.kylecorry.trail_sense.shared.dem.ElevationModel
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathMetadata
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.IPathPreferences
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class CreatePathCommand(
    private val pathService: IPathService,
    private val prefs: IPathPreferences,
    private val name: String?,
    private val knownElevations: Map<Coordinate, Float> = emptyMap(),
    private val shouldSkipElevations: () -> Boolean = { false },
    private val parentId: Long? = null,
    private val elevationModel: ElevationModel = getAppService()
) : CoroutineValueCommand<List<Coordinate>, Long> {
    override suspend fun execute(value: List<Coordinate>) = withContext(ioDispatcher) {
        val waypoints = value.filter { it.latitude.isFinite() && it.longitude.isFinite() }.map {
            val elevation = knownElevations[it]
                ?: if (shouldSkipElevations()) null else elevationModel.getElevation(it).takeIf { dem -> dem.hasData }?.elevation
            PathPoint(0, 0, it, elevation)
        }
        require(waypoints.isNotEmpty())
        val path = Path(0, name, prefs.defaultPathStyle, PathMetadata.empty, parentId = parentId)
        withContext(NonCancellable) {
            val id = pathService.addPath(path)
            try {
                pathService.addWaypointsToPath(waypoints, id)
            } catch (failure: Exception) {
                try {
                    pathService.deletePath(path.copy(id = id))
                } catch (cleanupFailure: Exception) {
                    failure.addSuppressed(cleanupFailure)
                }
                throw failure
            }
            id
        }
    }
}
