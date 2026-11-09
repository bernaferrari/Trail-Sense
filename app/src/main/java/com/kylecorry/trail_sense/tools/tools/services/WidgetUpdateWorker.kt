package com.kylecorry.trail_sense.tools.tools.services

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

import android.content.Context
import android.os.SystemClock
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kylecorry.trail_sense.shared.andromeda_temp.background.IPeriodicTaskScheduler
import com.kylecorry.trail_sense.shared.andromeda_temp.background.PeriodicTaskSchedulerFactory
import com.kylecorry.trail_sense.shared.andromeda_temp.core.system.Wakelocks
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.trail_sense.shared.andromeda_temp.permissions.Permissions
import com.kylecorry.trail_sense.shared.andromeda_temp.widgets.Widgets
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.permissions.canGetLocationCustom
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem.SensorRefreshPolicy
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlin.time.Duration

class WidgetUpdateWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val wakelock = Wakelocks.get(applicationContext, TAG)
        wakelock?.acquire(60.seconds.inWholeMilliseconds)
        val start = SystemClock.elapsedRealtime()
        try {
            // Update stale location/elevation data if needed
            tryOrLog {
                if (Tools.hasAnyWidgetsOnHomeScreen(applicationContext) { it.usesLocation }) {
                    updateStaleSensorData()
                }
            }

            // Update all widgets
            Tools.triggerWidgetUpdate(applicationContext, null)
        } finally {
            wakelock?.release()
        }
        getAppService<Logger>().info(TAG, "Widgets updated in ${SystemClock.elapsedRealtime() - start}ms")
        return Result.success()
    }

    private suspend fun updateStaleSensorData() {
        val locationSubsystem = LocationSubsystem.getInstance(applicationContext)
        val sensorSubsystem = SensorSubsystem.getInstance(applicationContext)

        if (!Permissions.canGetLocationCustom(applicationContext)) {
            return
        }

        val isLocationStale = locationSubsystem.locationAge.inWholeMinutes > 30
        val isElevationStale = locationSubsystem.elevationAge.inWholeMinutes > 30

        if (isLocationStale || isElevationStale) {
            getAppService<Logger>().info(
                TAG,
                "Refreshing stale widget sensor data (location age: ${locationSubsystem.locationAge.inWholeMinutes}min, " +
                    "elevation age: ${locationSubsystem.elevationAge.inWholeMinutes}min)"
            )
        }

        if (isLocationStale && isElevationStale) {
            sensorSubsystem.getLocationAndElevation(SensorRefreshPolicy.Refresh)
        } else if (isLocationStale) {
            sensorSubsystem.getLocation(SensorRefreshPolicy.Refresh)
        } else if (isElevationStale) {
            sensorSubsystem.getElevation(SensorRefreshPolicy.Refresh)
        }
    }

    companion object {
        private const val TAG = "WidgetUpdateWorker"
        private const val UNIQUE_ID = 267389

        val FREQUENCY: Duration = 30.minutes

        fun start(context: Context) {
            scheduler(context).interval(FREQUENCY)
        }

        fun stop(context: Context) {
            scheduler(context).cancel()
        }

        private fun scheduler(context: Context): IPeriodicTaskScheduler {
            return PeriodicTaskSchedulerFactory(context).deferrable(
                WidgetUpdateWorker::class.java,
                UNIQUE_ID
            )
        }
    }
}
