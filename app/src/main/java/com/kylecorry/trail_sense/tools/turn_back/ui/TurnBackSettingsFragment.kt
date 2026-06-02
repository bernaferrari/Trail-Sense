package com.kylecorry.trail_sense.tools.turn_back.ui

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
import com.kylecorry.trail_sense.settings.ui.SingleSwitchSettingsScreen
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme

class TurnBackSettingsFragment : Fragment() {

    private val prefs by lazy { UserPreferences(requireContext()).turnBack }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    var useAlarm by remember { mutableStateOf(prefs.useAlarm) }
                    SingleSwitchSettingsScreen(
                        title = stringResource(R.string.tool_turn_back),
                        label = stringResource(R.string.use_alarm_for_turn_back_alert),
                        summary = stringResource(R.string.alarm_description),
                        checked = useAlarm,
                        onCheckedChange = {
                            useAlarm = it
                            prefs.useAlarm = it
                        }
                    )
                }
            }
        }
    }
}
