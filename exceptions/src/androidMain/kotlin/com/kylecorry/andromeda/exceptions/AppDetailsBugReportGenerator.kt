package com.kylecorry.andromeda.exceptions

import android.content.Context
import android.content.pm.ApplicationInfo
import com.kylecorry.andromeda.core.system.Package

/**
 * Reports the app name and version.
 * @param isDebug whether this is a debug build, defaults to the app's debuggable flag
 */
class AppDetailsBugReportGenerator(
    private val appName: String,
    private val isDebug: Boolean? = null
) : IBugReportGenerator {
    override fun generate(context: Context, throwable: Throwable): String {
        val appVersion = Package.getVersionName(context)
        val debug = isDebug
            ?: ((context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0)
        return "$appName $appVersion${if (debug) " (Debug)" else ""}"
    }
}
