package com.kylecorry.trail_sense.tools.paths.domain

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class BacktrackRecorderTest {
    @Test
    fun recordsOnlyWhenEnabledAndFarEnough() = runBlocking {
        val paths = mock<IPathService>()
        val point = PathPoint(0, 0, Coordinate(0.0, 0.0))
        val recorder = BacktrackRecorder(paths)
        assertFalse(recorder.record(point, false, null))
        verify(paths, never()).addBacktrackPoint(point)

        whenever(paths.getBacktrackPathId()).thenReturn(1L)
        whenever(paths.getLastWaypoint(1L)).thenReturn(point)
        assertFalse(recorder.record(point, true, Distance.meters(10f)))
        verify(paths, never()).addBacktrackPoint(point)

        assertTrue(recorder.record(point, true, null))
        verify(paths).addBacktrackPoint(point)
    }
}
