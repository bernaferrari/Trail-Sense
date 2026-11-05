package com.kylecorry.trail_sense.shared.permissions

import com.kylecorry.trail_sense.shared.views.compose.AppPermission

import kotlinx.coroutines.launch

import androidx.lifecycle.lifecycleScope

import com.google.android.material.snackbar.Snackbar
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.alerts.IAlerter
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost

class RemoveBatteryRestrictionsAlerter(private val host: TrailSenseDestinationHost) : IAlerter {

    override fun alert() {
        val context = host.context
        CustomUiUtils.snackbar(
            host,
            context.getString(R.string.battery_settings_limit_accuracy),
            Snackbar.LENGTH_LONG,
            context.getString(R.string.learn_more)
        ) {
            host.lifecycleOwner.lifecycleScope.launch {
                // Let the user know they did it right since this process is confusing
                if (host.requestPermission(AppPermission.IgnoreBatteryOptimizations)) {
                    Alerts.toast(context, context.getString(R.string.battery_usage_allowed))
                }
            }
        }
    }

}
