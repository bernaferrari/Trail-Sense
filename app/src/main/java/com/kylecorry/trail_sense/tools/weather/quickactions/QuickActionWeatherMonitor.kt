package com.kylecorry.trail_sense.tools.weather.quickactions

import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.permissions.RemoveBatteryRestrictionsAlerter
import com.kylecorry.trail_sense.shared.permissions.RequestRemoveBatteryRestrictionCommand
import com.kylecorry.trail_sense.shared.quickactions.ToolServiceQuickAction
import com.kylecorry.trail_sense.tools.weather.WeatherToolRegistration
import kotlinx.coroutines.launch

class QuickActionWeatherMonitor(
    btn: QuickActionButtonView,
    fragment: TrailSenseDestinationHost
) : ToolServiceQuickAction(
    btn,
    fragment,
    WeatherToolRegistration.SERVICE_WEATHER_MONITOR,
    WeatherToolRegistration.BROADCAST_WEATHER_MONITOR_STATE_CHANGED,
    hideWhenUnavailable = false
) {

    override fun onCreate() {
        super.onCreate()
        setIcon(R.drawable.ic_weather)
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        fragment.findNavController().navigateWithAnimation(R.id.action_weather)
        return true
    }

    override fun onClick() {
        super.onClick()
        fragment.lifecycleScope.launch {
            when (state) {
                FeatureState.On -> service?.disable()
                FeatureState.Off -> {
                    service?.enable()
                    RequestRemoveBatteryRestrictionCommand(
                        fragment.requireContext(),
                        RemoveBatteryRestrictionsAlerter(fragment)
                    ).execute()
                }

                else -> fragment.toast(context.getString(R.string.weather_monitoring_disabled))
            }
        }
    }
}
