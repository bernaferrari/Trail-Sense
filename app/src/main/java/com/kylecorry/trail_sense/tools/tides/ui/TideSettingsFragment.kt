package com.kylecorry.trail_sense.tools.tides.ui

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

class TideSettingsFragment : TrailSenseComposeFragment() {

    private val prefs by lazy { UserPreferences(requireContext()).tides }

    @Composable
    override fun FragmentContent() {
        var showNearestTide by remember { mutableStateOf(prefs.showNearestTide) }
        SingleSwitchSettingsScreen(
            title = stringResource(R.string.tides),
            label = stringResource(R.string.pref_show_nearest_tide_title),
            checked = showNearestTide,
            onCheckedChange = {
                showNearestTide = it
                prefs.showNearestTide = it
            }
        )
    }

}
