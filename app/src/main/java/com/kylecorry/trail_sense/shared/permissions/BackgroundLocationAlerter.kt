package com.kylecorry.trail_sense.shared.permissions

import android.os.Build
import com.google.android.material.snackbar.Snackbar
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.markdown.MarkdownService
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
            context.getString(R.string.learn_more)
        ) {

            val markdown = DependencyRegistry.get<MarkdownService>()
            val contents =
                markdown.toMarkdown(context.getString(R.string.access_background_location_rationale))

            Alerts.dialog(
                context,
                context.getString(R.string.access_background_location),
                contents,
                okText = context.getString(R.string.settings),
                allowLinks = true
            ) { cancelled ->
                if (!cancelled) {
                    host.requestBackgroundLocation {
                        // Do nothing
                    }
                }
            }
        }
    }

}
