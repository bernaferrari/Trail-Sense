package com.kylecorry.trail_sense.tools.augmented_reality.ui

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
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.SettingsActionRow
import com.kylecorry.trail_sense.settings.ui.SettingsSwitchRow
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

class AugmentedRealitySettingsFragment : TrailSenseComposeFragment() {

    private val userPrefs by lazy { UserPreferences(requireContext()) }
    private val formatter by lazy { FormatService.getInstance(requireContext()) }
    private val distanceUnits by lazy {
        formatter.sortDistanceUnits(DistanceUtils.hikingDistanceUnits)
    }

    @Composable
    override fun FragmentContent() {
                    val arPrefs = userPrefs.augmentedReality
                    val hasGyro = remember { Sensors.hasGyroscope(requireContext()) }
                    val pathDistanceDescription = remember {
                        getString(
                            R.string.path_distance_performance_disclaimer,
                            formatter.formatDistance(
                                relative(
                                    Distance.meters(
                                        arPrefs.maxRecommendedPathViewDistanceMeters
                                    )
                                )
                            )
                        )
                    }

                    var showBeaconLayer by remember {
                        mutableStateOf(arPrefs.showBeaconLayer)
                    }
                    var showPathLayer by remember { mutableStateOf(arPrefs.showPathLayer) }
                    var showAstronomyLayer by remember {
                        mutableStateOf(arPrefs.showAstronomyLayer)
                    }
                    var showStars by remember { mutableStateOf(arPrefs.showStars) }
                    var showLowBrightnessObjects by remember {
                        mutableStateOf(arPrefs.showLowBrightnessObjects)
                    }
                    var showConstellations by remember {
                        mutableStateOf(arPrefs.showConstellations)
                    }
                    var showAstronomyBelowHorizon by remember {
                        mutableStateOf(arPrefs.showAstronomyBelowHorizon)
                    }
                    var showGridLayer by remember { mutableStateOf(arPrefs.showGridLayer) }
                    var useGyroOnlyAfterCalibration by remember {
                        mutableStateOf(arPrefs.useGyroOnlyAfterCalibration)
                    }
                    var increaseExposureAtNight by remember {
                        mutableStateOf(arPrefs.increaseExposureAtNight)
                    }
                    var adjustForPathElevation by remember {
                        mutableStateOf(arPrefs.adjustForPathElevation)
                    }
                    var beaconViewDistance by remember {
                        mutableStateOf(relative(Distance.meters(arPrefs.beaconViewDistance)))
                    }
                    var pathViewDistance by remember {
                        mutableStateOf(relative(Distance.meters(arPrefs.pathViewDistance)))
                    }

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
                            item("beacons_header") {
                                TrailSenseSectionHeader(title = stringResource(R.string.beacons))
                            }
                            item("beacons_visible") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.visible),
                                    checked = showBeaconLayer,
                                    onCheckedChange = {
                                        showBeaconLayer = it
                                        arPrefs.showBeaconLayer = it
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("beacon_view_distance") {
                                SettingsActionRow(
                                    label = stringResource(R.string.view_distance),
                                    summary = formatDistance(beaconViewDistance),
                                    onClick = {
                                        pickDistance(
                                            title = getString(R.string.view_distance),
                                            getDistance = { beaconViewDistance },
                                            setDistance = {
                                                beaconViewDistance = it
                                                arPrefs.beaconViewDistance = it.meters().value
                                            }
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            item("paths_header") {
                                TrailSenseSectionHeader(title = stringResource(R.string.paths))
                            }
                            item("paths_visible") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.visible),
                                    checked = showPathLayer,
                                    onCheckedChange = {
                                        showPathLayer = it
                                        arPrefs.showPathLayer = it
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("path_view_distance") {
                                SettingsActionRow(
                                    label = stringResource(R.string.view_distance),
                                    summary = formatDistance(pathViewDistance),
                                    onClick = {
                                        pickDistance(
                                            title = getString(R.string.view_distance),
                                            getDistance = { pathViewDistance },
                                            setDistance = {
                                                pathViewDistance = it
                                                arPrefs.pathViewDistance = it.meters().value
                                            },
                                            description = pathDistanceDescription
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            item("path_elevation") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.use_path_elevation),
                                    checked = adjustForPathElevation,
                                    onCheckedChange = {
                                        adjustForPathElevation = it
                                        arPrefs.adjustForPathElevation = it
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("astronomy_header") {
                                TrailSenseSectionHeader(title = stringResource(R.string.astronomy))
                            }
                            item("astronomy_visible") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.visible),
                                    checked = showAstronomyLayer,
                                    onCheckedChange = {
                                        showAstronomyLayer = it
                                        arPrefs.showAstronomyLayer = it
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("stars") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.show_stars),
                                    checked = showStars,
                                    onCheckedChange = {
                                        showStars = it
                                        arPrefs.showStars = it
                                    },
                                    enabled = showAstronomyLayer,
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("low_brightness") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.show_low_brightness_objects),
                                    checked = showLowBrightnessObjects,
                                    onCheckedChange = {
                                        showLowBrightnessObjects = it
                                        arPrefs.showLowBrightnessObjects = it
                                    },
                                    enabled = showAstronomyLayer && showStars,
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("constellations") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.show_constellations),
                                    checked = showConstellations,
                                    onCheckedChange = {
                                        showConstellations = it
                                        arPrefs.showConstellations = it
                                    },
                                    enabled = showAstronomyLayer,
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("below_horizon") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.show_below_horizon),
                                    checked = showAstronomyBelowHorizon,
                                    onCheckedChange = {
                                        showAstronomyBelowHorizon = it
                                        arPrefs.showAstronomyBelowHorizon = it
                                    },
                                    enabled = showAstronomyLayer,
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("grid_header") {
                                TrailSenseSectionHeader(title = stringResource(R.string.grid))
                            }
                            item("grid_visible") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.visible),
                                    checked = showGridLayer,
                                    onCheckedChange = {
                                        showGridLayer = it
                                        arPrefs.showGridLayer = it
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("advanced_header") {
                                TrailSenseSectionHeader(title = stringResource(R.string.advanced))
                            }
                            if (hasGyro) {
                                item("gyro_after_calibration") {
                                    SettingsSwitchRow(
                                        label = stringResource(R.string.use_gyroscope_after_calibration),
                                        summary = stringResource(R.string.use_gyroscope_after_calibration_description),
                                        checked = useGyroOnlyAfterCalibration,
                                        onCheckedChange = {
                                            useGyroOnlyAfterCalibration = it
                                            arPrefs.useGyroOnlyAfterCalibration = it
                                        },
                                        modifier = Modifier.padding(
                                            horizontal = TrailSenseSpacing.lg
                                        )
                                    )
                                    TrailSenseDivider()
                                }
                            }
                            item("increase_exposure") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.increase_exposure_at_night),
                                    checked = increaseExposureAtNight,
                                    onCheckedChange = {
                                        increaseExposureAtNight = it
                                        arPrefs.increaseExposureAtNight = it
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                        }
                    }
    }

    private fun pickDistance(
        title: String,
        getDistance: () -> Distance,
        setDistance: (Distance) -> Unit,
        description: String? = null
    ) {
        CustomUiUtils.pickDistance(
            requireContext(),
            distanceUnits,
            getDistance(),
            title,
            description = description
        ) { distance, cancelled ->
            if (cancelled || distance == null || distance.value <= 0) {
                return@pickDistance
            }

            setDistance(distance)
        }
    }

    private fun formatDistance(distance: Distance): String {
        return formatter.formatDistance(distance, Units.getDecimalPlaces(distance.units))
    }

    private fun relative(distance: Distance): Distance {
        return distance.convertTo(userPrefs.baseDistanceUnits).toRelativeDistance()
    }
}
