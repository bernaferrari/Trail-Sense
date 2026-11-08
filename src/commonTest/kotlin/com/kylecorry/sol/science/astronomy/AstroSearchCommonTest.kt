package com.kylecorry.sol.science.astronomy

import com.kylecorry.sol.math.Range
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class AstroSearchCommonTest {
    @Test
    fun locatesEventBoundariesAndPeak() {
        val start = Instant.parse("2025-01-01T00:00:00Z")
        val range = Range(start, start + 100.seconds)
        val eventStart = start + 20.seconds
        val eventEnd = start + 80.seconds
        val rise = AstroSearch.findStart(Range(start, start + 50.seconds), 1.milliseconds) { it >= eventStart }
        val set = AstroSearch.findEnd(Range(start + 50.seconds, range.end), 1.milliseconds) { it <= eventEnd }
        assertNotNull(rise)
        assertNotNull(set)
        assertTrue((rise - eventStart).absoluteValue <= 1.milliseconds)
        assertTrue((set - eventEnd).absoluteValue <= 1.milliseconds)
        val peak = AstroSearch.findPeak(range, 1.milliseconds) { time ->
            -(time - (start + 50.seconds)).absoluteValue.inWholeMilliseconds.toFloat()
        }
        assertTrue((peak - (start + 50.seconds)).absoluteValue <= 5.milliseconds)
    }
}
