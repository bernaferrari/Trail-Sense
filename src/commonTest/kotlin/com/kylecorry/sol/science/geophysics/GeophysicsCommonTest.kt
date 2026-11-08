package com.kylecorry.sol.science.geophysics

import com.kylecorry.sol.units.Coordinate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlin.time.Duration.Companion.seconds
import com.kylecorry.sol.science.physics.Physics

class GeophysicsCommonTest {
    @Test
    fun fallHeightUsesKotlinDuration() {
        assertEquals(19.62f, Physics.fallHeight(2.seconds).meters().value, 0.0001f)
    }

    @Test
    fun magneticFieldAtKnownLocationAndDate() {
        val time = Instant.parse("2025-01-01T00:00:00Z").toEpochMilliseconds()
        val location = Coordinate(80.0, 0.0)
        assertEquals(1.28f, Geophysics.getGeomagneticDeclination(location, 0f, time), 0.01f)
        assertEquals(83.21f, Geophysics.getGeomagneticInclination(location, 0f, time), 0.01f)
    }
}
