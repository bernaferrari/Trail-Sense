package com.kylecorry.trail_sense.tools.paths.ui.commands

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.alerts.IAlerter
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.shared.permissions.RequestRemoveBatteryRestrictionCommand
import com.kylecorry.trail_sense.shared.views.compose.AppPermission
import com.kylecorry.trail_sense.shared.views.compose.ScreenEffects
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.getFeatureState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Turns Backtrack off, or on after asking for its permissions (then offers to lift battery restrictions). Runs in
 * [scope]; permissions and messages go through [effects].
 */
class ToggleBacktrackCommand(
    private val context: Context,
    private val effects: ScreenEffects,
    private val scope: CoroutineScope,
    private val onPermissionDenied: () -> Unit = {}
) : Command {

    /**
     * For callers that still hold a destination host: the Backtrack quick action (the shared QuickActionButton hands
     * it one) and Paths settings. The Compose destination host is also the screen's [ScreenEffects].
     */
    constructor(host: TrailSenseDestinationHost, onPermissionDenied: () -> Unit = {}) : this(
        host.requireContext(),
        host as ScreenEffects,
        host.lifecycleScope,
        onPermissionDenied
    )

    override fun execute() {
        scope.launch {
            val service = Tools.getService(context, PathsToolRegistration.SERVICE_BACKTRACK) ?: return@launch
            when (service.getFeatureState()) {
                FeatureState.On -> service.disable()
                FeatureState.Off -> {
                    if (effects.requestPermission(AppPermission.Backtrack)) {
                        service.enable()
                        RequestRemoveBatteryRestrictionCommand(
                            context,
                            RemoveBatteryRestrictionsSnackbar(context, effects, scope)
                        ).execute()
                    } else {
                        onPermissionDenied()
                    }
                }

                FeatureState.Unavailable -> effects.showMessage(
                    context.getString(R.string.backtrack_disabled_low_power_toast)
                )
            }
        }
    }
}

/** The battery restrictions snackbar (RemoveBatteryRestrictionsAlerter) through [ScreenEffects]. */
private class RemoveBatteryRestrictionsSnackbar(
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
            // Let the user know they did it right since this process is confusing
            if (learnMore && effects.requestPermission(AppPermission.IgnoreBatteryOptimizations)) {
                effects.showMessage(context.getString(R.string.battery_usage_allowed))
            }
        }
    }
}
