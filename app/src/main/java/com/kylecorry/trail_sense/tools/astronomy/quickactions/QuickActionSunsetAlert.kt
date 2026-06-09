package com.kylecorry.trail_sense.tools.astronomy.quickactions

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

class QuickActionSunsetAlert(btn: QuickActionButtonView, fragment: TrailSenseDestinationHost) :
    ToolServiceQuickAction(
        btn,
        fragment,
        AstronomyToolRegistration.SERVICE_SUNSET_ALERTS,
        AstronomyToolRegistration.BROADCAST_SUNSET_ALERTS_STATE_CHANGED
    ) {

    private val prefs by lazy { UserPreferences(context) }
    private val formatter by lazy { FormatService.getInstance(context) }


    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.ic_sunset_notification)
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        fragment.findNavController().openTool(Tools.ASTRONOMY)
        return true
    }

    override fun onClick() {
        super.onClick()
        fragment.lifecycleScope.launch {
            when (state) {
                FeatureState.On -> service?.disable()
                FeatureState.Off -> {
                    SunsetAlarmReceiver.enable(fragment, true)
                    val alertTime = Duration.ofMinutes(prefs.astronomy.sunsetAlertMinutesBefore)
                    val formattedAlertTime = formatter.formatDuration(alertTime)
                    fragment.toast(
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
