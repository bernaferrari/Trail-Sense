package com.kylecorry.trail_sense.settings.ui

import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.kylecorry.luna.text.toFloatCompat
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDialog
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection

@Composable
fun RulerSettingsToolScreen() {
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context).ruler }
    var scale by remember { mutableFloatStateOf(prefs.rulerScale) }
    var flipped by remember { mutableStateOf(prefs.rulerFlipped) }
    var showCalibrationDialog by remember { mutableStateOf(false) }
    val calibrationTitle = stringResource(R.string.pref_ruler_calibration_title)

    SettingsList(title = stringResource(R.string.tool_ruler_title)) {
        trailSenseSection("ruler") {
            row { position ->
                SettingsActionRow(
                    label = calibrationTitle,
                    summary = formatScale(scale),
                    onClick = {
                        showCalibrationDialog = true
                    },
                    position = position
                )
            }
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.pref_ruler_flip_direction_title),
                    checked = flipped,
                    onCheckedChange = {
                        flipped = it
                        prefs.rulerFlipped = it
                    },
                    position = position
                )
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
    TrailSenseDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmText = stringResource(android.R.string.ok),
        onConfirm = { onConfirm((value.toFloatCompat() ?: 1f).coerceAtLeast(0f)) },
        dismissText = stringResource(android.R.string.cancel)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
        ) {
            Text(
                text = stringResource(R.string.pref_ruler_calibration_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TrailSenseInputField(
                value = value,
                onValueChange = { value = it },
                label = title,
                keyboardType = KeyboardType.Decimal
            )
        }
    }
}

private fun formatScale(scale: Float): String {
    return scale.toString()
}
