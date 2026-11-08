package com.kylecorry.andromeda.core.time

import kotlin.time.Clock

/** System wall time and platform monotonic time, both expressed in milliseconds. */
class SystemTimeProvider : TimeProvider {
    override fun elapsedRealtime(): Long = monotonicNanos() / 1_000_000
    override fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
}
