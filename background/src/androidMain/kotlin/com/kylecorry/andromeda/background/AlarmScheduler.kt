package com.kylecorry.andromeda.background

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.time.Clock

/** Android alarm delivery policy. Exact alarms may require system permission. */
public sealed interface AlarmDelivery {
    public data class Exact(val allowWhileIdle: Boolean = true, val fallbackToInexact: Boolean = true) : AlarmDelivery
    public data class Inexact(val allowWhileIdle: Boolean = false) : AlarmDelivery
    public data class Window(val duration: Duration, val centered: Boolean = false) : AlarmDelivery
    public data class AlarmClock(val showIntent: PendingIntent) : AlarmDelivery
}

/** Schedules a pending intent using Android's alarm service. The host supplies immutable intents and permissions. */
public fun alarmScheduler(context: Context, action: PendingIntent, delivery: AlarmDelivery = AlarmDelivery.Exact()): BackgroundScheduler =
    object : BackgroundScheduler {
        private val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        @SuppressLint("MissingPermission")
        override fun schedule(earliest: Instant) {
            val time = earliest.toEpochMilliseconds()
            fun inexact(whileIdle: Boolean) {
                if (whileIdle) alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, action)
                else alarms.set(AlarmManager.RTC_WAKEUP, time, action)
            }
            when (delivery) {
                is AlarmDelivery.Exact -> {
                    val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
                    if (!allowed) {
                        check(delivery.fallbackToInexact) { "Exact alarm permission is not granted" }
                        inexact(delivery.allowWhileIdle)
                    } else if (delivery.allowWhileIdle) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, action)
                    else alarms.setExact(AlarmManager.RTC_WAKEUP, time, action)
                }
                is AlarmDelivery.Inexact -> inexact(delivery.allowWhileIdle)
                is AlarmDelivery.Window -> {
                    require(delivery.duration.isFinite() && delivery.duration > Duration.ZERO)
                    val window = delivery.duration.inWholeMilliseconds
                    require(window > 0)
                    val start = if (delivery.centered) time - window / 2 else time
                    alarms.setWindow(AlarmManager.RTC_WAKEUP, start, window, action)
                }
                is AlarmDelivery.AlarmClock -> alarms.setAlarmClock(AlarmManager.AlarmClockInfo(time, delivery.showIntent), action)
            }
        }
        override fun cancel() = alarms.cancel(action)
    }

/** Schedules an inexact repeating Android alarm. The OS may defer or batch delivery. */
public fun repeatingAlarmScheduler(context: Context, action: PendingIntent): PeriodicBackgroundScheduler =
    object : PeriodicBackgroundScheduler {
        private val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        override fun interval(period: Duration, initialDelay: Duration) {
            require(period.isFinite() && period.inWholeMilliseconds > 0)
            require(initialDelay.isFinite() && initialDelay >= Duration.ZERO)
            alarms.setRepeating(AlarmManager.RTC_WAKEUP, (Clock.System.now() + initialDelay).toEpochMilliseconds(), period.inWholeMilliseconds, action)
        }
        override fun onTaskStarted() = Unit
        override fun cancel() = alarms.cancel(action)
    }
