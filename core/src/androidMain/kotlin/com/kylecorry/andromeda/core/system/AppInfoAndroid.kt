package com.kylecorry.andromeda.core.system

import android.content.pm.PackageManager
import android.os.Build
import com.ksensor.core.context.KSensorContext

actual fun appInfo(): AppInfo {
    val context = KSensorContext.get()
    val manager = context.packageManager
    val info = if (Build.VERSION.SDK_INT >= 33) manager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
    else @Suppress("DEPRECATION") manager.getPackageInfo(context.packageName, 0)
    val build = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
    return AppInfo(context.packageName, context.applicationInfo.loadLabel(manager).toString(), info.versionName, build.toString())
}
