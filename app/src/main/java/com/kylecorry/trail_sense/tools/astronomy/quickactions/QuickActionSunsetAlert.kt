package com.kylecorry.trail_sense.tools.astronomy.quickactions

import androidx.compose.material.icons.outlined.WbTwilight
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.shared.quickactions.ToolServiceQuickAction
import com.kylecorry.trail_sense.tools.astronomy.AstronomyToolRegistration
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.receivers.SunsetAlarmReceiver
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.launch
import java.time.Duration

class QuickActionSunsetAlert(btn: QuickActionButtonView, host: TrailSenseDestinationHost) :
    ToolServiceQuickAction(
        btn,
        host,
        AstronomyToolRegistration.SERVICE_SUNSET_ALERTS,
        AstronomyToolRegistration.BROADCAST_SUNSET_ALERTS_STATE_CHANGED
    ) {

    private val prefs by lazy { UserPreferences(context) }
    private val formatter by lazy { FormatService.getInstance(context) }


    override fun onCreate() {
        super.onCreate()
        // The sun on the horizon, outlined like the other app-bar actions (the solid disc read as "download").
        setIcon(androidx.compose.material.icons.Icons.Outlined.WbTwilight)
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        host.findNavigator().openTool(Tools.ASTRONOMY)
        return true
    }

    override fun onClick() {
        super.onClick()
        host.lifecycleScope.launch {
            when (state) {
                FeatureState.On -> service?.disable()
                FeatureState.Off -> {
                    SunsetAlarmReceiver.enable(host, true)
                    val alertTime = Duration.ofMinutes(prefs.astronomy.sunsetAlertMinutesBefore)
                    val formattedAlertTime = formatter.formatDuration(alertTime)
                    host.toast(
                        context.getString(
                            R.string.sunset_alert_scheduled,
                            formattedAlertTime
                        )
                    )
                }

                else -> return@launch
            }
        }
    }
}
