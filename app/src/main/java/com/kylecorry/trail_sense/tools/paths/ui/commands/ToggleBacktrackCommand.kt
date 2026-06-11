package com.kylecorry.trail_sense.tools.paths.ui.commands

import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.shared.permissions.RemoveBatteryRestrictionsAlerter
import com.kylecorry.trail_sense.shared.permissions.RequestRemoveBatteryRestrictionCommand
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.getFeatureState

class ToggleBacktrackCommand(
    private val host: TrailSenseDestinationHost,
    private val onPermissionDenied: () -> Unit = {}
) : Command {
    override fun execute() {
        host.inBackground {
            val service =
                Tools.getService(host.requireContext(), PathsToolRegistration.SERVICE_BACKTRACK)
                    ?: return@inBackground
            when (service.getFeatureState()) {
                FeatureState.On -> service.disable()
                FeatureState.Off -> {
                    host.requestBacktrackPermission { success ->
                        if (success) {
                            host.inBackground {
                                service.enable()
                                RequestRemoveBatteryRestrictionCommand(
                                    host.requireContext(),
                                    RemoveBatteryRestrictionsAlerter(host)
                                ).execute()
                            }
                        } else {
                            onPermissionDenied()
                        }
                    }
                }

                FeatureState.Unavailable -> host.toast(host.getString(R.string.backtrack_disabled_low_power_toast))
            }
        }
    }


}
