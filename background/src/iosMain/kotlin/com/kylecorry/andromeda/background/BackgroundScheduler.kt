package com.kylecorry.andromeda.background

import kotlinx.cinterop.ExperimentalForeignApi
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSince1970
import kotlin.time.Instant
import kotlin.time.Duration.Companion.seconds

/** The app must register this identifier with BGTaskScheduler and list it in Info.plist. */
@OptIn(ExperimentalForeignApi::class)
fun backgroundScheduler(identifier: String): BackgroundScheduler = object : BackgroundScheduler {
    override fun schedule(earliest: Instant) {
        BGTaskScheduler.sharedScheduler.cancelTaskRequestWithIdentifier(identifier)
        val request = BGAppRefreshTaskRequest(identifier = identifier)
        request.earliestBeginDate = NSDate.dateWithTimeIntervalSince1970(earliest.toEpochMilliseconds() / 1000.0)
        check(BGTaskScheduler.sharedScheduler.submitTaskRequest(request, null)) { "The system rejected the background task" }
    }
    override fun cancel() { BGTaskScheduler.sharedScheduler.cancelTaskRequestWithIdentifier(identifier) }
}

private val periodicScheduleLock = platform.Foundation.NSLock()

/** The app must register this identifier at launch and call onTaskStarted when it is delivered. */
@OptIn(ExperimentalForeignApi::class)
fun periodicBackgroundScheduler(identifier: String): PeriodicBackgroundScheduler = object : PeriodicBackgroundScheduler {
    private val key = "com.kylecorry.andromeda.background.period.$identifier"
    private val defaults = platform.Foundation.NSUserDefaults.standardUserDefaults
    private val oneShot = backgroundScheduler(identifier)

    override fun interval(period: kotlin.time.Duration, initialDelay: kotlin.time.Duration) {
        requirePeriodicSchedule(period, initialDelay)
        periodicScheduleLock.lock()
        try {
            oneShot.once(initialDelay)
            defaults.setDouble(period.toDouble(kotlin.time.DurationUnit.SECONDS), forKey = key)
        } finally { periodicScheduleLock.unlock() }
    }

    override fun onTaskStarted() {
        periodicScheduleLock.lock()
        try {
            val seconds = defaults.doubleForKey(key)
            if (seconds >= 900.0 && seconds.isFinite()) {
                oneShot.once(seconds.seconds)
            }
        } finally { periodicScheduleLock.unlock() }
    }

    override fun cancel() {
        periodicScheduleLock.lock()
        try {
            defaults.removeObjectForKey(key)
            oneShot.cancel()
        } finally { periodicScheduleLock.unlock() }
    }
}
