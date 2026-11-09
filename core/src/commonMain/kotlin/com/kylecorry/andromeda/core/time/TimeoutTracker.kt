package com.kylecorry.andromeda.core.time

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** Tracks elapsed monotonic time since [start], for timeouts that must ignore wall-clock changes. */
public class TimeoutTracker(private val timeProvider: TimeProvider = SystemTimeProvider()) {

    private var startMillis: Long? = null

    public val isStarted: Boolean
        get() = startMillis != null

    public fun start() {
        startMillis = timeProvider.elapsedRealtime()
    }

    public fun reset() {
        startMillis = null
    }

    /** The time since [start], or zero when not started. */
    public fun elapsed(): Duration {
        return startMillis?.let { (timeProvider.elapsedRealtime() - it).milliseconds } ?: Duration.ZERO
    }

    /**
     * Returns true once at least [timeout] has elapsed since [start].
     * @param startIfNotStarted starts tracking (and returns false) when not yet started
     */
    public fun isTimedOut(timeout: Duration, startIfNotStarted: Boolean = true): Boolean {
        if (startMillis == null) {
            if (!startIfNotStarted) return false
            start()
        }
        return elapsed() >= timeout
    }
}
