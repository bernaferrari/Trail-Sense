@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.navigation.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.SettingsActionRow
import com.kylecorry.trail_sense.settings.ui.SettingsPickerRow
import com.kylecorry.trail_sense.settings.ui.SettingsSwitchRow
import com.kylecorry.trail_sense.settings.ui.goBack
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.MapLayersSheetState
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseConnectedChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.navigation.NavigationToolRegistration
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

@Composable
fun NavigationSettingsToolScreen(host: TrailSenseDestinationHost) {
    val controller = remember(host) { NavigationSettingsController(host) }
    DisposableEffect(controller) {
        onDispose {
            controller.onDispose()
        }
    }
    NavigationSettingsContent(controller)
    controller.LayerSheet()
}

private class NavigationSettingsController(private val host: TrailSenseDestinationHost) {
    private val layerSheet = MapLayersSheetState(NavigationToolRegistration.MAP_ID)

    val cache by lazy { PreferencesSubsystem.getInstance(host.requireContext()).preferences }
    val userPrefs by lazy { UserPreferences(host.requireContext()) }
    val prefs by lazy { userPrefs.navigation }
    val formatter by lazy { FormatService.getInstance(host.requireContext()) }
    val hasCompass by lazy { SensorService(host.requireContext()).hasCompass() }
    val distanceUnits by lazy {
        formatter.sortDistanceUnits(DistanceUtils.hikingDistanceUnits)
    }

    fun onDispose() {
        layerSheet.dismiss()
    }

    fun goBack() {
        host.goBack()
    }

    fun showLayerSheet() {
        layerSheet.dismiss()
        layerSheet.show()
    }

    @Composable
    fun LayerSheet() {
        layerSheet.Content(
            host.requireContext(),
            { host.findNavController().navigateWithAnimation(it) }
        )
    }

    fun onCurrentPaceSpeedometerSelected() {
        host.requestActivityRecognition { hasPermission ->
            if (!hasPermission) {
                host.alertNoActivityRecognitionPermission()
            }
        }
    }

    fun pickOption(
        title: String,
        options: List<NavigationOption>,
        selected: String,
        onSelected: (String) -> Unit
    ) {
        Pickers.item(
            host.requireContext(),
            title,
            options.map { it.label },
            options.indexOfFirst { it.value == selected }.coerceAtLeast(0)
        ) { index ->
            if (index != null) {
                onSelected(options[index].value)
            }
        }
    }

    fun pickVisibleBeaconCount(current: Int, onPicked: (Int) -> Unit) {
        Pickers.number(
            host.requireContext(),
            host.getString(R.string.pref_num_visible_beacons_title),
            null,
            current,
            allowDecimals = false,
            allowNegative = false
        ) { value ->
            if (value != null) {
                val count = value.toInt()
                putString(R.string.pref_num_visible_beacons, count.toString())
                onPicked(count)
            }
        }
    }

    fun pickNearbyRadius(current: Distance, onPicked: (Distance) -> Unit) {
        CustomUiUtils.pickDistance(
            host.requireContext(),
            distanceUnits,
            current,
            host.getString(R.string.pref_max_beacon_distance_title)
        ) { distance, cancelled ->
            if (cancelled || distance == null || distance.value <= 0) {
                return@pickDistance
            }

            prefs.maxBeaconDistance = distance.meters().value
            onPicked(relative(Distance.meters(prefs.maxBeaconDistance)))
        }
    }

    fun pickBearingLength(current: Distance, onPicked: (Distance) -> Unit) {
        CustomUiUtils.pickDistance(
            host.requireContext(),
            distanceUnits,
            current,
            host.getString(R.string.bearing_length)
        ) { distance, cancelled ->
            if (cancelled || distance == null || distance.value <= 0) {
                return@pickDistance
            }

            prefs.bearingDistance = distance
            onPicked(relative(prefs.bearingDistance))
        }
    }

    fun options(entriesId: Int, valuesId: Int): List<NavigationOption> {
        val entries = host.getResources().getStringArray(entriesId)
        val values = host.getResources().getStringArray(valuesId)
        return values.mapIndexed { index, value ->
            NavigationOption(value, entries.getOrElse(index) { value })
        }
    }

    fun optionLabel(options: List<NavigationOption>, selected: String): String {
        return options.firstOrNull { it.value == selected }?.label ?: selected
    }

    fun formatDistance(distance: Distance): String {
        return formatter.formatDistance(distance, Units.getDecimalPlaces(distance.units))
    }

    fun relative(distance: Distance): Distance {
        return distance.convertTo(userPrefs.baseDistanceUnits).toRelativeDistance()
    }

    fun getStringPreference(key: Int, default: String): String {
        return cache.getString(host.getString(key)) ?: default
    }

    fun getBoolean(key: Int, defaultValue: Boolean = false): Boolean {
        return cache.getBoolean(host.getString(key)) ?: defaultValue
    }

    fun putString(key: Int, value: String) {
        cache.putString(host.getString(key), value)
    }

    fun putBoolean(key: Int, value: Boolean) {
        cache.putBoolean(host.getString(key), value)
    }
}

private data class NavigationOption(
    val value: String,
    val label: String
)

@Composable
private fun NavigationSettingsContent(controller: NavigationSettingsController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = controller.prefs
    val hasCompass = controller.hasCompass
    val quickActionOptions = remember {
        Tools.getQuickActions(context).map { NavigationOption(it.id.toString(), it.name) }
    }
    val speedometerOptions = remember {
        controller.options(R.array.speedometer_entries, R.array.speedometer_values)
    }
    val sunMoonOptions = remember {
        controller.options(R.array.sun_moon_compass_entries, R.array.sun_moon_compass_values)
    }

    var leftQuickAction by remember { mutableStateOf(prefs.leftButton.toString()) }
    var rightQuickAction by remember { mutableStateOf(prefs.rightButton.toString()) }
    var showCalibrationDialog by remember {
        mutableStateOf(controller.getBoolean(R.string.pref_show_calibrate_on_navigate_dialog, true))
    }
    var keepScreenUnlockedWhileNavigating by remember {
        mutableStateOf(prefs.keepScreenUnlockedWhileNavigating)
    }
    var keepScreenUnlockedWhileOpen by remember { mutableStateOf(prefs.keepScreenUnlockedWhileOpen) }
    var lockBearingToLocation by remember { mutableStateOf(prefs.lockBearingToLocation) }
    var speedometerType by remember {
        mutableStateOf(controller.getStringPreference(R.string.pref_navigation_speedometer_type, "instant"))
    }
    var showLinearCompass by remember {
        mutableStateOf(controller.getBoolean(R.string.pref_show_linear_compass, true))
    }
    var showDialTicksWhenNoCompass by remember { mutableStateOf(prefs.showDialTicksWhenNoCompass) }
    var showSunMoonCompass by remember {
        mutableStateOf(controller.getStringPreference(R.string.pref_show_sun_moon_compass, "when_up"))
    }
    var displayMultipleBeacons by remember {
        mutableStateOf(if (hasCompass) prefs.showMultipleBeacons else true)
    }
    var numberOfVisibleBeacons by remember { mutableStateOf(prefs.numberOfVisibleBeacons) }
    var nearbyRadius by remember {
        mutableStateOf(controller.relative(Distance.meters(prefs.maxBeaconDistance)))
    }
    var nearbyLinearOnly by remember { mutableStateOf(prefs.showNearbyBeaconsOnlyOnLinearCompass) }
    var drawCompassDialBezel by remember { mutableStateOf(prefs.drawDialBezel) }
    var showAzimuthIndicator by remember { mutableStateOf(prefs.showAzimuthIndicator) }
    var highDetailMode by remember { mutableStateOf(prefs.highDetailMode) }
    var bearingLength by remember { mutableStateOf(controller.relative(prefs.bearingDistance)) }
    val maxBearingLength = remember {
        controller.formatDistance(controller.relative(Destination.Bearing.maxBearingDistance))
    }

    LaunchedEffect(hasCompass) {
        if (!hasCompass) {
            displayMultipleBeacons = true
            controller.putBoolean(R.string.pref_display_multi_beacons, true)
        }
    }

    val generalTitle = stringResource(R.string.general)
    val navigationTitle = stringResource(R.string.navigation)
    val compassTitle = stringResource(R.string.pref_compass_sensor_title)
    val beaconsTitle = stringResource(R.string.beacons)
    val mapTitle = stringResource(R.string.map)
    val sunMoonTitle = stringResource(R.string.pref_show_sun_moon_compass_title)
    val bearingFooter = stringResource(R.string.bearing_length_description) + " " +
            stringResource(R.string.bearing_length_max, maxBearingLength)
    val sunMoonChoices = sunMoonOptions.map { TrailSenseChoice(it.value, it.label) }

    TrailSensePage(
        title = navigationTitle,
        onBack = controller::goBack,
        backLabel = stringResource(R.string.back)
    ) {
        trailSenseSection("quick_actions", title = generalTitle) {
            row { position ->
                SettingsPickerRow(
                    label = stringResource(R.string.left_quick_action),
                    selected = controller.optionLabel(quickActionOptions, leftQuickAction),
                    onClick = {
                        controller.pickOption(
                            context.getString(R.string.left_quick_action),
                            quickActionOptions,
                            leftQuickAction
                        ) {
                            leftQuickAction = it
                            controller.putString(R.string.pref_navigation_quick_action_left, it)
                        }
                    },
                    position = position
                )
            }
            row { position ->
                SettingsPickerRow(
                    label = stringResource(R.string.right_quick_action),
                    selected = controller.optionLabel(quickActionOptions, rightQuickAction),
                    onClick = {
                        controller.pickOption(
                            context.getString(R.string.right_quick_action),
                            quickActionOptions,
                            rightQuickAction
                        ) {
                            rightQuickAction = it
                            controller.putString(R.string.pref_navigation_quick_action_right, it)
                        }
                    },
                    position = position
                )
            }
        }

        trailSenseSection("navigation", title = navigationTitle, footer = bearingFooter) {
            if (hasCompass) {
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_show_calibrate_on_navigate_dialog_title),
                        checked = showCalibrationDialog,
                        onCheckedChange = {
                            showCalibrationDialog = it
                            controller.putBoolean(R.string.pref_show_calibrate_on_navigate_dialog, it)
                        },
                        position = position
                    )
                }
            }
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.pref_navigation_lock_screen_presence_title),
                    checked = keepScreenUnlockedWhileNavigating,
                    onCheckedChange = {
                        keepScreenUnlockedWhileNavigating = it
                        controller.putBoolean(R.string.pref_navigation_lock_screen_presence, it)
                    },
                    position = position
                )
            }
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.pref_navigation_lock_screen_when_open_title),
                    checked = keepScreenUnlockedWhileOpen,
                    onCheckedChange = {
                        keepScreenUnlockedWhileOpen = it
                        controller.putBoolean(R.string.pref_navigation_keep_unlocked, it)
                    },
                    position = position
                )
            }
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.lock_bearing_to_location),
                    summary = stringResource(R.string.lock_bearing_to_location_description),
                    checked = lockBearingToLocation,
                    onCheckedChange = {
                        lockBearingToLocation = it
                        prefs.lockBearingToLocation = it
                    },
                    position = position
                )
            }
            row { position ->
                SettingsPickerRow(
                    label = stringResource(R.string.speedometer),
                    selected = controller.optionLabel(speedometerOptions, speedometerType),
                    onClick = {
                        controller.pickOption(
                            context.getString(R.string.speedometer),
                            speedometerOptions,
                            speedometerType
                        ) {
                            speedometerType = it
                            controller.putString(R.string.pref_navigation_speedometer_type, it)
                            if (it == "instant_pedometer") {
                                controller.onCurrentPaceSpeedometerSelected()
                            }
                        }
                    },
                    position = position
                )
            }
            row { position ->
                SettingsPickerRow(
                    label = stringResource(R.string.bearing_length),
                    selected = controller.formatDistance(bearingLength),
                    onClick = { controller.pickBearingLength(bearingLength) { bearingLength = it } },
                    position = position
                )
            }
        }

        trailSenseSection("compass", title = compassTitle) {
            if (hasCompass) {
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_show_linear_compass_title),
                        summary = stringResource(R.string.pref_show_linear_compass_summary),
                        checked = showLinearCompass,
                        onCheckedChange = {
                            showLinearCompass = it
                            controller.putBoolean(R.string.pref_show_linear_compass, it)
                        },
                        position = position
                    )
                }
            } else {
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.show_dial_ticks_on_nearby_radar),
                        summary = stringResource(R.string.show_dial_ticks_on_nearby_radar_summary),
                        checked = showDialTicksWhenNoCompass,
                        onCheckedChange = {
                            showDialTicksWhenNoCompass = it
                            controller.putBoolean(R.string.pref_show_dial_ticks_when_no_compass, it)
                        },
                        position = position
                    )
                }
            }
            row { position ->
                TrailSenseGroupCard(position = position) {
                    Text(
                        text = sunMoonTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = TrailSenseSpacing.md)
                    )
                    TrailSenseConnectedChoice(
                        options = sunMoonChoices,
                        selected = showSunMoonCompass,
                        label = sunMoonTitle,
                        onSelect = {
                            showSunMoonCompass = it
                            controller.putString(R.string.pref_show_sun_moon_compass, it)
                        }
                    )
                }
            }
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.pref_navigation_draw_compass_dial_bezel_title),
                    summary = stringResource(R.string.pref_navigation_draw_compass_dial_bezel_summary),
                    checked = drawCompassDialBezel,
                    onCheckedChange = {
                        drawCompassDialBezel = it
                        controller.putBoolean(R.string.pref_navigation_draw_compass_dial_bezel, it)
                    },
                    position = position
                )
            }
            if (hasCompass) {
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_navigation_show_azimuth_indicator_title),
                        summary = stringResource(R.string.pref_navigation_show_azimuth_indicator_summary),
                        checked = showAzimuthIndicator,
                        onCheckedChange = {
                            showAzimuthIndicator = it
                            controller.putBoolean(R.string.pref_navigation_show_azimuth_indicator, it)
                        },
                        position = position
                    )
                }
            }
        }

        if (hasCompass) {
            trailSenseSection("beacons", title = beaconsTitle) {
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_display_multi_beacons_title),
                        summary = stringResource(R.string.pref_display_multi_beacons_summary),
                        checked = displayMultipleBeacons,
                        onCheckedChange = {
                            displayMultipleBeacons = it
                            controller.putBoolean(R.string.pref_display_multi_beacons, it)
                        },
                        position = position
                    )
                }
                if (displayMultipleBeacons) {
                    row { position ->
                        SettingsPickerRow(
                            label = stringResource(R.string.pref_num_visible_beacons_title),
                            selected = numberOfVisibleBeacons.toString(),
                            onClick = {
                                controller.pickVisibleBeaconCount(numberOfVisibleBeacons) {
                                    numberOfVisibleBeacons = it
                                }
                            },
                            position = position
                        )
                    }
                    row { position ->
                        SettingsPickerRow(
                            label = stringResource(R.string.pref_max_beacon_distance_title),
                            selected = controller.formatDistance(nearbyRadius),
                            onClick = { controller.pickNearbyRadius(nearbyRadius) { nearbyRadius = it } },
                            position = position
                        )
                    }
                    row { position ->
                        SettingsSwitchRow(
                            label = stringResource(R.string.pref_nearby_linear_only_title),
                            checked = nearbyLinearOnly,
                            onCheckedChange = {
                                nearbyLinearOnly = it
                                controller.putBoolean(R.string.pref_nearby_linear_only, it)
                            },
                            position = position
                        )
                    }
                }
            }
        }

        trailSenseSection("map", title = mapTitle) {
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.pref_high_detail_mode_title),
                    summary = stringResource(R.string.pref_high_detail_mode_summary),
                    checked = highDetailMode,
                    onCheckedChange = {
                        highDetailMode = it
                        controller.putBoolean(R.string.pref_navigation_high_detail_mode, it)
                    },
                    position = position
                )
            }
            row { position ->
                SettingsActionRow(
                    label = stringResource(R.string.layers),
                    onClick = controller::showLayerSheet,
                    position = position
                )
            }
        }
    }
}
