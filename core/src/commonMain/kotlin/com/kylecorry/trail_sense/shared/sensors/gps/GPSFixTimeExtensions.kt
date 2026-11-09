package com.kylecorry.trail_sense.shared.sensors.gps

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

import com.kylecorry.andromeda.core.time.TimeProvider
import com.kylecorry.andromeda.sense.location.GpsSensor
import kotlin.time.Duration

fun GpsSensor.durationSince(elapsedNanos: Long): Duration {
    return (eventTimeElapsedNanos - elapsedNanos).nanoseconds
}

fun GpsSensor.durationSince(other: GpsSensor): Duration {
    return durationSince(other.eventTimeElapsedNanos)
}

fun GpsSensor.age(timeProvider: TimeProvider): Duration {
    return timeProvider.elapsedRealtime().milliseconds - eventTimeElapsedNanos.nanoseconds
}
