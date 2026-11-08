package com.kylecorry.andromeda.background

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/** Schedules work while the supplied coroutine scope is alive. */
class CoroutineTaskScheduler(private val scope: CoroutineScope, private val task: suspend () -> Unit) : ITaskScheduler {
    private var job: Job? = null

    fun once(delay: Duration = Duration.ZERO) {
        require(delay >= Duration.ZERO && delay.isFinite())
        cancel()
        job = scope.launch(start = CoroutineStart.LAZY) {
            delay(delay)
            task()
        }
        job?.start()
    }

    fun once(time: Instant) = once((time - Clock.System.now()).coerceAtLeast(Duration.ZERO))

    fun interval(period: Duration, initialDelay: Duration = Duration.ZERO) {
        require(period > Duration.ZERO && period.isFinite() && initialDelay >= Duration.ZERO && initialDelay.isFinite())
        cancel()
        job = scope.launch(start = CoroutineStart.LAZY) {
            delay(initialDelay)
            while (isActive) {
                task()
                delay(period)
            }
        }
        job?.start()
    }

    override fun cancel() {
        job?.cancel()
        job = null
    }
}
