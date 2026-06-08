package com.kylecorry.trail_sense.tools.ruler.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
        var showCalibrationDialog by remember { mutableStateOf(false) }
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
                            showCalibrationDialog = true
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

        if (showCalibrationDialog) {
            RulerScaleDialog(
                title = calibrationTitle,
                current = scale,
                onDismiss = { showCalibrationDialog = false },
                onConfirm = {
                    scale = it
                    prefs.rulerScale = it
                    showCalibrationDialog = false
                }
            )
        }
    }

    @Composable
    private fun RulerScaleDialog(
        title: String,
        current: Float,
        onDismiss: () -> Unit,
        onConfirm: (Float) -> Unit
    ) {
        var value by remember(current) { mutableStateOf(formatScale(current)) }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = title)
            },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(text = stringResource(R.string.pref_ruler_calibration_summary)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm((value.toFloatCompat() ?: 1f).coerceAtLeast(0f))
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            }
        )
    }

    private fun formatScale(scale: Float): String {
        return scale.toString()
    }
}
