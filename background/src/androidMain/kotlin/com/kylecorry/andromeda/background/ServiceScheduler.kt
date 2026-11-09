package com.kylecorry.andromeda.background

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** Starts and stops an Android service; foreground services must meet platform requirements. */
fun serviceScheduler(context: Context, intent: Intent, foreground: Boolean = false): IAlwaysOnTaskScheduler =
    object : IAlwaysOnTaskScheduler {
        override fun start() {
            if (foreground) ContextCompat.startForegroundService(context, intent) else context.startService(intent)
        }
        override fun cancel() { context.stopService(intent) }
    }
