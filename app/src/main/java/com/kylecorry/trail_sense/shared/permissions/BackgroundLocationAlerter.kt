package com.kylecorry.trail_sense.shared.permissions

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

        val context = host.requireContext()
        CustomUiUtils.snackbar(
            host,
            context.getString(R.string.sunset_alerts_background_location_disclaimer),
            Snackbar.LENGTH_LONG,
            context.getString(R.string.settings)
        ) {
            host.requestBackgroundLocation {
                // The system permission screen supplies the detailed explanation.
            }
        }
    }

}
