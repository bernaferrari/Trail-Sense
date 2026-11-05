package com.kylecorry.trail_sense.settings.ui

import android.content.Context

import androidx.compose.ui.platform.LocalContext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection

@Composable
fun ClinometerSettingsToolScreen() {
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }
    val formatter = remember { FormatService.getInstance(context) }
    var lockWithVolume by remember {
        mutableStateOf(prefs.clinometer.lockWithVolumeButtons)
    }
    var baselineDistance by remember {
        mutableStateOf(prefs.clinometer.baselineDistance)
    }
    val baselineTitle = stringResource(R.string.pref_clinometer_baseline_distance_title)

    SettingsList(title = stringResource(R.string.clinometer_title)) {
        trailSenseSection("clinometer") {
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.pref_clinometer_lock_with_volume_buttons_title),
                    checked = lockWithVolume,
                    onCheckedChange = {
                        lockWithVolume = it
                        prefs.clinometer.lockWithVolumeButtons = it
                    },
                    position = position
                )
            }
            row { position ->
                SettingsActionRow(
                    label = baselineTitle,
                    summary = context.formatDistance(formatter, baselineDistance),
                    onClick = {
                        context.pickBaselineDistance(prefs, formatter, baselineTitle, baselineDistance) {
                            baselineDistance = it
                        }
                    },
                    position = position
                )
            }
        }
    }
}

private fun Context.pickBaselineDistance(
    prefs: UserPreferences,
    formatter: FormatService,
    title: String,
    current: Distance?,
    onChange: (Distance?) -> Unit
) {
    CustomUiUtils.pickDistance(
        this,
        formatter.sortDistanceUnits(DistanceUtils.hikingDistanceUnits),
        current,
        title
    ) { distance, cancelled ->
        if (cancelled) {
            return@pickDistance
        }
        val value = if (distance != null && distance.value > 0) {
            distance
        } else {
            null
        }
        prefs.clinometer.baselineDistance = value
        onChange(value)
    }
}
