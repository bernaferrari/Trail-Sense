package com.kylecorry.trail_sense.shared.andromeda_temp.background

import android.content.Context
import android.os.Bundle
import androidx.work.ListenableWorker
import com.kylecorry.andromeda.core.annotations.ExperimentalUsage

@ExperimentalUsage("This class may change significantly to accommodate generic tasks")
class PeriodicTaskSchedulerFactory(private val context: Context) {
    fun deferrable(
        task: Class<out ListenableWorker>,
        uniqueId: Int,
        data: Bundle? = null
    ): IPeriodicTaskScheduler {
        return WorkTaskScheduler(
            context.applicationContext,
            task,
            WorkTaskScheduler.createStringId(context, uniqueId),
            input = data
        )
    }
}