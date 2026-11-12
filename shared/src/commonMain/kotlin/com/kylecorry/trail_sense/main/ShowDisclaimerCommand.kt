package com.kylecorry.trail_sense.main

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.dialogs.disclaimer
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.settings.licenses.Licenses
import com.kylecorry.trail_sense.shared.commands.Command

class ShowDisclaimerCommand() : Command {
    override fun execute() {
        // Shown value was previously considered to be false, but this was changed to true to make it clearer
        val prefs = getAppService<Preferences>()
        if (prefs.getBoolean(PreferenceKeys.mainDisclaimerShownKey) == false) {
            prefs.putBoolean(PreferenceKeys.mainDisclaimerShownKey, true)
        }

        Alerts.disclaimer(AppHost.current(),
            getAppService<AppStrings>().get(Res.string.app_disclaimer_message_title),
            getAppService<AppStrings>().get(Res.string.disclaimer_message_content) + "\n\n" + Licenses.mit(
                "2020",
                "Kyle Corry"
            ),
            PreferenceKeys.mainDisclaimerShownKey,
            considerShownIfCancelled = true,
            cancelText = null,
            okText = getAppService<AppStrings>().get(Res.string.i_agree),
            shownValue = true
        )
    }
}
