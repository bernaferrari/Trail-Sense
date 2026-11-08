package com.kylecorry.andromeda.background

import android.app.PendingIntent
import android.content.Context
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import java.time.Instant as JavaInstant

/**
 * Schedules an alarm clock
 * @param context the context
 * @param viewTask the task to run when the user wants to view the alarm
 * @param task the task to run
 */
class AlarmClockTaskScheduler(
    private val context: Context,
    private val viewTask: PendingIntent,
    private val task: () -> PendingIntent
) :
    IOneTimeTaskScheduler {


    override fun once(delay: Duration) {
        Alarms.setAlarmClock(
            context,
            JavaInstant.now().plus(delay.toJavaDuration()),
            task(),
            viewTask
        )
    }

    override fun cancel() {
        Alarms.cancel(context, task())
    }
}