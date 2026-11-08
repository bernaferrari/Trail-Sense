package com.kylecorry.andromeda.background

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration

/** Schedules work while the supplied coroutine scope is alive. */
class CoroutineTaskScheduler(private val scope: CoroutineScope, private val task: suspend () -> Unit) : IOneTimeTaskScheduler, IPeriodicTaskScheduler {
    private var job: Job? = null

    override fun once(delay: Duration) {
        require(delay >= Duration.ZERO && delay.isFinite())
        cancel()
        job = scope.launch(start = CoroutineStart.LAZY) {
            delay(delay)
            task()
        }
        job?.start()
    }

    override fun interval(period: Duration, initialDelay: Duration) {
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
