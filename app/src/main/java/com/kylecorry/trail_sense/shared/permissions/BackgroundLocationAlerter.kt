package com.kylecorry.trail_sense.shared.permissions

import com.kylecorry.trail_sense.shared.views.compose.AppPermission

import kotlinx.coroutines.launch

import androidx.lifecycle.lifecycleScope

import android.os.Build
import com.google.android.material.snackbar.Snackbar
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.alerts.IAlerter
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost

class BackgroundLocationAlerter(private val host: TrailSenseDestinationHost) : IAlerter {

    override fun alert() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return
        }

        val context = host.context
        CustomUiUtils.snackbar(
            host,
            context.getString(R.string.sunset_alerts_background_location_disclaimer),
            Snackbar.LENGTH_LONG,
            context.getString(R.string.settings)
        ) {
            // The system permission screen supplies the detailed explanation.
            host.lifecycleOwner.lifecycleScope.launch { host.requestPermission(AppPermission.BackgroundLocation) }
        }
    }

}
