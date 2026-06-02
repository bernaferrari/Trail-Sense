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

class CellSignalSettingsFragment : Fragment() {

    private val prefs by lazy { UserPreferences(requireContext()).cellSignal }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    var populateCache by remember { mutableStateOf(prefs.populateCache) }
                    SingleSwitchSettingsScreen(
                        title = stringResource(R.string.cell_signal),
                        label = stringResource(R.string.pref_cell_signal_refresh_cache_title),
                        summary = stringResource(R.string.pref_cell_signal_refresh_cache_summary),
                        checked = populateCache,
                        onCheckedChange = {
                            populateCache = it
                            prefs.populateCache = it
                        }
                    )
                }
            }
        }
    }

}
