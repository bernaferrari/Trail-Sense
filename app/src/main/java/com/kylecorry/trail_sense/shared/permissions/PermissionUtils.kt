package com.kylecorry.trail_sense.shared.permissions

import android.Manifest
import android.content.Context
import android.os.Build
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.fragments.IPermissionRequester
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.trail_sense.R

fun TrailSenseComposeDestinationFragment.alertNoCameraPermission() {
    Alerts.toast(
        requireContext(),
        getString(R.string.camera_permission_denied),
        short = false
    )
}

fun TrailSenseComposeDestinationFragment.alertExactAlarmsDenied() {
    toast(getString(R.string.exact_alarm_permission_denied), short = false)
}

fun TrailSenseComposeDestinationFragment.getPermissionRequester(): IPermissionRequester? {
    return this
}

fun TrailSenseComposeDestinationFragment.requirePermissionRequester(): IPermissionRequester {
    return this
}

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
