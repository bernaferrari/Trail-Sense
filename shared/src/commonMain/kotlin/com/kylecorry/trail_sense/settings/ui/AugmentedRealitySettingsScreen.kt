package com.kylecorry.trail_sense.settings.ui

import com.kylecorry.trail_sense.shared.device.DeviceProfile

import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.generated.resources.*

import com.kylecorry.trail_sense.shared.dialogs.UnitPickers
import com.kylecorry.trail_sense.shared.views.compose.AppHost



import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection

@Composable
fun AugmentedRealitySettingsToolScreen() {
    val userPrefs = remember { getAppService<UserPreferences>() }
    val formatter = remember { FormatService.getInstance() }
    val distanceUnits = remember {
        formatter.sortDistanceUnits(DistanceUtils.hikingDistanceUnits)
    }
    val arPrefs = userPrefs.augmentedReality
    val hasGyro = remember { getAppService<DeviceProfile>().hasGyroscope }
    val pathDistanceDescription = remember {
        getAppService<AppStrings>().get(Res.string.path_distance_performance_disclaimer,
            formatter.formatDistance(
                relativeDistance(
                    userPrefs,
                    Distance.meters(arPrefs.maxRecommendedPathViewDistanceMeters)
                )
            )
        )
    }

    var showBeaconLayer by remember { mutableStateOf(arPrefs.showBeaconLayer) }
    var showPathLayer by remember { mutableStateOf(arPrefs.showPathLayer) }
    var showAstronomyLayer by remember { mutableStateOf(arPrefs.showAstronomyLayer) }
    var showStars by remember { mutableStateOf(arPrefs.showStars) }
    var showLowBrightnessObjects by remember {
        mutableStateOf(arPrefs.showLowBrightnessObjects)
    }
    var showConstellations by remember { mutableStateOf(arPrefs.showConstellations) }
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
        mutableStateOf(relativeDistance(userPrefs, Distance.meters(arPrefs.beaconViewDistance)))
    }
    var pathViewDistance by remember {
        mutableStateOf(relativeDistance(userPrefs, Distance.meters(arPrefs.pathViewDistance)))
    }

    val beaconsTitle = appString(Res.string.beacons)
    val pathsTitle = appString(Res.string.paths)
    val astronomyTitle = appString(Res.string.astronomy)
    val gridTitle = appString(Res.string.grid)
    val advancedTitle = appString(Res.string.advanced)

    // Each layer's own options only show while the layer does.
    SettingsList(title = appString(Res.string.augmented_reality)) {
        trailSenseSection("beacons", title = beaconsTitle) {
            row { position ->
                SettingsSwitchRow(
                    label = appString(Res.string.visible),
                    checked = showBeaconLayer,
                    onCheckedChange = {
                        showBeaconLayer = it
                        arPrefs.showBeaconLayer = it
                    },
                    position = position
                )
            }
            if (showBeaconLayer) {
                row { position ->
                    SettingsActionRow(
                        label = appString(Res.string.view_distance),
                        summary = formatDistance(formatter, beaconViewDistance),
                        onClick = {
                            pickDistance(
                                distanceUnits,
                                title = getAppService<AppStrings>().get(Res.string.view_distance),
                                current = beaconViewDistance
                            ) {
                                beaconViewDistance = it
                                arPrefs.beaconViewDistance = it.meters().value
                            }
                        },
                        position = position
                    )
                }
            }
        }
        trailSenseSection("paths", title = pathsTitle) {
            row { position ->
                SettingsSwitchRow(
                    label = appString(Res.string.visible),
                    checked = showPathLayer,
                    onCheckedChange = {
                        showPathLayer = it
                        arPrefs.showPathLayer = it
                    },
                    position = position
                )
            }
            if (showPathLayer) {
                row { position ->
                    SettingsActionRow(
                        label = appString(Res.string.view_distance),
                        summary = formatDistance(formatter, pathViewDistance),
                        onClick = {
                            pickDistance(
                                distanceUnits,
                                title = getAppService<AppStrings>().get(Res.string.view_distance),
                                current = pathViewDistance,
                                description = pathDistanceDescription
                            ) {
                                pathViewDistance = it
                                arPrefs.pathViewDistance = it.meters().value
                            }
                        },
                        position = position
                    )
                }
                row { position ->
                    SettingsSwitchRow(
                        label = appString(Res.string.use_path_elevation),
                        checked = adjustForPathElevation,
                        onCheckedChange = {
                            adjustForPathElevation = it
                            arPrefs.adjustForPathElevation = it
                        },
                        position = position
                    )
                }
            }
        }
        trailSenseSection("astronomy", title = astronomyTitle) {
            row { position ->
                SettingsSwitchRow(
                    label = appString(Res.string.visible),
                    checked = showAstronomyLayer,
                    onCheckedChange = {
                        showAstronomyLayer = it
                        arPrefs.showAstronomyLayer = it
                    },
                    position = position
                )
            }
            if (showAstronomyLayer) {
                row { position ->
                    SettingsSwitchRow(
                        label = appString(Res.string.show_stars),
                        checked = showStars,
                        onCheckedChange = {
                            showStars = it
                            arPrefs.showStars = it
                        },
                        position = position
                    )
                }
                if (showStars) {
                    row { position ->
                        SettingsSwitchRow(
                            label = appString(Res.string.show_low_brightness_objects),
                            checked = showLowBrightnessObjects,
                            onCheckedChange = {
                                showLowBrightnessObjects = it
                                arPrefs.showLowBrightnessObjects = it
                            },
                            position = position
                        )
                    }
                }
                row { position ->
                    SettingsSwitchRow(
                        label = appString(Res.string.show_constellations),
                        checked = showConstellations,
                        onCheckedChange = {
                            showConstellations = it
                            arPrefs.showConstellations = it
                        },
                        position = position
                    )
                }
                row { position ->
                    SettingsSwitchRow(
                        label = appString(Res.string.show_below_horizon),
                        checked = showAstronomyBelowHorizon,
                        onCheckedChange = {
                            showAstronomyBelowHorizon = it
                            arPrefs.showAstronomyBelowHorizon = it
                        },
                        position = position
                    )
                }
            }
        }
        trailSenseSection("grid", title = gridTitle) {
            row { position ->
                SettingsSwitchRow(
                    label = appString(Res.string.visible),
                    checked = showGridLayer,
                    onCheckedChange = {
                        showGridLayer = it
                        arPrefs.showGridLayer = it
                    },
                    position = position
                )
            }
        }
        trailSenseSection("advanced", title = advancedTitle) {
            if (hasGyro) {
                row { position ->
                    SettingsSwitchRow(
                        label = appString(Res.string.use_gyroscope_after_calibration),
                        summary = appString(Res.string.use_gyroscope_after_calibration_description),
                        checked = useGyroOnlyAfterCalibration,
                        onCheckedChange = {
                            useGyroOnlyAfterCalibration = it
                            arPrefs.useGyroOnlyAfterCalibration = it
                        },
                        position = position
                    )
                }
            }
            row { position ->
                SettingsSwitchRow(
                    label = appString(Res.string.increase_exposure_at_night),
                    checked = increaseExposureAtNight,
                    onCheckedChange = {
                        increaseExposureAtNight = it
                        arPrefs.increaseExposureAtNight = it
                    },
                    position = position
                )
            }
        }
    }
}

private fun pickDistance(
    units: List<DistanceUnits>,
    title: String,
    current: Distance,
    description: String? = null,
    onPicked: (Distance) -> Unit
) {
    UnitPickers.distance(
        AppHost.current(),
        units,
        current,
        title,
        description = description
    ) { distance, cancelled ->
        if (cancelled || distance == null || distance.value <= 0) {
            return@distance
        }

        onPicked(distance)
    }
}

private fun relativeDistance(prefs: UserPreferences, distance: Distance): Distance {
    return distance.convertTo(prefs.baseDistanceUnits).toRelativeDistance()
}
