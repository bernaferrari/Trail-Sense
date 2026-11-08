package com.kylecorry.andromeda.background

import kotlinx.cinterop.ExperimentalForeignApi
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSince1970
import kotlin.time.Instant

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
