package com.kylecorry.trail_sense.shared.sensors.gps

import kotlin.time.Duration.Companion.seconds
import com.kylecorry.andromeda.core.time.TimeProvider
import kotlin.time.Duration
import kotlin.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GPSFixTimeExtensionsTest {

    private fun reading(seconds: Long, elapsedSeconds: Long) = ModularGPSData(
        eventTime = (Instant.fromEpochMilliseconds(0) + seconds.seconds),
        eventTimeElapsedNanos = elapsedSeconds * 1_000_000_000
    )

    @Test
    fun durationSinceUsesElapsedRealtime() {
        assertEquals(2.seconds, reading(50, 3).durationSince(reading(100, 1)))
        assertEquals((-2).seconds, reading(100, 1).durationSince(reading(50, 3)))
        assertEquals(4.seconds, reading(0, 3).durationSince(-1_000_000_000))
    }

    @Test
    fun ageUsesElapsedRealtime() {
        val timeProvider = object : TimeProvider {
            override fun elapsedRealtime() = 10_000L
            override fun currentTimeMillis() = 500_000L
        }
        assertEquals(7.seconds, reading(1000, 3).age(timeProvider))
    }
}
