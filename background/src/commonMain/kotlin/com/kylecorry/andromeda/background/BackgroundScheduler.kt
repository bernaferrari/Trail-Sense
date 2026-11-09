package com.kylecorry.andromeda.background

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/** Requests platform-managed work no earlier than the specified time. */
public interface BackgroundScheduler : OneTimeTaskScheduler {
    public fun schedule(earliest: Instant)

    override fun once(delay: Duration) {
        require(delay >= Duration.ZERO && delay.isFinite())
        schedule(Clock.System.now() + delay)
    }

    override fun once(time: Instant): Unit = schedule(time)
}
