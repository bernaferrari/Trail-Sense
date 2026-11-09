package com.kylecorry.andromeda.background.services

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import com.kylecorry.andromeda.background.BroadcastWorker
import com.kylecorry.andromeda.background.TaskSchedulerFactory
import com.kylecorry.andromeda.core.system.broadcasts
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.luna.time.ITimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * A base service for running a background task on an interval.
 * This is only recommended for foreground services, since background services may be killed by the OS more often.
 * @param workerThreshold Determines when to switch from to a scheduled worker. Scheduled jobs are inexact.
 * @param wakelockDuration The wakelock duration per update when not holding a continuous wakelock
 * @param useOneTimeWorkers Use one time workers instead of a periodic worker when over the worker threshold. Using one time workers can lead to slightly more on time intervals and it also allows variable durations.
 */
abstract class IntervalService(
    private val workerThreshold: Duration = 15.minutes,
    private val wakelockDuration: Duration? = null,
    private val useOneTimeWorkers: Boolean = false,
) : AndromedaService() {
    abstract val period: Duration
    protected open val holdWakelockWhenBelowThreshold: Boolean = true

    protected open fun getNonWorkerTimer(action: suspend () -> Unit): ITimer {
        return CoroutineTimer { action() }
    }

    private var workerBroadcasts: Job? = null

    private val periodicWorker by lazy {
        TaskSchedulerFactory(this).interval(
            BroadcastWorker::class.java,
            uniqueId,
            Bundle().apply {
                putString("action", action)
            }
        )
    }

    private val oneTimeWorker by lazy {
        TaskSchedulerFactory(this).once(
            BroadcastWorker::class.java,
            uniqueId,
            Bundle().apply {
                putString("action", action)
            }
        )
    }

    private var isWakelockManaged = false
    private var isEnabled = false

    private val timer by lazy {
        getNonWorkerTimer {
            try {
                if (isWakelockManaged) {
                    acquireWakelock(tag, wakelockDuration)
                }
                doWork()
            } finally {
                if (isWakelockManaged) {
                    releaseWakelock()
                }
            }
        }
    }

    abstract suspend fun doWork()

    open val action: String
        get() = "$tag.INTERVAL_ACTION"

    abstract val uniqueId: Int

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        isEnabled = true
        if (period < workerThreshold) {
            if (holdWakelockWhenBelowThreshold) {
                acquireWakelock(tag)
            }
            isWakelockManaged = !holdWakelockWhenBelowThreshold
            timer.interval(period.inWholeMilliseconds)
        } else {
            isWakelockManaged = true
            workerBroadcasts?.cancel()
            workerBroadcasts = broadcasts(applicationContext, IntentFilter(action), includeSticky = false)
                .onEach { onWorkerBroadcast() }
                .launchIn(CoroutineScope(Dispatchers.Main.immediate))
            if (useOneTimeWorkers) {
                oneTimeWorker.start()
            } else {
                periodicWorker.interval(period)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        isEnabled = false
        releaseWakelock()
        workerBroadcasts?.cancel()
        workerBroadcasts = null
        timer.stop()
        if (useOneTimeWorkers) {
            oneTimeWorker.cancel()
        } else {
            periodicWorker.cancel()
        }
        super.onDestroy()
    }

    private fun onWorkerBroadcast() {
        if (!isEnabled) {
            return
        }
        timer.once(0L)
        if (useOneTimeWorkers) {
            oneTimeWorker.cancel()
            oneTimeWorker.once(period)
        }
    }
}
