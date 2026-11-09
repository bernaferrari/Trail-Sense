package com.kylecorry.andromeda.background

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.core.system.Wakelocks
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.preferences.preferences
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

abstract class DailyWorker(
    context: Context,
    params: WorkerParameters,
    private val tolerance: Duration = 30.minutes,
    private val wakelockDuration: Duration? = null,
    private val getPreferences: (Context) -> Preferences = { preferences(it) }
) : CoroutineWorker(context, params) {

    private val lock = Any()

    override suspend fun doWork(): Result {

        val wakelock = if (wakelockDuration != null) {
            val wakelockTag = "$uniqueId::interval::wakelock"
            tryOrDefault(null) {
                Wakelocks.get(applicationContext, wakelockTag)
                    ?.also { it.acquire(wakelockDuration.inWholeMilliseconds) }
            }
        } else {
            null
        }

        try {
            val context = applicationContext

            val jobState = synchronized(lock) {
                val zone = TimeZone.currentSystemDefault()
                val now = Clock.System.now()
                val today = now.toLocalDateTime(zone).date
                val cache = getPreferences(context)
                val lastRun = cache.getLocalDate(getLastRunKey(context))
                val shouldSend = isEnabled(context) && lastRun != today

                val scheduledTime = getScheduledTime(context)
                val sendTime = today.atTime(scheduledTime).toInstant(zone)
                val tomorrowSendTime =
                    today.plus(DatePeriod(days = 1)).atTime(scheduledTime).toInstant(zone)

                val sendWindowStart = sendTime - tolerance
                val sendWindowEnd = sendTime + tolerance

                val inWindow = now > sendWindowStart && now < sendWindowEnd
                val isTooEarly = now < sendWindowStart
                val isAfterWindow = now > sendWindowEnd

                if (inWindow && shouldSend) {
                    Log.d(
                        javaClass.simpleName,
                        "Received a broadcast and executed"
                    )
                    cache.putLocalDate(getLastRunKey(context), today)
                    return@synchronized true to tomorrowSendTime
                }

                if (isTooEarly) {
                    Log.d(
                        javaClass.simpleName,
                        "Received a broadcast too early"
                    )
                    return@synchronized false to sendTime
                }

                if (isAfterWindow || (inWindow && !shouldSend)) {
                    Log.d(
                        javaClass.simpleName,
                        "Received a broadcast too late, it already ran today, or it is not enabled"
                    )
                }

                return@synchronized false to tomorrowSendTime
            }

            try {
                if (jobState.first) {
                    val foregroundInfo = getForegroundInfo(context)
                    if (foregroundInfo != null) {
                        setForeground(foregroundInfo)
                    }
                    execute(context)
                }
            } finally {
                setAlarm(context, jobState.second)
            }
        } finally {
            wakelock?.release()
        }

        return Result.success()
    }

    protected open fun isEnabled(context: Context): Boolean {
        return true
    }

    protected abstract fun getScheduledTime(context: Context): LocalTime

    protected open fun getLastRunKey(context: Context): String {
        return "pref_andromeda_daily_worker_last_run_date_$uniqueId"
    }

    protected abstract suspend fun execute(context: Context)
    protected abstract val uniqueId: Int

    protected open fun getForegroundInfo(context: Context): ForegroundInfo? {
        return null
    }

    protected open fun getScheduler(context: Context): OneTimeTaskScheduler {
        return OneTimeTaskSchedulerFactory(context).deferrable(this::class.java, uniqueId)
    }

    private fun setAlarm(context: Context, time: Instant) {
        val scheduler = getScheduler(context)
        scheduler.cancel()
        scheduler.once(time)
        Log.d(
            javaClass.simpleName,
            "Scheduled the next run for $time"
        )
    }
}