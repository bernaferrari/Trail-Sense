package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme


class ClockSettingsFragment : Fragment() {

    private val prefs by lazy { UserPreferences(requireContext()).clock }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    var showAnalogClock by remember { mutableStateOf(prefs.showAnalogClock) }
                    SingleSwitchSettingsScreen(
                        title = stringResource(R.string.tool_clock_title),
                        label = stringResource(R.string.pref_enable_analog_title),
                        checked = showAnalogClock,
                        onCheckedChange = {
                            showAnalogClock = it
                            prefs.showAnalogClock = it
                        }
                    )
                }
            }
        }
    }
}
