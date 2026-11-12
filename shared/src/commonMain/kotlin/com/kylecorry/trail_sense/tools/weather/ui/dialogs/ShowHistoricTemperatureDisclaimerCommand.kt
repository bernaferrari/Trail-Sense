package com.kylecorry.trail_sense.tools.weather.ui.dialogs

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.dialogs.disclaimer
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.shared.commands.Command

class ShowHistoricTemperatureDisclaimerCommand() : Command {
    override fun execute() {
        val source = "NASA Global Modeling and Assimilation Office - MERRA-2"
        Alerts.disclaimer(AppHost.current(),
            getAppService<AppStrings>().get(Res.string.historic_temperatures),
            getAppService<AppStrings>().get(Res.string.historic_temperatures_full_disclaimer,
                30
            ) + "\n\n" + getAppService<AppStrings>().get(Res.string.data_source, source),
            PreferenceKeys.historicTemperaturesDisclaimerShown,
            considerShownIfCancelled = true,
            cancelText = null,
            shownValue = true
        )
    }
}
