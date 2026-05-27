package com.kylecorry.trail_sense.tools.waterpurification.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.extensions.compose.useBooleanPreference
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSwitchRow

/**
 * Pure Compose migration of the water boil timer settings.
 * Expressive design tokens, modern switch row, no XML preference screen.
 */
class WaterBoilTimerSettingsFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        WaterBoilTimerSettingsContent()
    }
}

@Composable
fun WaterBoilTimerSettingsContent(modifier: Modifier = Modifier) {
    val prefKey = stringResource(R.string.pref_water_boil_timer_use_alarm)
    val (useAlarm, setUseAlarm) = useBooleanPreference(prefKey)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.md)
    ) {
        TrailSenseSectionHeader(
            title = stringResource(R.string.water_boil_timer),
            modifier = Modifier.padding(bottom = TrailSenseSpacing.sm)
        )

        TrailSenseSwitchRow(
            label = stringResource(R.string.use_alarm_for_water_boil_timer),
            checked = useAlarm ?: false,
            onCheckedChange = { setUseAlarm(it) },
            modifier = Modifier.testTag("pref_water_boil_timer_use_alarm")
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WaterBoilTimerSettingsPreview() {
    MaterialTheme {
        WaterBoilTimerSettingsContent()
    }
}
