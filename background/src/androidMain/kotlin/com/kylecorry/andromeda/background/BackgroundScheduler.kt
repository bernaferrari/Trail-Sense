package com.kylecorry.andromeda.background

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequest
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** Schedules a WorkManager worker; the worker type must be constructible by WorkManager. */
public fun backgroundScheduler(
    context: Context,
    identifier: String,
    worker: Class<out ListenableWorker>,
    input: Data = Data.EMPTY,
    constraints: Constraints = Constraints.NONE,
    expedited: Boolean = false,
): BackgroundScheduler =
    object : BackgroundScheduler {
        override fun schedule(earliest: Instant) {
            val delay = (earliest - Clock.System.now()).inWholeMilliseconds.coerceAtLeast(0)
            require(!expedited || delay == 0L) { "Expedited work cannot have an initial delay" }
            val request = OneTimeWorkRequest.Builder(worker)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(input)
                .setConstraints(constraints)
                .apply { if (expedited) setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST) }
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(identifier, ExistingWorkPolicy.REPLACE, request)
        }
        override fun cancel() { WorkManager.getInstance(context).cancelUniqueWork(identifier) }
    }

/** Schedules recurring WorkManager work; Android reschedules it automatically. */
public fun periodicBackgroundScheduler(
    context: Context,
    identifier: String,
    worker: Class<out ListenableWorker>,
    input: Data = Data.EMPTY,
    constraints: Constraints = Constraints.NONE,
    flex: Duration? = null,
): PeriodicBackgroundScheduler =
    object : PeriodicBackgroundScheduler {
        override fun interval(period: kotlin.time.Duration, initialDelay: kotlin.time.Duration) {
            requirePeriodicSchedule(period, initialDelay)
            val builder = if (flex == null) PeriodicWorkRequest.Builder(worker, period.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            else {
                require(flex.isFinite() && flex >= 5.minutes && flex <= period)
                PeriodicWorkRequest.Builder(worker, period.inWholeMilliseconds, TimeUnit.MILLISECONDS, flex.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            }
            val request = builder
                .setInitialDelay(initialDelay.inWholeMilliseconds, TimeUnit.MILLISECONDS)
                .setInputData(input)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(identifier, androidx.work.ExistingPeriodicWorkPolicy.UPDATE, request)
        }
        override fun onTaskStarted() = Unit
        override fun cancel() { WorkManager.getInstance(context).cancelUniqueWork(identifier) }
    }
