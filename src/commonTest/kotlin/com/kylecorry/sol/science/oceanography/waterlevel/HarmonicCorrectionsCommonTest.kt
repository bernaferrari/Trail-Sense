package com.kylecorry.sol.science.oceanography.waterlevel

import com.kylecorry.sol.science.oceanography.TideConstituent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class HarmonicCorrectionsCommonTest {
    @Test
    fun matchesReferenceValues() {
        val epoch = Instant.parse("2030-03-15T00:00:00Z").epochSeconds
        val (factor, angle) = NodalCorrection.at(epoch).of(TideConstituent.M2)
        assertEquals(1.0061, factor, 0.003)
        assertEquals(2.108, angle, 0.5)
        val time = Instant.parse("2030-03-15T06:00:00Z").epochSeconds
        assertEquals(278.071, AstronomicalArgument.at(time.toDouble()).of(TideConstituent.M2)!!, 0.05)
    }
}
