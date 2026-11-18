@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.navigation.ui

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.appString

import com.kylecorry.trail_sense.main.getAppService
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.kylecorry.trail_sense.shared.navigation.rememberSystemBack
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.ViewModel
import com.kylecorry.trail_sense.settings.ui.SettingsOption
import com.kylecorry.trail_sense.settings.ui.rememberSettingsOptions
import com.kylecorry.trail_sense.shared.dialogs.UnitPickers
import com.kylecorry.trail_sense.shared.text.LocalAppStrings
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.shared.views.compose.AppPermission
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.tools.navigation.infrastructure.NavigationPreferences
import kotlinx.coroutines.launch
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.settings.ui.SettingsActionRow
import com.kylecorry.trail_sense.settings.ui.SettingsPickerRow
import com.kylecorry.trail_sense.settings.ui.SettingsSwitchRow
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseConnectedChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.navigation.domain.Destination

/**
 * The navigation settings page. [quickActionOptions] are the quick actions the platform offers; [onShowLayers] opens
 * the platform's map layer sheet.
 */
@Composable
fun NavigationSettingsScreen(
    quickActionOptions: List<SettingsOption>,
    onShowLayers: () -> Unit
) {
    val effects = LocalScreenEffects.current
    val scope = rememberCoroutineScope()
    val backDispatcher = rememberSystemBack()
    val viewModel = viewModel {
        val userPrefs = getAppService<UserPreferences>()
        NavigationSettingsViewModel(
            prefs = userPrefs.navigation,
            cache = getAppService<Preferences>(),
            keys = NavigationSettingsKeys.create(),
            hasCompass = getAppService<SensorService>().hasCompass(),
            baseDistanceUnits = userPrefs.baseDistanceUnits
        )
    }
    val formatter = remember { FormatService.getInstance() }
    val distanceUnits = remember(formatter) { formatter.sortDistanceUnits(DistanceUtils.hikingDistanceUnits) }
    val host = AppHost.current()

    NavigationSettingsContent(
        viewModel = viewModel,
        quickActionOptions = quickActionOptions,
        formatDistance = { formatter.formatDistance(it, Units.getDecimalPlaces(it.units)) },
        onPickDistance = { current, title, onPicked ->
            UnitPickers.distance(host, distanceUnits, current, title) { distance, cancelled ->
                if (!cancelled && distance != null && distance.value > 0) {
                    onPicked(distance)
                }
            }
        },
        onCurrentPaceSpeedometerSelected = {
            scope.launch {
                if (!effects.requestPermission(AppPermission.ActivityRecognition)) {
                    effects.explainPermissionDenied(AppPermission.ActivityRecognition)
                }
            }
        },
        onShowLayers = onShowLayers,
        onBack = { backDispatcher?.invoke() }
    )
}

/** The preference keys the page writes, resolved once from the resources so the ViewModel never touches them. */
data class NavigationSettingsKeys(
    val quickActionLeft: String,
    val quickActionRight: String,
    val showCalibrationDialog: String,
    val lockScreenPresence: String,
    val keepUnlocked: String,
    val speedometerType: String,
    val showLinearCompass: String,
    val showDialTicksWhenNoCompass: String,
    val showSunMoonCompass: String,
    val displayMultipleBeacons: String,
    val numberOfVisibleBeacons: String,
    val nearbyLinearOnly: String,
    val showAzimuthIndicator: String,
    val highDetailMode: String,
    val dialLobes: String
) {
    companion object {
        fun create(): NavigationSettingsKeys {
            return NavigationSettingsKeys(
                quickActionLeft = PreferenceKeys.navigationQuickActionLeft,
                quickActionRight = PreferenceKeys.navigationQuickActionRight,
                showCalibrationDialog = PreferenceKeys.showCalibrateOnNavigateDialog,
                lockScreenPresence = PreferenceKeys.navigationLockScreenPresence,
                keepUnlocked = PreferenceKeys.navigationKeepUnlocked,
                speedometerType = PreferenceKeys.navigationSpeedometerType,
                showLinearCompass = PreferenceKeys.showLinearCompass,
                showDialTicksWhenNoCompass = PreferenceKeys.showDialTicksWhenNoCompass,
                showSunMoonCompass = PreferenceKeys.showSunMoonCompass,
                displayMultipleBeacons = PreferenceKeys.displayMultiBeacons,
                numberOfVisibleBeacons = PreferenceKeys.numVisibleBeacons,
                nearbyLinearOnly = PreferenceKeys.nearbyLinearOnly,
                showAzimuthIndicator = PreferenceKeys.navigationShowAzimuthIndicator,
                highDetailMode = PreferenceKeys.navigationHighDetailMode,
                dialLobes = PreferenceKeys.navigationDialLobes
            )
        }
    }
}

/**
 * The navigation settings: each setting as state, read from the preferences when the page opens and written back as
 * it changes. Plain Kotlin; the page shows the pickers and formats the distances.
 */
class NavigationSettingsViewModel(
    private val prefs: NavigationPreferences,
    private val cache: Preferences,
    private val keys: NavigationSettingsKeys,
    val hasCompass: Boolean,
    private val baseDistanceUnits: DistanceUnits
) : ViewModel() {

    var leftQuickAction by mutableStateOf(prefs.leftButton.toString())
        private set
    var rightQuickAction by mutableStateOf(prefs.rightButton.toString())
        private set
    var showCalibrationDialog by mutableStateOf(cache.getBoolean(keys.showCalibrationDialog) ?: true)
        private set
    var keepScreenUnlockedWhileNavigating by mutableStateOf(prefs.keepScreenUnlockedWhileNavigating)
        private set
    var keepScreenUnlockedWhileOpen by mutableStateOf(prefs.keepScreenUnlockedWhileOpen)
        private set
    var lockBearingToLocation by mutableStateOf(prefs.lockBearingToLocation)
        private set
    var speedometerType by mutableStateOf(cache.getString(keys.speedometerType) ?: "instant")
        private set
    var showLinearCompass by mutableStateOf(cache.getBoolean(keys.showLinearCompass) ?: false)
        private set
    var showDialTicksWhenNoCompass by mutableStateOf(prefs.showDialTicksWhenNoCompass)
        private set
    var showSunMoonCompass by mutableStateOf(cache.getString(keys.showSunMoonCompass) ?: "when_up")
        private set
    var displayMultipleBeacons by mutableStateOf(if (hasCompass) prefs.showMultipleBeacons else true)
        private set
    var numberOfVisibleBeacons by mutableStateOf(prefs.numberOfVisibleBeacons)
        private set
    var nearbyRadius by mutableStateOf(relative(Distance.meters(prefs.maxBeaconDistance)))
        private set
    var nearbyLinearOnly by mutableStateOf(prefs.showNearbyBeaconsOnlyOnLinearCompass)
        private set
    var showAzimuthIndicator by mutableStateOf(prefs.showAzimuthIndicator)
        private set
    var highDetailMode by mutableStateOf(prefs.highDetailMode)
        private set
    var bearingLength by mutableStateOf(relative(prefs.bearingDistance))
        private set
    var dialLobes by mutableStateOf(prefs.dialLobes.toString())
        private set

    /** The longest bearing line, in the units the page shows. */
    val maxBearingLength: Distance = relative(Destination.Bearing.maxBearingDistance)

    init {
        if (!hasCompass) {
            displayMultipleBeacons = true
            cache.putBoolean(keys.displayMultipleBeacons, true)
        }
    }

    fun updateLeftQuickAction(value: String) {
        leftQuickAction = value
        cache.putString(keys.quickActionLeft, value)
    }

    fun updateRightQuickAction(value: String) {
        rightQuickAction = value
        cache.putString(keys.quickActionRight, value)
    }

    fun updateShowCalibrationDialog(value: Boolean) {
        showCalibrationDialog = value
        cache.putBoolean(keys.showCalibrationDialog, value)
    }

    fun updateKeepScreenUnlockedWhileNavigating(value: Boolean) {
        keepScreenUnlockedWhileNavigating = value
        cache.putBoolean(keys.lockScreenPresence, value)
    }

    fun updateKeepScreenUnlockedWhileOpen(value: Boolean) {
        keepScreenUnlockedWhileOpen = value
        cache.putBoolean(keys.keepUnlocked, value)
    }

    fun updateLockBearingToLocation(value: Boolean) {
        lockBearingToLocation = value
        prefs.lockBearingToLocation = value
    }

    /** Returns true when the new speedometer counts steps, so the page can ask for activity recognition. */
    fun updateSpeedometerType(value: String): Boolean {
        speedometerType = value
        cache.putString(keys.speedometerType, value)
        return value == "instant_pedometer"
    }

    fun updateShowLinearCompass(value: Boolean) {
        showLinearCompass = value
        cache.putBoolean(keys.showLinearCompass, value)
    }

    fun updateShowDialTicksWhenNoCompass(value: Boolean) {
        showDialTicksWhenNoCompass = value
        cache.putBoolean(keys.showDialTicksWhenNoCompass, value)
    }

    fun updateShowSunMoonCompass(value: String) {
        showSunMoonCompass = value
        cache.putString(keys.showSunMoonCompass, value)
    }

    fun updateDialLobes(value: String) {
        dialLobes = value
        cache.putString(keys.dialLobes, value)
    }

    fun updateShowAzimuthIndicator(value: Boolean) {
        showAzimuthIndicator = value
        cache.putBoolean(keys.showAzimuthIndicator, value)
    }

    fun updateDisplayMultipleBeacons(value: Boolean) {
        displayMultipleBeacons = value
        cache.putBoolean(keys.displayMultipleBeacons, value)
    }

    fun updateNumberOfVisibleBeacons(count: Int) {
        cache.putString(keys.numberOfVisibleBeacons, count.toString())
        numberOfVisibleBeacons = count
    }

    fun updateNearbyRadius(distance: Distance) {
        prefs.maxBeaconDistance = distance.meters().value
        nearbyRadius = relative(Distance.meters(prefs.maxBeaconDistance))
    }

    fun updateNearbyLinearOnly(value: Boolean) {
        nearbyLinearOnly = value
        cache.putBoolean(keys.nearbyLinearOnly, value)
    }

    fun updateHighDetailMode(value: Boolean) {
        highDetailMode = value
        cache.putBoolean(keys.highDetailMode, value)
    }

    fun updateBearingLength(distance: Distance) {
        prefs.bearingDistance = distance
        bearingLength = relative(prefs.bearingDistance)
    }

    private fun relative(distance: Distance): Distance {
        return distance.convertTo(baseDistanceUnits).toRelativeDistance()
    }
}

private fun optionLabel(options: List<SettingsOption>, selected: String): String {
    return options.firstOrNull { it.value == selected }?.label ?: selected
}

@Composable
private fun NavigationSettingsContent(
    viewModel: NavigationSettingsViewModel,
    quickActionOptions: List<SettingsOption>,
    formatDistance: (Distance) -> String,
    onPickDistance: (current: Distance, title: String, onPicked: (Distance) -> Unit) -> Unit,
    onCurrentPaceSpeedometerSelected: () -> Unit,
    onShowLayers: () -> Unit,
    onBack: () -> Unit
) {
    val host = AppHost.current()
    val strings = LocalAppStrings.current
    val hasCompass = viewModel.hasCompass
    val linearCompassAvailable = LocalLinearCompassAvailable.current
    val speedometerOptions = rememberSettingsOptions(Res.array.speedometer_entries, Res.array.speedometer_values)
    val sunMoonOptions = rememberSettingsOptions(Res.array.sun_moon_compass_entries, Res.array.sun_moon_compass_values)

    fun pickOption(
        title: String,
        options: List<SettingsOption>,
        selected: String,
        onSelected: (String) -> Unit
    ) {
        Pickers.item(
            host,
            title,
            options.map { it.label },
            options.indexOfFirst { it.value == selected }.coerceAtLeast(0)
        ) { index ->
            if (index != null) {
                onSelected(options[index].value)
            }
        }
    }

    val generalTitle = appString(Res.string.general)
    val navigationTitle = appString(Res.string.navigation)
    val compassTitle = appString(Res.string.pref_compass_sensor_title)
    val beaconsTitle = appString(Res.string.beacons)
    val mapTitle = appString(Res.string.map)
    val sunMoonTitle = appString(Res.string.pref_show_sun_moon_compass_title)
    val bearingFooter = appString(Res.string.bearing_length_description) + " " +
            appString(Res.string.bearing_length_max, formatDistance(viewModel.maxBearingLength))
    val sunMoonChoices = sunMoonOptions.map { TrailSenseChoice(it.value, it.label) }
    val dialShapeTitle = appString(Res.string.navigation_dial_shape)
    val dialShapeChoices = listOf(
        TrailSenseChoice("12", appString(Res.string.navigation_dial_shape_12)),
        TrailSenseChoice("8", appString(Res.string.navigation_dial_shape_8))
    )
    TrailSensePage(
        title = navigationTitle,
        onBack = onBack,
        backLabel = appString(Res.string.back)
    ) {
        trailSenseSection("quick_actions", title = generalTitle) {
            row { position ->
                SettingsPickerRow(
                    label = appString(Res.string.left_quick_action),
                    selected = optionLabel(quickActionOptions, viewModel.leftQuickAction),
                    onClick = {
                        pickOption(
                            strings.get(Res.string.left_quick_action),
                            quickActionOptions,
                            viewModel.leftQuickAction,
                            viewModel::updateLeftQuickAction
                        )
                    },
                    position = position
                )
            }
            row { position ->
                SettingsPickerRow(
                    label = appString(Res.string.right_quick_action),
                    selected = optionLabel(quickActionOptions, viewModel.rightQuickAction),
                    onClick = {
                        pickOption(
                            strings.get(Res.string.right_quick_action),
                            quickActionOptions,
                            viewModel.rightQuickAction,
                            viewModel::updateRightQuickAction
                        )
                    },
                    position = position
                )
            }
        }

        trailSenseSection("navigation", title = navigationTitle, footer = bearingFooter) {
            if (hasCompass) {
                row { position ->
                    SettingsSwitchRow(
                        label = appString(Res.string.pref_show_calibrate_on_navigate_dialog_title),
                        checked = viewModel.showCalibrationDialog,
                        onCheckedChange = viewModel::updateShowCalibrationDialog,
                        position = position
                    )
                }
            }
            row { position ->
                SettingsSwitchRow(
                    label = appString(Res.string.pref_navigation_lock_screen_presence_title),
                    checked = viewModel.keepScreenUnlockedWhileNavigating,
                    onCheckedChange = viewModel::updateKeepScreenUnlockedWhileNavigating,
                    position = position
                )
            }
            row { position ->
                SettingsSwitchRow(
                    label = appString(Res.string.pref_navigation_lock_screen_when_open_title),
                    checked = viewModel.keepScreenUnlockedWhileOpen,
                    onCheckedChange = viewModel::updateKeepScreenUnlockedWhileOpen,
                    position = position
                )
            }
            row { position ->
                SettingsSwitchRow(
                    label = appString(Res.string.lock_bearing_to_location),
                    summary = appString(Res.string.lock_bearing_to_location_description),
                    checked = viewModel.lockBearingToLocation,
                    onCheckedChange = viewModel::updateLockBearingToLocation,
                    position = position
                )
            }
            row { position ->
                SettingsPickerRow(
                    label = appString(Res.string.speedometer),
                    selected = optionLabel(speedometerOptions, viewModel.speedometerType),
                    onClick = {
                        pickOption(
                            strings.get(Res.string.speedometer),
                            speedometerOptions,
                            viewModel.speedometerType
                        ) {
                            if (viewModel.updateSpeedometerType(it)) {
                                onCurrentPaceSpeedometerSelected()
                            }
                        }
                    },
                    position = position
                )
            }
            row { position ->
                SettingsPickerRow(
                    label = appString(Res.string.bearing_length),
                    selected = formatDistance(viewModel.bearingLength),
                    onClick = {
                        onPickDistance(
                            viewModel.bearingLength,
                            strings.get(Res.string.bearing_length),
                            viewModel::updateBearingLength
                        )
                    },
                    position = position
                )
            }
        }

        trailSenseSection("compass", title = compassTitle) {
            if (hasCompass && linearCompassAvailable) {
                row { position ->
                    SettingsSwitchRow(
                        label = appString(Res.string.pref_show_linear_compass_title),
                        summary = appString(Res.string.pref_show_linear_compass_summary),
                        checked = viewModel.showLinearCompass,
                        onCheckedChange = viewModel::updateShowLinearCompass,
                        position = position
                    )
                }
            } else if (!hasCompass) {
                row { position ->
                    SettingsSwitchRow(
                        label = appString(Res.string.show_dial_ticks_on_nearby_radar),
                        summary = appString(Res.string.show_dial_ticks_on_nearby_radar_summary),
                        checked = viewModel.showDialTicksWhenNoCompass,
                        onCheckedChange = viewModel::updateShowDialTicksWhenNoCompass,
                        position = position
                    )
                }
            }
            row { position ->
                TrailSenseGroupCard(position = position) {
                    Text(
                        text = sunMoonTitle,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = TrailSenseSpacing.md)
                    )
                    TrailSenseConnectedChoice(
                        options = sunMoonChoices,
                        selected = viewModel.showSunMoonCompass,
                        label = sunMoonTitle,
                        onSelect = viewModel::updateShowSunMoonCompass
                    )
                }
            }
            row { position ->
                TrailSenseGroupCard(position = position) {
                    Text(
                        text = dialShapeTitle,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = TrailSenseSpacing.md)
                    )
                    TrailSenseConnectedChoice(
                        options = dialShapeChoices,
                        selected = viewModel.dialLobes,
                        label = dialShapeTitle,
                        onSelect = viewModel::updateDialLobes
                    )
                }
            }
            if (hasCompass) {
                row { position ->
                    SettingsSwitchRow(
                        label = appString(Res.string.pref_navigation_show_azimuth_indicator_title),
                        summary = appString(Res.string.pref_navigation_show_azimuth_indicator_summary),
                        checked = viewModel.showAzimuthIndicator,
                        onCheckedChange = viewModel::updateShowAzimuthIndicator,
                        position = position
                    )
                }
            }
        }

        if (hasCompass) {
            trailSenseSection("beacons", title = beaconsTitle) {
                row { position ->
                    SettingsSwitchRow(
                        label = appString(Res.string.pref_display_multi_beacons_title),
                        summary = appString(Res.string.pref_display_multi_beacons_summary),
                        checked = viewModel.displayMultipleBeacons,
                        onCheckedChange = viewModel::updateDisplayMultipleBeacons,
                        position = position
                    )
                }
                if (viewModel.displayMultipleBeacons) {
                    row { position ->
                        SettingsPickerRow(
                            label = appString(Res.string.pref_num_visible_beacons_title),
                            selected = viewModel.numberOfVisibleBeacons.toString(),
                            onClick = {
                                Pickers.number(
                                    host,
                                    strings.get(Res.string.pref_num_visible_beacons_title),
                                    null,
                                    viewModel.numberOfVisibleBeacons,
                                    allowDecimals = false,
                                    allowNegative = false
                                ) { value ->
                                    if (value != null) {
                                        viewModel.updateNumberOfVisibleBeacons(value.toInt())
                                    }
                                }
                            },
                            position = position
                        )
                    }
                    row { position ->
                        SettingsPickerRow(
                            label = appString(Res.string.pref_max_beacon_distance_title),
                            selected = formatDistance(viewModel.nearbyRadius),
                            onClick = {
                                onPickDistance(
                                    viewModel.nearbyRadius,
                                    strings.get(Res.string.pref_max_beacon_distance_title),
                                    viewModel::updateNearbyRadius
                                )
                            },
                            position = position
                        )
                    }
                    row { position ->
                        SettingsSwitchRow(
                            label = appString(Res.string.pref_nearby_linear_only_title),
                            checked = viewModel.nearbyLinearOnly,
                            onCheckedChange = viewModel::updateNearbyLinearOnly,
                            position = position
                        )
                    }
                }
            }
        }

        trailSenseSection("map", title = mapTitle) {
            row { position ->
                SettingsSwitchRow(
                    label = appString(Res.string.pref_high_detail_mode_title),
                    summary = appString(Res.string.pref_high_detail_mode_summary),
                    checked = viewModel.highDetailMode,
                    onCheckedChange = viewModel::updateHighDetailMode,
                    position = position
                )
            }
            row { position ->
                SettingsActionRow(
                    label = appString(Res.string.layers),
                    onClick = onShowLayers,
                    position = position
                )
            }
        }
    }
}
