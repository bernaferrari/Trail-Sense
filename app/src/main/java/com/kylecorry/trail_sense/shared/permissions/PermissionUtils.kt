package com.kylecorry.trail_sense.shared.permissions

import android.content.Context
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.kylecorry.andromeda.permissions.Permissions

fun Permissions.canStartLocationForegroundService(context: Context): Boolean {
    // Older API versions don't need foreground permission
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        return true
    }

    return canGetLocationCustom(context)
}

fun Permissions.canStartLocationForgroundService(context: Context): Boolean {
    return canStartLocationForegroundService(context)
}

fun isAppForeground(): Boolean {
    return ProcessLifecycleOwner.get()
        .lifecycle.currentState
        .isAtLeast(Lifecycle.State.STARTED)
}

fun Permissions.canGetLocationCustom(context: Context): Boolean {
    return isBackgroundLocationEnabled(context) || canGetLocation(
        context,
        // We only need to check app ops when running in the background. Running in foreground grants while-in-use and some manufacters incorrectly deal with app ops
        checkAppOps = !isAppForeground()
    )
}
