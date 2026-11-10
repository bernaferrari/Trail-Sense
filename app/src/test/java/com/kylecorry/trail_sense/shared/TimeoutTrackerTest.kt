package com.kylecorry.trail_sense.shared

import com.kylecorry.andromeda.core.time.TimeProvider
import com.kylecorry.andromeda.core.time.TimeoutTracker
import kotlin.time.Duration.Companion.milliseconds
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TimeoutTrackerTest {
    private class FakeTimeProvider(var elapsedMillis: Long = 0L) : TimeProvider {
        override fun elapsedRealtime() = elapsedMillis
        override fun currentTimeMillis() = elapsedMillis
    }

    @Test
    fun startsOnFirstTimeoutCheckByDefault() {
        val timeProvider = FakeTimeProvider()
        val tracker = TimeoutTracker(timeProvider)

        assertFalse(tracker.isTimedOut(1_000.milliseconds))
        timeProvider.elapsedMillis = 999L
        assertFalse(tracker.isTimedOut(1_000.milliseconds))
        timeProvider.elapsedMillis = 1_000L
        assertTrue(tracker.isTimedOut(1_000.milliseconds))
    }

    @Test
    fun doesNotStartWhenStartIfNotStartedIsFalse() {
        val timeProvider = FakeTimeProvider()
        val tracker = TimeoutTracker(timeProvider)

        assertFalse(tracker.isTimedOut(0.milliseconds, startIfNotStarted = false))
        timeProvider.elapsedMillis = 1_000L
        assertFalse(tracker.isTimedOut(1_000.milliseconds, startIfNotStarted = false))

        tracker.start()
        timeProvider.elapsedMillis = 2_000L
        assertTrue(tracker.isTimedOut(1_000.milliseconds, startIfNotStarted = false))
    }

    @Test
    fun explicitStartUsesTheStartTimeAndRestartsTheTimeout() {
        val timeProvider = FakeTimeProvider(1_000L)
        val tracker = TimeoutTracker(timeProvider)

        tracker.start()
        timeProvider.elapsedMillis = 1_999L
        assertFalse(tracker.isTimedOut(1_000.milliseconds))
        timeProvider.elapsedMillis = 2_000L
        assertTrue(tracker.isTimedOut(1_000.milliseconds))

        timeProvider.elapsedMillis = 3_000L
        tracker.start()
        assertFalse(tracker.isTimedOut(1_000.milliseconds))
        timeProvider.elapsedMillis = 4_000L
        assertTrue(tracker.isTimedOut(1_000.milliseconds))
    }

    @Test
    fun resetStopsTrackingUntilTheNextCheckStartsItAgain() {
        val timeProvider = FakeTimeProvider()
        val tracker = TimeoutTracker(timeProvider)

        tracker.start()
        timeProvider.elapsedMillis = 2_000L
        tracker.reset()
        assertFalse(tracker.isTimedOut(1_000.milliseconds, startIfNotStarted = false))

        assertFalse(tracker.isTimedOut(1_000.milliseconds))
        timeProvider.elapsedMillis = 3_000L
        assertTrue(tracker.isTimedOut(1_000.milliseconds))
    }
}
