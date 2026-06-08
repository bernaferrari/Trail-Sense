@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.navigation.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.SettingsActionRow
import com.kylecorry.trail_sense.settings.ui.SettingsPickerRow
import com.kylecorry.trail_sense.settings.ui.SettingsSwitchRow
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.MapLayersSheetState
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.permissions.alertNoActivityRecognitionPermission
import com.kylecorry.trail_sense.shared.permissions.requestActivityRecognition
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.navigation.NavigationToolRegistration
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

@Composable
fun NavigationSettingsToolScreen(fragment: TrailSenseComposeDestinationFragment) {
    val controller = remember(fragment) { NavigationSettingsController(fragment) }
    DisposableEffect(controller) {
        onDispose {
            controller.onDispose()
        }
    }
    NavigationSettingsContent(controller)
    controller.LayerSheet()
}

private class NavigationSettingsController(private val fragment: TrailSenseComposeDestinationFragment) {
    private val layerSheet = MapLayersSheetState(NavigationToolRegistration.MAP_ID)

    val cache by lazy { PreferencesSubsystem.getInstance(fragment.requireContext()).preferences }
    val userPrefs by lazy { UserPreferences(fragment.requireContext()) }
    val prefs by lazy { userPrefs.navigation }
    val formatter by lazy { FormatService.getInstance(fragment.requireContext()) }
    val hasCompass by lazy { SensorService(fragment.requireContext()).hasCompass() }
    val distanceUnits by lazy {
        formatter.sortDistanceUnits(DistanceUtils.hikingDistanceUnits)
    }

    fun onDispose() {
        layerSheet.dismiss()
    }

    fun showLayerSheet() {
        layerSheet.dismiss()
        layerSheet.show()
    }

    @Composable
    fun LayerSheet() {
        layerSheet.Content(
            fragment.requireContext(),
            { fragment.findNavController().navigateWithAnimation(it) }
        )
    }

    fun onCurrentPaceSpeedometerSelected() {
        fragment.requestActivityRecognition { hasPermission ->
            if (!hasPermission) {
                fragment.alertNoActivityRecognitionPermission()
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
            fragment.requireContext(),
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
            fragment.requireContext(),
            fragment.getString(R.string.pref_num_visible_beacons_title),
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
            fragment.requireContext(),
            distanceUnits,
            current,
            fragment.getString(R.string.pref_max_beacon_distance_title)
        ) { distance, cancelled ->
            if (cancelled || distance == null || distance.value <= 0) {
                return@pickDistance
            }

            prefs.maxBeaconDistance = distance.meters().value
            onPicked(relative(Distance.meters(prefs.maxBeaconDistance)))
        }
    }

    fun options(entriesId: Int, valuesId: Int): List<NavigationOption> {
        val entries = fragment.resources.getStringArray(entriesId)
        val values = fragment.resources.getStringArray(valuesId)
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
        return cache.getString(fragment.getString(key)) ?: default
    }

    fun getBoolean(key: Int, defaultValue: Boolean = false): Boolean {
        return cache.getBoolean(fragment.getString(key)) ?: defaultValue
    }

    fun putString(key: Int, value: String) {
        cache.putString(fragment.getString(key), value)
    }

    fun putBoolean(key: Int, value: Boolean) {
        cache.putBoolean(fragment.getString(key), value)
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
                        Tools.getQuickActions(context).map {
                            NavigationOption(it.id.toString(), it.name)
                        }
                    }
                    val speedometerOptions = remember {
                        controller.options(R.array.speedometer_entries, R.array.speedometer_values)
                    }
                    val sunMoonOptions = remember {
                        controller.options(R.array.sun_moon_compass_entries, R.array.sun_moon_compass_values)
                    }

                    var leftQuickAction by remember {
                        mutableStateOf(prefs.leftButton.toString())
                    }
                    var rightQuickAction by remember {
                        mutableStateOf(prefs.rightButton.toString())
                    }
                    var showCalibrationDialog by remember {
                        mutableStateOf(
                            controller.getBoolean(R.string.pref_show_calibrate_on_navigate_dialog, true)
                        )
                    }
                    var keepScreenUnlockedWhileNavigating by remember {
                        mutableStateOf(prefs.keepScreenUnlockedWhileNavigating)
                    }
                    var keepScreenUnlockedWhileOpen by remember {
                        mutableStateOf(prefs.keepScreenUnlockedWhileOpen)
                    }
                    var lockBearingToLocation by remember {
                        mutableStateOf(prefs.lockBearingToLocation)
                    }
                    var speedometerType by remember {
                        mutableStateOf(
                            controller.getStringPreference(R.string.pref_navigation_speedometer_type, "instant")
                        )
                    }
                    var showLinearCompass by remember {
                        mutableStateOf(controller.getBoolean(R.string.pref_show_linear_compass, true))
                    }
                    var showDialTicksWhenNoCompass by remember {
                        mutableStateOf(prefs.showDialTicksWhenNoCompass)
                    }
                    var showSunMoonCompass by remember {
                        mutableStateOf(
                            controller.getStringPreference(R.string.pref_show_sun_moon_compass, "when_up")
                        )
                    }
                    var displayMultipleBeacons by remember {
                        mutableStateOf(if (hasCompass) prefs.showMultipleBeacons else true)
                    }
                    var numberOfVisibleBeacons by remember {
                        mutableStateOf(prefs.numberOfVisibleBeacons)
                    }
                    var nearbyRadius by remember {
                        mutableStateOf(controller.relative(Distance.meters(prefs.maxBeaconDistance)))
                    }
                    var nearbyLinearOnly by remember {
                        mutableStateOf(prefs.showNearbyBeaconsOnlyOnLinearCompass)
                    }
                    var drawCompassDialBezel by remember {
                        mutableStateOf(prefs.drawDialBezel)
                    }
                    var showAzimuthIndicator by remember {
                        mutableStateOf(prefs.showAzimuthIndicator)
                    }
                    var highDetailMode by remember {
                        mutableStateOf(prefs.highDetailMode)
                    }

                    LaunchedEffect(hasCompass) {
                        if (!hasCompass) {
                            displayMultipleBeacons = true
                            controller.putBoolean(R.string.pref_display_multi_beacons, true)
                        }
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
                            item("general_header") {
                                TrailSenseSectionHeader(title = stringResource(R.string.general))
                            }
                            item("left_action") {
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
                                            controller.putString(
                                                R.string.pref_navigation_quick_action_left,
                                                it
                                            )
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            item("right_action") {
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
                                            controller.putString(
                                                R.string.pref_navigation_quick_action_right,
                                                it
                                            )
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            item("navigation_header") {
                                TrailSenseSectionHeader(title = stringResource(R.string.navigation))
                            }
                            if (hasCompass) {
                                item("calibrate_dialog") {
                                    SettingsSwitchRow(
                                        label = stringResource(R.string.pref_show_calibrate_on_navigate_dialog_title),
                                        checked = showCalibrationDialog,
                                        onCheckedChange = {
                                            showCalibrationDialog = it
                                            controller.putBoolean(
                                                R.string.pref_show_calibrate_on_navigate_dialog,
                                                it
                                            )
                                        },
                                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                    )
                                    TrailSenseDivider()
                                }
                            }
                            item("lock_screen_navigation") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.pref_navigation_lock_screen_presence_title),
                                    checked = keepScreenUnlockedWhileNavigating,
                                    onCheckedChange = {
                                        keepScreenUnlockedWhileNavigating = it
                                        controller.putBoolean(
                                            R.string.pref_navigation_lock_screen_presence,
                                            it
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("lock_screen_open") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.pref_navigation_lock_screen_when_open_title),
                                    checked = keepScreenUnlockedWhileOpen,
                                    onCheckedChange = {
                                        keepScreenUnlockedWhileOpen = it
                                        controller.putBoolean(R.string.pref_navigation_keep_unlocked, it)
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("lock_bearing_to_location") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.lock_bearing_to_location),
                                    summary = stringResource(R.string.lock_bearing_to_location_description),
                                    checked = lockBearingToLocation,
                                    onCheckedChange = {
                                        lockBearingToLocation = it
                                        prefs.lockBearingToLocation = it
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("speedometer") {
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
                                            controller.putString(
                                                R.string.pref_navigation_speedometer_type,
                                                it
                                            )
                                            if (it == "instant_pedometer") {
                                                controller.onCurrentPaceSpeedometerSelected()
                                            }
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            item("compass_header") {
                                TrailSenseSectionHeader(
                                    title = stringResource(R.string.pref_compass_sensor_title)
                                )
                            }
                            if (hasCompass) {
                                item("linear_compass") {
                                    SettingsSwitchRow(
                                        label = stringResource(R.string.pref_show_linear_compass_title),
                                        summary = stringResource(R.string.pref_show_linear_compass_summary),
                                        checked = showLinearCompass,
                                        onCheckedChange = {
                                            showLinearCompass = it
                                            controller.putBoolean(R.string.pref_show_linear_compass, it)
                                        },
                                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                    )
                                    TrailSenseDivider()
                                }
                            } else {
                                item("dial_ticks_no_compass") {
                                    SettingsSwitchRow(
                                        label = stringResource(R.string.show_dial_ticks_on_nearby_radar),
                                        summary = stringResource(R.string.show_dial_ticks_on_nearby_radar_summary),
                                        checked = showDialTicksWhenNoCompass,
                                        onCheckedChange = {
                                            showDialTicksWhenNoCompass = it
                                            controller.putBoolean(
                                                R.string.pref_show_dial_ticks_when_no_compass,
                                                it
                                            )
                                        },
                                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                    )
                                    TrailSenseDivider()
                                }
                            }
                            item("sun_moon_compass") {
                                SettingsPickerRow(
                                    label = stringResource(R.string.pref_show_sun_moon_compass_title),
                                    selected = controller.optionLabel(sunMoonOptions, showSunMoonCompass),
                                    onClick = {
                                        controller.pickOption(
                                            context.getString(R.string.pref_show_sun_moon_compass_title),
                                            sunMoonOptions,
                                            showSunMoonCompass
                                        ) {
                                            showSunMoonCompass = it
                                            controller.putString(R.string.pref_show_sun_moon_compass, it)
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            if (hasCompass) {
                                item("multi_beacons") {
                                    SettingsSwitchRow(
                                        label = stringResource(R.string.pref_display_multi_beacons_title),
                                        summary = stringResource(R.string.pref_display_multi_beacons_summary),
                                        checked = displayMultipleBeacons,
                                        onCheckedChange = {
                                            displayMultipleBeacons = it
                                            controller.putBoolean(R.string.pref_display_multi_beacons, it)
                                        },
                                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                    )
                                    TrailSenseDivider()
                                }
                                item("visible_beacons") {
                                    SettingsPickerRow(
                                        label = stringResource(R.string.pref_num_visible_beacons_title),
                                        selected = numberOfVisibleBeacons.toString(),
                                        enabled = displayMultipleBeacons,
                                        onClick = {
                                            controller.pickVisibleBeaconCount(numberOfVisibleBeacons) {
                                                numberOfVisibleBeacons = it
                                            }
                                        },
                                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                    )
                                }
                                item("nearby_radius") {
                                    SettingsPickerRow(
                                        label = stringResource(R.string.pref_max_beacon_distance_title),
                                    selected = controller.formatDistance(nearbyRadius),
                                    enabled = displayMultipleBeacons,
                                    onClick = {
                                            controller.pickNearbyRadius(nearbyRadius) {
                                                nearbyRadius = it
                                            }
                                        },
                                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                    )
                                }
                                item("nearby_linear_only") {
                                    SettingsSwitchRow(
                                        label = stringResource(R.string.pref_nearby_linear_only_title),
                                        checked = nearbyLinearOnly,
                                        enabled = displayMultipleBeacons,
                                        onCheckedChange = {
                                            nearbyLinearOnly = it
                                            controller.putBoolean(R.string.pref_nearby_linear_only, it)
                                        },
                                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                    )
                                    TrailSenseDivider()
                                }
                            }
                            item("dial_bezel") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.pref_navigation_draw_compass_dial_bezel_title),
                                    summary = stringResource(R.string.pref_navigation_draw_compass_dial_bezel_summary),
                                    checked = drawCompassDialBezel,
                                    onCheckedChange = {
                                        drawCompassDialBezel = it
                                        controller.putBoolean(
                                            R.string.pref_navigation_draw_compass_dial_bezel,
                                            it
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            if (hasCompass) {
                                item("azimuth_indicator") {
                                    SettingsSwitchRow(
                                        label = stringResource(R.string.pref_navigation_show_azimuth_indicator_title),
                                        summary = stringResource(R.string.pref_navigation_show_azimuth_indicator_summary),
                                        checked = showAzimuthIndicator,
                                        onCheckedChange = {
                                            showAzimuthIndicator = it
                                            controller.putBoolean(
                                                R.string.pref_navigation_show_azimuth_indicator,
                                                it
                                            )
                                        },
                                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                    )
                                    TrailSenseDivider()
                                }
                            }
                            item("map_header") {
                                TrailSenseSectionHeader(title = stringResource(R.string.map))
                            }
                            item("high_detail") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.pref_high_detail_mode_title),
                                    summary = stringResource(R.string.pref_high_detail_mode_summary),
                                    checked = highDetailMode,
                                    onCheckedChange = {
                                        highDetailMode = it
                                        controller.putBoolean(R.string.pref_navigation_high_detail_mode, it)
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("layers") {
                                SettingsActionRow(
                                    label = stringResource(R.string.layers),
                                    onClick = controller::showLayerSheet,
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                        }
                    }
}
