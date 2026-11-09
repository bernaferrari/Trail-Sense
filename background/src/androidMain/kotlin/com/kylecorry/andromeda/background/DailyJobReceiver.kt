package com.kylecorry.andromeda.background

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import kotlin.time.toKotlinInstant
import android.content.Intent
import android.util.Log
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.preferences.preferences
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

abstract class DailyJobReceiver(
    private val tolerance: Duration = Duration.ofMinutes(30),
    private val getPreferences: (Context) -> Preferences = { preferences(it) }
) : BroadcastReceiver() {

    private val lock = Any()

    override fun onReceive(context: Context, intent: Intent?) {
        synchronized(lock) {
            val now = LocalDateTime.now()
            val cache = getPreferences(context)
            val lastRun = cache.getLocalDate(getLastRunKey(context))?.toJavaLocalDate()
            val shouldSend = isEnabled(context) && lastRun != now.toLocalDate()

            val sendTime = LocalDate.now().atTime(getScheduledTime(context))
            val tomorrowSendTime =
                LocalDate.now().plusDays(1).atTime(getScheduledTime(context))

            val sendWindowStart = sendTime - tolerance
            val sendWindowEnd = sendTime + tolerance

            val inWindow = now.isAfter(sendWindowStart) && now.isBefore(sendWindowEnd)
            val isTooEarly = now.isBefore(sendWindowStart)
            val isAfterWindow = now.isAfter(sendWindowEnd)

            if (inWindow && shouldSend) {
                Log.d(
                    "ScheduledJobReceiver",
                    "${this::class.java.simpleName} received a broadcast and executed"
                )
                cache.putLocalDate(getLastRunKey(context), now.toLocalDate().toKotlinLocalDate())
                execute(context)
                setAlarm(context, tomorrowSendTime)
            }

            if (isTooEarly) {
                Log.d(
                    "ScheduledJobReceiver",
                    "${this::class.java.simpleName} received a broadcast too early"
                )
                setAlarm(context, sendTime)
            }

            if (isAfterWindow || (inWindow && !shouldSend)) {
                Log.d(
                    "ScheduledJobReceiver",
                    "${this::class.java.simpleName} received a broadcast too late, it already ran today, or it is not enabled"
                )
                setAlarm(context, tomorrowSendTime)
            }
        }
    }

    protected abstract fun isEnabled(context: Context): Boolean
    protected abstract fun getScheduledTime(context: Context): LocalTime
    protected abstract fun getLastRunKey(context: Context): String
    protected abstract fun execute(context: Context)
    protected abstract val pendingIntentId: Int

    protected open fun getIntent(context: Context): Intent {
        return Intent(context, this::class.java)
    }

    protected open fun getScheduler(context: Context): IOneTimeTaskScheduler {
        return alarmScheduler(context,
            PendingIntent.getBroadcast(
                context,
                pendingIntentId,
                getIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ), AlarmDelivery.Exact(allowWhileIdle = false))
    }

    private fun setAlarm(context: Context, time: LocalDateTime) {
        val scheduler = getScheduler(context)
        scheduler.cancel()
        scheduler.once(time.atZone(java.time.ZoneId.systemDefault()).toInstant().toKotlinInstant())
        Log.d(
            "ScheduledJobReceiver",
            "${this::class.java.simpleName} schedule the next alarm for $time"
        )
    }
}
