package com.kylecorry.trail_sense.tools.level.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.SettingsActionRow
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

class BubbleLevelSettingsFragment : Fragment() {

    private val prefs = DependencyRegistry.get<UserPreferences>().bubbleLevel
    private val formatter = DependencyRegistry.get<FormatService>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    var threshold by remember { mutableIntStateOf(prefs.threshold.toInt()) }
                    val title = stringResource(R.string.pref_bubble_level_threshold_title)
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = TrailSenseSpacing.lg,
                                end = TrailSenseSpacing.lg,
                                top = TrailSenseSpacing.lg,
                                bottom = TrailSenseSpacing.xxxl
                            )
                        ) {
                            item("header") {
                                TrailSenseSectionHeader(
                                    title = stringResource(R.string.tool_bubble_level_title)
                                )
                            }
                            item("threshold") {
                                SettingsActionRow(
                                    label = title,
                                    summary = formatter.formatDegrees(threshold.toFloat()),
                                    onClick = {
                                        pickThreshold(title, threshold) {
                                            threshold = it
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun pickThreshold(title: String, current: Int, onChange: (Int) -> Unit) {
        val picker = NumberPicker(requireContext())
        picker.minValue = 0
        picker.maxValue = 10
        picker.value = current
        picker.wrapSelectorWheel = false

        Alerts.dialog(
            requireContext(),
            title,
            contentView = picker
        ) { cancelled ->
            if (!cancelled) {
                prefs.threshold = picker.value.toFloat()
                onChange(picker.value)
            }
        }
    }
}
