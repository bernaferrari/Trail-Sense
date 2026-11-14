package com.kylecorry.andromeda.device

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class GeofencesTest {
    @Test fun detectsTransitionsAndIgnoresStaleAndExpiredReadings() = runBlocking {
        val center = Coordinate(0.0, 0.0)
        val outside = Coordinate(0.0, 0.01)
        val expiry = Instant.fromEpochSeconds(5)
        val fence = Geofence("home", center, 100f, expiry)
        fun location(coordinate: Coordinate, second: Long) =
            Reading(LocationReading(coordinate, null), Instant.fromEpochSeconds(second))
        val readings = listOf(location(outside, 1), location(center, 2), location(center, 3),
            location(outside, 2), location(outside, 4), location(center, 5))
        assertEquals(listOf(GeofenceTransitionType.Enter, GeofenceTransitionType.Exit),
            readings.asFlow().geofenceTransitions(listOf(fence)).toList().map { it.type })
    }
}
