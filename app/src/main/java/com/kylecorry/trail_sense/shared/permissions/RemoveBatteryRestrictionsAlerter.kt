package com.kylecorry.trail_sense.shared.permissions

import com.google.android.material.snackbar.Snackbar
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.alerts.IAlerter
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost

class RemoveBatteryRestrictionsAlerter(private val host: TrailSenseDestinationHost) : IAlerter {

    override fun alert() {
        val context = host.requireContext()
        CustomUiUtils.snackbar(
            host,
            context.getString(R.string.battery_settings_limit_accuracy),
            Snackbar.LENGTH_LONG,
            context.getString(R.string.learn_more)
        ) {
            host.requestIgnoreBatteryOptimizations { isGranted ->
                // Let the user know they did it right since this process is confusing
                if (isGranted) {
                    Alerts.toast(context, context.getString(R.string.battery_usage_allowed))
                }
            }
        }
    }

}
