package com.kylecorry.trail_sense.settings.ui

import com.kylecorry.trail_sense.main.getAppService

import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.shared.generated.resources.*

import com.kylecorry.trail_sense.shared.dialogs.UnitPickers
import com.kylecorry.trail_sense.shared.views.compose.AppHost



import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection

@Composable
fun ClinometerSettingsToolScreen() {
    val prefs = remember { getAppService<UserPreferences>() }
    val formatter = remember { FormatService.getInstance() }
    var lockWithVolume by remember {
        mutableStateOf(prefs.clinometer.lockWithVolumeButtons)
    }
    var baselineDistance by remember {
        mutableStateOf(prefs.clinometer.baselineDistance)
    }
    val baselineTitle = appString(Res.string.pref_clinometer_baseline_distance_title)

    SettingsList(title = appString(Res.string.clinometer_title)) {
        trailSenseSection("clinometer") {
            row { position ->
                SettingsSwitchRow(
                    label = appString(Res.string.pref_clinometer_lock_with_volume_buttons_title),
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
                    summary = formatDistance(formatter, baselineDistance),
                    onClick = {
                        pickBaselineDistance(prefs, formatter, baselineTitle, baselineDistance) {
                            baselineDistance = it
                        }
                    },
                    position = position
                )
            }
        }
    }
}

private fun pickBaselineDistance(
    prefs: UserPreferences,
    formatter: FormatService,
    title: String,
    current: Distance?,
    onChange: (Distance?) -> Unit
) {
    UnitPickers.distance(
        AppHost.current(),
        formatter.sortDistanceUnits(DistanceUtils.hikingDistanceUnits),
        current,
        title
    ) { distance, cancelled ->
        if (cancelled) {
            return@distance
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
