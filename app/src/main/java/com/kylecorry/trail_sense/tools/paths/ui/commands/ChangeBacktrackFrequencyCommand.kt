package com.kylecorry.trail_sense.tools.paths.ui.commands

import android.content.Context
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.paths.infrastructure.BacktrackScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Duration

class ChangeBacktrackFrequencyCommand(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onChange: (Duration) -> Unit
) {
    private val prefs by lazy { UserPreferences(context) }

    fun execute(frequency: Duration) {
        if (frequency.isZero) {
            return
        }

        prefs.paths.backtrackRecordFrequency = frequency
        onChange(frequency)
        scope.launch {
            BacktrackScheduler.restart(context)
        }
        if (frequency < Duration.ofMinutes(15)) {
            Alerts.dialog(
                context,
                context.getString(R.string.battery_warning),
                context.getString(R.string.backtrack_battery_warning),
                cancelText = null
            )
        }
    }
}
