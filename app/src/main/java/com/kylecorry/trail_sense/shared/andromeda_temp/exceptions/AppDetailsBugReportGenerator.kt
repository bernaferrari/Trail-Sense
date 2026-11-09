package com.kylecorry.trail_sense.shared.andromeda_temp.exceptions

import android.content.Context
import com.kylecorry.trail_sense.shared.andromeda_temp.core.system.Package

class AppDetailsBugReportGenerator(private val appName: String) : IBugReportGenerator {
    override fun generate(context: Context, throwable: Throwable): String {
        val appVersion = Package.getVersionName(context)
        val isDebug = com.kylecorry.trail_sense.BuildConfig.DEBUG
        return "$appName $appVersion${if (isDebug) " (Debug)" else ""}"
    }
}