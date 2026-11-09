package com.kylecorry.trail_sense.tools.turn_back.ui

import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.hours
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Instant

class TurnBackViewModelTest {

    @Test
    fun turnsBackAfterFortyPercentOfTheTimeLeft() {
        val now = Instant.parse("2026-01-01T10:00:00Z")
        val returnTime = now.plus(10.hours)
        assertEquals(now.plus(4.hours), TurnBackViewModel.turnBackTimeFor(now, returnTime))
    }

    @Test
    fun aPastReturnTimeMeansTurnBackNow() {
        val now = Instant.parse("2026-01-01T10:00:00Z")
        assertEquals(now, TurnBackViewModel.turnBackTimeFor(now, (now - 60.seconds)))
    }
}
