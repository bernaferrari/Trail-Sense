package com.kylecorry.trail_sense.tools.paths.domain

import com.kylecorry.sol.units.Distance

/** Applies the backtrack recording policy to a location supplied by the platform. */
class BacktrackRecorder(private val paths: IPathService) {
    suspend fun record(point: PathPoint, enabled: Boolean, minimumDistance: Distance?): Boolean {
        if (!enabled) return false
        val pathId = paths.getBacktrackPathId()
        val previous = if (pathId == null) null else paths.getLastWaypoint(pathId)
        if (!ShouldRecordBacktrackPoint(minimumDistance).isSatisfiedBy(previous, point)) return false
        paths.addBacktrackPoint(point)
        return true
    }
}
