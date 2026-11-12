package com.kylecorry.trail_sense.tools.tides.domain

import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.tides.domain.waterlevel.TideHarmonicsSource
import com.kylecorry.trail_sense.tools.tides.infrastructure.model.TideModelResult
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Instant

class TideServicePhaseTest {

    private val zone = TimeZone.UTC
    private val service = TideService(object : TideHarmonicsSource {
        override suspend fun getHarmonics(location: Coordinate): TideModelResult? = null
        override val currentLocation: Coordinate = Coordinate.zero
    })

    @Test
    fun phaseGoesAllTheWayAroundBetweenTides() = runTest {
        val table = TideTable(
            0, listOf(
                Tide.low(time(10, 8, 0), 0f),
                Tide.high(time(10, 14, 0), 1f)
            )
        )

        assertEquals(180f, service.getPhase(table, time(10, 8, 0), zone)!!, 5f)
        assertEquals(270f, service.getPhase(table, time(10, 11, 0), zone)!!, 5f)
        // Rising runs from low (180) up to high (360), not stopping at 270
        assertEquals(359f, service.getPhase(table, time(10, 13, 58), zone)!!, 5f)
        assertEquals(0f, service.getPhase(table, time(10, 14, 0), zone)!!, 5f)
        assertEquals(90f, service.getPhase(table, time(10, 17, 0), zone)!!, 5f)
    }

    @Test
    fun phaseBetweenMidnightAndTheFirstTideUsesThePreviousDaysTide() = runTest {
        val table = TideTable(
            0, listOf(
                Tide.high(time(10, 14, 0), 2.71f),
                Tide.low(time(10, 19, 27), 0.39f),
                Tide.high(time(11, 14, 56), 2.54f),
            )
        )

        val phase = service.getPhase(table, time(11, 0, 30), zone)

        assertNotNull(phase)
    }

    private fun time(day: Int, hour: Int, minute: Int): Instant {
        return LocalDateTime(2022, 1, day, hour, minute).toInstant(zone)
    }
}
