package com.kylecorry.trail_sense.tools.weather.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.alerts.IAlerter
import com.kylecorry.trail_sense.shared.views.compose.AppPermission
import com.kylecorry.trail_sense.shared.views.compose.ScreenEffects
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Offers to lift battery restrictions after the weather monitor starts: a long snackbar whose Learn more asks to
 * ignore battery optimizations, then says so when allowed. The screen-effects form of RemoveBatteryRestrictionsAlerter.
 */
internal class WeatherBatteryRestrictionsAlerter(
    private val context: Context,
    private val effects: ScreenEffects,
    private val scope: CoroutineScope
) : IAlerter {
    override fun alert() {
        scope.launch {
            val learnMore = effects.showSnackbar(
                context.getString(R.string.battery_settings_limit_accuracy),
                actionLabel = context.getString(R.string.learn_more),
                long = true
            )
            if (learnMore && effects.requestPermission(AppPermission.IgnoreBatteryOptimizations)) {
                // Let the user know they did it right since this process is confusing
                effects.showMessage(context.getString(R.string.battery_usage_allowed))
            }
        }
    }
}

/** Calls [onReceive] on the main thread for each [toolBroadcastId] broadcast while the screen is resumed. */
@Composable
internal fun ToolBroadcastEffect(toolBroadcastId: String, onReceive: () -> Unit) {
    val scope = rememberCoroutineScope()
    val latest by rememberUpdatedState(onReceive)
    LifecycleResumeEffect(toolBroadcastId) {
        val callback: (android.os.Bundle) -> Unit = { scope.launch { latest() } }
        Tools.subscribe(toolBroadcastId, callback)
        onPauseOrDispose {
            Tools.unsubscribe(toolBroadcastId, callback)
        }
    }
}
