package com.kylecorry.andromeda.background

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class CoroutineTaskSchedulerTest {
    @Test
    fun supportsOneTimeAndRepeatingContracts() = runBlocking {
        var count = 0
        var complete = CompletableDeferred<Unit>()
        val scheduler = CoroutineTaskScheduler(this) {
            count++
            if (count == 1 || count == 3) complete.complete(Unit)
        }
        val once: OneTimeTaskScheduler = scheduler
        once.once(Instant.fromEpochSeconds(0))
        withTimeout(1000) { complete.await() }
        assertEquals(1, count)
        complete = CompletableDeferred()
        val periodic: PeriodicTaskScheduler = scheduler
        periodic.interval(10.milliseconds)
        withTimeout(1000) { complete.await() }
        periodic.cancel()
        assertEquals(3, count)
        assertFailsWith<IllegalArgumentException> { periodic.interval(Duration.ZERO) }
        Unit
    }
}
