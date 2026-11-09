package com.kylecorry.andromeda.exceptions

import android.content.Context
import android.content.pm.ApplicationInfo
import com.kylecorry.andromeda.core.system.Package

/**
 * An [AppDetailsReport] for the installed Android app: its version name comes from the package manager.
 * @param debug whether this is a debug build; defaults to the app's debuggable flag
 */
fun AppDetailsReport(context: Context, name: String, debug: Boolean? = null): AppDetailsReport {
    val isDebug = debug ?: ((context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0)
    return AppDetailsReport(name, Package.getVersionName(context), isDebug)
}
