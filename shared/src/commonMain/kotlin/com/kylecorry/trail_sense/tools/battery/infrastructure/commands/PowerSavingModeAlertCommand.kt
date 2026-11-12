package com.kylecorry.trail_sense.tools.battery.infrastructure.commands

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.commands.Command
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.views.compose.AppHost

class PowerSavingModeAlertCommand : Command {

    private val preference = getAppService<UserPreferences>()

    override fun execute() {
        if (preference.isLowPowerModeOn) {
            Alerts.snackbar(AppHost.current(), getAppService<AppStrings>().get(Res.string.low_power_mode_on_message))
        }
    }
}
