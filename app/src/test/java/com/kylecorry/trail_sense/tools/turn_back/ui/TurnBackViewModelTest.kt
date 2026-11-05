package com.kylecorry.trail_sense.tools.turn_back.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class TurnBackViewModelTest {

    @Test
    fun turnsBackAfterFortyPercentOfTheTimeLeft() {
        val now = Instant.parse("2026-01-01T10:00:00Z")
        val returnTime = now.plus(Duration.ofHours(10))
        assertEquals(now.plus(Duration.ofHours(4)), TurnBackViewModel.turnBackTimeFor(now, returnTime))
    }

    @Test
    fun aPastReturnTimeMeansTurnBackNow() {
        val now = Instant.parse("2026-01-01T10:00:00Z")
        assertEquals(now, TurnBackViewModel.turnBackTimeFor(now, now.minusSeconds(60)))
    }
}
