package com.kylecorry.andromeda.background

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Instant

/** Schedules a WorkManager worker; the worker type must be constructible by WorkManager. */
fun backgroundScheduler(context: Context, identifier: String, worker: Class<out ListenableWorker>): BackgroundScheduler =
    object : BackgroundScheduler {
        override fun schedule(earliest: Instant) {
            val delay = (earliest - Clock.System.now()).inWholeMilliseconds.coerceAtLeast(0)
            val request = OneTimeWorkRequest.Builder(worker).setInitialDelay(delay, TimeUnit.MILLISECONDS).build()
            WorkManager.getInstance(context).enqueueUniqueWork(identifier, ExistingWorkPolicy.REPLACE, request)
        }
        override fun cancel() { WorkManager.getInstance(context).cancelUniqueWork(identifier) }
    }
