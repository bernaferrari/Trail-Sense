package com.kylecorry.trail_sense.tools.ruler.ui

import android.text.InputType
import android.widget.EditText
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.luna.text.toFloatCompat
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.SettingsActionRow
import com.kylecorry.trail_sense.settings.ui.SettingsSwitchRow
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

class ToolRulerSettingsFragment : TrailSenseComposeFragment() {

    private val prefs by lazy { UserPreferences(requireContext()).ruler }

    @Composable
    override fun FragmentContent() {
        var scale by remember { mutableFloatStateOf(prefs.rulerScale) }
        var flipped by remember { mutableStateOf(prefs.rulerFlipped) }
        val calibrationTitle =
            stringResource(R.string.pref_ruler_calibration_title)
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
                        title = stringResource(R.string.tool_ruler_title)
                    )
                }
                item("calibration") {
                    SettingsActionRow(
                        label = calibrationTitle,
                        summary = formatScale(scale),
                        onClick = {
                            pickScale(calibrationTitle, scale) {
                                scale = it
                            }
                        },
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                }
                item("flip") {
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_ruler_flip_direction_title),
                        checked = flipped,
                        onCheckedChange = {
                            flipped = it
                            prefs.rulerFlipped = it
                        },
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                    TrailSenseDivider()
                }
            }
        }
    }

    private fun pickScale(title: String, current: Float, onChange: (Float) -> Unit) {
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER.or(InputType.TYPE_NUMBER_FLAG_DECIMAL)
            setText(formatScale(current))
            selectAll()
        }

        Alerts.dialog(
            requireContext(),
            title,
            content = getString(R.string.pref_ruler_calibration_summary),
            contentView = input
        ) { cancelled ->
            if (!cancelled) {
                val value = (input.text.toString().toFloatCompat() ?: 1f).coerceAtLeast(0f)
                prefs.rulerScale = value
                onChange(value)
            }
        }
    }

    private fun formatScale(scale: Float): String {
        return scale.toString()
    }
}
