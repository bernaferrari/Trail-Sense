package com.kylecorry.trail_sense.shared.sensors.gps

import com.kylecorry.andromeda.core.time.TimeProvider
import com.kylecorry.andromeda.sense.location.GpsSensor
import java.time.Duration

fun GpsSensor.durationSince(elapsedNanos: Long): Duration {
    return Duration.ofNanos(eventTimeElapsedNanos - elapsedNanos)
}

fun GpsSensor.durationSince(other: GpsSensor): Duration {
    return durationSince(other.eventTimeElapsedNanos)
}

fun GpsSensor.age(timeProvider: TimeProvider): Duration {
    return Duration.ofMillis(timeProvider.elapsedRealtime()).minusNanos(eventTimeElapsedNanos)
}
