package com.kylecorry.trail_sense.tools.astronomy.quickactions

import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.quickactions.ToolServiceQuickAction
import com.kylecorry.trail_sense.tools.astronomy.AstronomyToolRegistration
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.receivers.SunriseAlarmReceiver
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.launch
import java.time.Duration

class QuickActionSunriseAlert(btn: QuickActionButtonView, host: TrailSenseDestinationHost) :
    ToolServiceQuickAction(
        btn,
        host,
        AstronomyToolRegistration.SERVICE_SUNRISE_ALERTS,
        AstronomyToolRegistration.BROADCAST_SUNRISE_ALERTS_STATE_CHANGED
    ) {

    private val prefs by lazy { UserPreferences(context) }
    private val formatter by lazy { FormatService.getInstance(context) }


    override fun onCreate() {
        super.onCreate()
        setIcon(TrailIcons.SunriseQuickAction)
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        host.findNavigator().navigate(Route.Astronomy)
        return true
    }

    override fun onClick() {
        super.onClick()
        host.lifecycleScope.launch {
            when (state) {
                FeatureState.On -> service?.disable()
                FeatureState.Off -> {
                    SunriseAlarmReceiver.enable(host, true)
                    val alertTime =
                        Duration.ofMinutes(prefs.astronomy.sunriseAlertMinutesBefore)
                    val formattedAlertTime = formatter.formatDuration(alertTime)
                    host.toast(
                        context.getString(
                            R.string.sunrise_alert_scheduled,
                            formattedAlertTime
                        )
                    )
                }

                else -> return@launch
            }
        }
    }
}
