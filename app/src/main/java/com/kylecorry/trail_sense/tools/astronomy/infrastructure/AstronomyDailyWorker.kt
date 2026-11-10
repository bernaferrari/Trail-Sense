package com.kylecorry.trail_sense.tools.astronomy.infrastructure

import kotlin.time.Duration.Companion.seconds
import android.content.Context
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.background.DailyWorker
import com.kylecorry.andromeda.background.OneTimeTaskScheduler
import com.kylecorry.andromeda.background.OneTimeTaskSchedulerFactory
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.commands.AstronomyAlertCommand
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toKotlinLocalTime

// TODO: Autogenerate last run key (pref_andromeda_daily_worker_last_run_date_UNIQUEID)
class AstronomyDailyWorker(context: Context, params: WorkerParameters) : DailyWorker(
    context,
    params,
    wakelockDuration = 15.seconds,
    getPreferences = { PreferencesSubsystem.getInstance(context).preferences },
) {

    override fun isEnabled(context: Context): Boolean {
        val prefs = UserPreferences(context)
        return prefs.astronomy.sendAstronomyAlerts
    }

    override fun getScheduledTime(context: Context): LocalTime {
        val prefs = UserPreferences(context)
        return prefs.astronomy.astronomyAlertTime.toKotlinLocalTime()
    }

    override suspend fun execute(context: Context) {
        AstronomyAlertCommand(context).execute()
    }

    override val uniqueId: Int = UNIQUE_ID


    companion object {

        const val UNIQUE_ID = 72394823

        private fun getScheduler(context: Context): OneTimeTaskScheduler {
            return OneTimeTaskSchedulerFactory(context).deferrable(
                AstronomyDailyWorker::class.java,
                UNIQUE_ID
            )
        }

        fun start(context: Context) {
            getScheduler(context).start()
        }

        fun stop(context: Context) {
            getScheduler(context).cancel()
        }
    }
}
