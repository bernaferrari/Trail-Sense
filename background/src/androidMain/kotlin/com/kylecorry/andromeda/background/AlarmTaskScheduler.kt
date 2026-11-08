package com.kylecorry.andromeda.background

import android.app.PendingIntent
import android.content.Context
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration
import java.time.Instant as JavaInstant

/**
 * Uses the AlarmManager to scheduler work. Useful for work that needs to run at an exact time.
 * @param context the context
 * @param exact determines if this should be run at exactly the time specified. Defaults to true. Deferrable tasks should use the WorkTaskScheduler
 * @param allowWhileIdle determines if this should be run while the device is idle. Defaults to false.
 * @param inexactWindow the window of time to run the task in if exact is false. Defaults to 10 minutes, which is the minimum allowed by the system.
 * @param task the task to run - if you are just calling a broadcast receiver, it is recommend to use the AlarmBroadcastTaskScheduler
 */
class AlarmTaskScheduler(
    private val context: Context,
    private val exact: Boolean = true,
    private val allowWhileIdle: Boolean = false,
    private val inexactWindow: Duration = 10.minutes,
    private val isWindowCentered: Boolean = false,
    private val task: () -> PendingIntent
) :
    IOneTimeTaskScheduler {


    override fun once(delay: Duration) {
        Alarms.set(
            context,
            JavaInstant.now().plus(delay.toJavaDuration()),
            task(),
            exact,
            allowWhileIdle,
            inexactWindow.toJavaDuration(),
            isWindowCentered
        )
    }

    override fun cancel() {
        Alarms.cancel(context, task())
    }
}