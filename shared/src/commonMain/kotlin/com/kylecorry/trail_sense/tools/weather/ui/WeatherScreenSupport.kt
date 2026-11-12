package com.kylecorry.trail_sense.tools.weather.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.alerts.IAlerter
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.views.compose.AppPermission
import com.kylecorry.trail_sense.shared.views.compose.ScreenEffects
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Offers to lift battery restrictions after the weather monitor starts: a long snackbar whose Learn more asks to
 * ignore battery optimizations, then says so when allowed. The screen-effects form of RemoveBatteryRestrictionsAlerter.
 */
class WeatherBatteryRestrictionsAlerter(
    private val effects: ScreenEffects,
    private val scope: CoroutineScope
) : IAlerter {
    override fun alert() {
        scope.launch {
            val learnMore = effects.showSnackbar(
                getAppService<AppStrings>().get(Res.string.battery_settings_limit_accuracy),
                actionLabel = getAppService<AppStrings>().get(Res.string.learn_more),
                long = true
            )
            if (learnMore && effects.requestPermission(AppPermission.IgnoreBatteryOptimizations)) {
                // Let the user know they did it right since this process is confusing
                effects.showMessage(getAppService<AppStrings>().get(Res.string.battery_usage_allowed))
            }
        }
    }
}

