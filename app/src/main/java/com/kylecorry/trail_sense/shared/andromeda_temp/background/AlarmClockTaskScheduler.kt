package com.kylecorry.trail_sense.shared.andromeda_temp.background

import kotlin.time.Clock

import android.app.PendingIntent
import android.content.Context
import kotlin.time.Duration
import kotlin.time.Instant

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
            Clock.System.now().plus(delay),
            task(),
            viewTask
        )
    }

    override fun once(time: Instant) {
        once((time - Clock.System.now()))
    }

    override fun start() {
        once()
    }

    override fun cancel() {
        Alarms.cancel(context, task())
    }
}