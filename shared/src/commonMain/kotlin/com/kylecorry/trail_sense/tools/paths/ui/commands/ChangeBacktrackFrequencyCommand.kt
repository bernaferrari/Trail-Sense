package com.kylecorry.trail_sense.tools.paths.ui.commands

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.tools.paths.infrastructure.BacktrackRestarter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class ChangeBacktrackFrequencyCommand(
    private val host: AppHost = AppHost.current(),
    private val scope: CoroutineScope,
    private val onChange: (Duration) -> Unit
) {
    private val prefs by lazy { getAppService<UserPreferences>() }

    fun execute(frequency: Duration) {
        if (frequency == Duration.ZERO) {
            return
        }

        prefs.paths.backtrackRecordFrequency = frequency
        onChange(frequency)
        scope.launch {
            getAppService<BacktrackRestarter>().restart()
        }
        if (frequency < 15.minutes) {
            val strings = getAppService<AppStrings>()
            Alerts.dialog(
                host,
                strings.get(Res.string.battery_warning),
                strings.get(Res.string.backtrack_battery_warning),
                cancelText = null
            )
        }
    }
}
