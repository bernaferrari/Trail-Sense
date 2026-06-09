package com.kylecorry.trail_sense.shared.permissions

import android.content.Context
import android.os.Build
import com.kylecorry.andromeda.permissions.Permissions

fun Permissions.canStartLocationForgroundService(context: Context): Boolean {
    // Older API versions don't need foreground permission
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        return true
    }

    // The service can be started if it has background permission or if the system says it can get location
    return isBackgroundLocationEnabled(context) || canGetLocation(context, checkAppOps = true)
}

fun Permissions.canGetLocationCustom(context: Context): Boolean {
    return isBackgroundLocationEnabled(context) || canGetLocation(context, checkAppOps = true)
}
