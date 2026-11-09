package com.kylecorry.trail_sense.shared.andromeda_temp.background

import kotlin.time.Duration.Companion.minutes

import kotlin.time.Clock

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * Uses the AlarmManager and Broadcast Receivers to scheduler work. Useful for work that needs to run at an exact time.
 * @param context the context
 * @param receiver the receiver to call
 * @param uniqueId an ID used to identify the receiver
 * @param exact determines if this should be run at exactly the time specified. Defaults to true. Deferrable tasks should use the WorkTaskScheduler
 * @param allowWhileIdle determines if this should be run while the device is idle. Defaults to false.
 * @param intentExtras extras to add to the intent
 */
class AlarmBroadcastTaskScheduler(
    private val context: Context,
    private val receiver: Class<out BroadcastReceiver>,
    private val uniqueId: Int,
    private val exact: Boolean = true,
    private val allowWhileIdle: Boolean = false,
    private val inexactWindow: Duration = 10.minutes,
    private val isWindowCentered: Boolean = false,
    private val intentExtras: Bundle? = null
) :
    IOneTimeTaskScheduler, IPeriodicTaskScheduler {


    override fun once(delay: Duration) {
        cancel()
        Alarms.set(
            context,
            Clock.System.now().plus(delay),
            createPendingIntent(),
            exact,
            allowWhileIdle,
            inexactWindow,
            isWindowCentered
        )
    }

    override fun once(time: Instant) {
        once((time - Clock.System.now()))
    }

    override fun start() {
        once()
    }

    override fun interval(period: Duration, initialDelay: Duration) {
        cancel()
        Alarms.setRepeating(
            context,
            Clock.System.now().plus(initialDelay),
            period,
            createPendingIntent()
        )
    }

    override fun interval(period: Duration, start: Instant) {
        interval(period, (start - Clock.System.now()))
    }

    override fun cancel() {
        Alarms.cancel(context, createPendingIntent())
    }

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(context, receiver)
        if (intentExtras != null) {
            intent.putExtras(intentExtras)
        }

        return PendingIntent.getBroadcast(
            context,
            uniqueId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
