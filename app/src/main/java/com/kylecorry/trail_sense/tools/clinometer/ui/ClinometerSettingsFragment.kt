package com.kylecorry.trail_sense.tools.clinometer.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.SettingsActionRow
import com.kylecorry.trail_sense.settings.ui.SettingsSwitchRow
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

class ClinometerSettingsFragment : TrailSenseComposeFragment() {

    private val prefs by lazy { UserPreferences(requireContext()) }
    private val formatter by lazy { FormatService.getInstance(requireContext()) }

    @Composable
    override fun FragmentContent() {
                    var lockWithVolume by remember {
                        mutableStateOf(prefs.clinometer.lockWithVolumeButtons)
                    }
                    var baselineDistance by remember {
                        mutableStateOf(prefs.clinometer.baselineDistance)
                    }
                    val baselineTitle =
                        stringResource(R.string.pref_clinometer_baseline_distance_title)
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
                                    title = stringResource(R.string.clinometer_title)
                                )
                            }
                            item("volume_lock") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.pref_clinometer_lock_with_volume_buttons_title),
                                    checked = lockWithVolume,
                                    onCheckedChange = {
                                        lockWithVolume = it
                                        prefs.clinometer.lockWithVolumeButtons = it
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("baseline_distance") {
                                SettingsActionRow(
                                    label = baselineTitle,
                                    summary = formatDistance(baselineDistance),
                                    onClick = {
                                        pickBaselineDistance(baselineTitle, baselineDistance) {
                                            baselineDistance = it
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                        }
                    }
    }

    private fun pickBaselineDistance(
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

    private fun formatDistance(distance: Distance?): String {
        return distance?.let {
            formatter.formatDistance(
                it,
                Units.getDecimalPlaces(it.units)
            )
        } ?: getString(R.string.dash)
    }
}
