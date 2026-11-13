package com.kylecorry.trail_sense.tools.paths.ui.commands

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.permissions.BatteryRestrictionPrompt
import com.kylecorry.trail_sense.shared.permissions.BatteryRestrictionsAlerter
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.views.compose.AppPermission
import com.kylecorry.trail_sense.shared.views.compose.ScreenEffects
import com.kylecorry.trail_sense.tools.paths.PathsToolServices
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCatalog
import com.kylecorry.trail_sense.tools.tools.infrastructure.getFeatureState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Turns Backtrack off, or on after asking for its permissions (then offers to lift battery restrictions). Runs in
 * [scope]; permissions and messages go through [effects].
 */
class ToggleBacktrackCommand(
    private val effects: ScreenEffects,
    private val scope: CoroutineScope,
    private val onPermissionDenied: () -> Unit = {}
) : Command {

    override fun execute() {
        scope.launch {
            val service = getAppService<ToolCatalog>().getService(PathsToolServices.BACKTRACK) ?: return@launch
            when (service.getFeatureState()) {
                FeatureState.On -> service.disable()
                FeatureState.Off -> {
                    if (effects.requestPermission(AppPermission.Backtrack)) {
                        service.enable()
                        getAppService<BatteryRestrictionPrompt>().request(BatteryRestrictionsAlerter(effects, scope))
                    } else {
                        onPermissionDenied()
                    }
                }

                FeatureState.Unavailable -> effects.showMessage(
                    getAppService<AppStrings>().get(Res.string.backtrack_disabled_low_power_toast)
                )
            }
        }
    }
}
