package com.kylecorry.trail_sense.tools.waterpurification.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.SingleSwitchSettingsScreen
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment

class WaterBoilTimerSettingsFragment : TrailSenseComposeFragment() {

    private val prefs by lazy { UserPreferences(requireContext()).waterBoilTimer }

    @Composable
    override fun FragmentContent() {
        var useAlarm by remember { mutableStateOf(prefs.useAlarm) }
        SingleSwitchSettingsScreen(
            title = stringResource(R.string.water_boil_timer),
            label = stringResource(R.string.use_alarm_for_water_boil_timer),
            summary = stringResource(R.string.alarm_description),
            checked = useAlarm,
            onCheckedChange = {
                useAlarm = it
                prefs.useAlarm = it
            }
        )
    }
}
