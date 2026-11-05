package com.kylecorry.trail_sense.settings.ui

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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection

@Composable
fun ClinometerSettingsToolScreen(host: TrailSenseDestinationHost) {
    val prefs = remember { UserPreferences(host.requireContext()) }
    val formatter = remember { FormatService.getInstance(host.requireContext()) }
    var lockWithVolume by remember {
        mutableStateOf(prefs.clinometer.lockWithVolumeButtons)
    }
    var baselineDistance by remember {
        mutableStateOf(prefs.clinometer.baselineDistance)
    }
    val baselineTitle = stringResource(R.string.pref_clinometer_baseline_distance_title)

    SettingsList(host, title = stringResource(R.string.clinometer_title)) {
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
                    summary = host.formatDistance(formatter, baselineDistance),
                    onClick = {
                        host.pickBaselineDistance(prefs, formatter, baselineTitle, baselineDistance) {
                            baselineDistance = it
                        }
                    },
                    position = position
                )
            }
        }
    }
}

private fun TrailSenseDestinationHost.pickBaselineDistance(
    prefs: UserPreferences,
    formatter: FormatService,
    title: String,
    current: Distance?,
    onChange: (Distance?) -> Unit
) {
    CustomUiUtils.pickDistance(
        requireContext(),
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
