package com.kylecorry.trail_sense.tools.tides.domain

import kotlinx.datetime.TimeZone
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.trail_sense.tools.tides.domain.selection.DefaultTideSelectionStrategy
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.time.Instant

internal class DefaultTideSelectionStrategyTest {

    @Test
    fun getTide() = runBlocking {
        val strategy = DefaultTideSelectionStrategy()
        val tides = listOf(
            TideTable(2, listOf(Tide.high(Instant.fromEpochMilliseconds(100), timeZone = TimeZone.UTC)), null, null),
            TideTable(1, listOf(Tide.high(Instant.fromEpochMilliseconds(10), timeZone = TimeZone.UTC)), null, null)
        )
        Assertions.assertEquals(tides[0], strategy.getTide(tides))
    }

    @Test
    fun getTideEmptyList() = runBlocking {
        val strategy = DefaultTideSelectionStrategy()
        val tides = emptyList<TideTable>()
        Assertions.assertNull(strategy.getTide(tides))
    }
}
