@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.navigation.ui

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
import androidx.compose.ui.res.stringResource
import android.content.Context
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.views.compose.AppPermission
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.tools.navigation.infrastructure.NavigationPreferences
import kotlinx.coroutines.launch
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.SettingsActionRow
import com.kylecorry.trail_sense.settings.ui.SettingsPickerRow
import com.kylecorry.trail_sense.settings.ui.SettingsSwitchRow
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.MapLayersSheetState
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
fun NavigationSettingsToolScreen() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val navigator = LocalNavigator.current
    val effects = LocalScreenEffects.current
    val scope = rememberCoroutineScope()
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val viewModel = viewModel {
        val userPrefs = UserPreferences(appContext)
        NavigationSettingsViewModel(
            prefs = userPrefs.navigation,
            cache = PreferencesSubsystem.getInstance(appContext).preferences,
            keys = NavigationSettingsKeys.from(appContext),
            hasCompass = SensorService(appContext).hasCompass(),
            baseDistanceUnits = userPrefs.baseDistanceUnits
        )
    }
    val formatter = remember(appContext) { FormatService.getInstance(appContext) }
    val distanceUnits = remember(formatter) { formatter.sortDistanceUnits(DistanceUtils.hikingDistanceUnits) }
    val layerSheet = remember { MapLayersSheetState(NavigationToolRegistration.MAP_ID) }

    DisposableEffect(layerSheet) {
        onDispose {
            layerSheet.dismiss()
        }
    }

    NavigationSettingsContent(
        viewModel = viewModel,
        formatDistance = { formatter.formatDistance(it, Units.getDecimalPlaces(it.units)) },
        onPickDistance = { current, title, onPicked ->
            CustomUiUtils.pickDistance(
                context,
                distanceUnits,
                current,
                title
            ) { distance, cancelled ->
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
        onShowLayers = {
            layerSheet.dismiss()
            layerSheet.show()
        },
        onBack = { backDispatcher?.onBackPressed() }
    )
    layerSheet.Content(
        context,
        { navigator.navigate(it) }
    )
}

/** The preference keys the page writes, resolved once from the resources so the ViewModel never touches them. */
internal data class NavigationSettingsKeys(
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
        fun from(context: Context): NavigationSettingsKeys {
            return NavigationSettingsKeys(
                quickActionLeft = context.getString(R.string.pref_navigation_quick_action_left),
                quickActionRight = context.getString(R.string.pref_navigation_quick_action_right),
                showCalibrationDialog = context.getString(R.string.pref_show_calibrate_on_navigate_dialog),
                lockScreenPresence = context.getString(R.string.pref_navigation_lock_screen_presence),
                keepUnlocked = context.getString(R.string.pref_navigation_keep_unlocked),
                speedometerType = context.getString(R.string.pref_navigation_speedometer_type),
                showLinearCompass = context.getString(R.string.pref_show_linear_compass),
                showDialTicksWhenNoCompass = context.getString(R.string.pref_show_dial_ticks_when_no_compass),
                showSunMoonCompass = context.getString(R.string.pref_show_sun_moon_compass),
                displayMultipleBeacons = context.getString(R.string.pref_display_multi_beacons),
                numberOfVisibleBeacons = context.getString(R.string.pref_num_visible_beacons),
                nearbyLinearOnly = context.getString(R.string.pref_nearby_linear_only),
                showAzimuthIndicator = context.getString(R.string.pref_navigation_show_azimuth_indicator),
                highDetailMode = context.getString(R.string.pref_navigation_high_detail_mode),
                dialLobes = context.getString(R.string.pref_navigation_dial_lobes)
            )
        }
    }
}

/**
 * The navigation settings: each setting as state, read from the preferences when the page opens and written back as
 * it changes. Plain Kotlin; the page shows the pickers and formats the distances.
 */
internal class NavigationSettingsViewModel(
    private val prefs: NavigationPreferences,
    private val cache: IPreferences,
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

private data class NavigationOption(
    val value: String,
    val label: String
)

private fun Context.navigationOptions(entriesId: Int, valuesId: Int): List<NavigationOption> {
    val entries = resources.getStringArray(entriesId)
    val values = resources.getStringArray(valuesId)
    return values.mapIndexed { index, value ->
        NavigationOption(value, entries.getOrElse(index) { value })
    }
}

private fun optionLabel(options: List<NavigationOption>, selected: String): String {
    return options.firstOrNull { it.value == selected }?.label ?: selected
}

@Composable
private fun NavigationSettingsContent(
    viewModel: NavigationSettingsViewModel,
    formatDistance: (Distance) -> String,
    onPickDistance: (current: Distance, title: String, onPicked: (Distance) -> Unit) -> Unit,
    onCurrentPaceSpeedometerSelected: () -> Unit,
    onShowLayers: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val hasCompass = viewModel.hasCompass
    val quickActionOptions = remember {
        Tools.getQuickActions(context).map { NavigationOption(it.id.toString(), it.name) }
    }
    val speedometerOptions = remember {
        context.navigationOptions(R.array.speedometer_entries, R.array.speedometer_values)
    }
    val sunMoonOptions = remember {
        context.navigationOptions(R.array.sun_moon_compass_entries, R.array.sun_moon_compass_values)
    }

    fun pickOption(
        title: String,
        options: List<NavigationOption>,
        selected: String,
        onSelected: (String) -> Unit
    ) {
        Pickers.item(
            context,
            title,
            options.map { it.label },
            options.indexOfFirst { it.value == selected }.coerceAtLeast(0)
        ) { index ->
            if (index != null) {
                onSelected(options[index].value)
            }
        }
    }

    val generalTitle = stringResource(R.string.general)
    val navigationTitle = stringResource(R.string.navigation)
    val compassTitle = stringResource(R.string.pref_compass_sensor_title)
    val beaconsTitle = stringResource(R.string.beacons)
    val mapTitle = stringResource(R.string.map)
    val sunMoonTitle = stringResource(R.string.pref_show_sun_moon_compass_title)
    val bearingFooter = stringResource(R.string.bearing_length_description) + " " +
            stringResource(R.string.bearing_length_max, formatDistance(viewModel.maxBearingLength))
    val sunMoonChoices = sunMoonOptions.map { TrailSenseChoice(it.value, it.label) }
    val dialShapeTitle = stringResource(R.string.navigation_dial_shape)
    val dialShapeChoices = listOf(
        TrailSenseChoice("12", stringResource(R.string.navigation_dial_shape_12)),
        TrailSenseChoice("8", stringResource(R.string.navigation_dial_shape_8))
    )
    TrailSensePage(
        title = navigationTitle,
        onBack = onBack,
        backLabel = stringResource(R.string.back)
    ) {
        trailSenseSection("quick_actions", title = generalTitle) {
            row { position ->
                SettingsPickerRow(
                    label = stringResource(R.string.left_quick_action),
                    selected = optionLabel(quickActionOptions, viewModel.leftQuickAction),
                    onClick = {
                        pickOption(
                            context.getString(R.string.left_quick_action),
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
                    label = stringResource(R.string.right_quick_action),
                    selected = optionLabel(quickActionOptions, viewModel.rightQuickAction),
                    onClick = {
                        pickOption(
                            context.getString(R.string.right_quick_action),
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
                        label = stringResource(R.string.pref_show_calibrate_on_navigate_dialog_title),
                        checked = viewModel.showCalibrationDialog,
                        onCheckedChange = viewModel::updateShowCalibrationDialog,
                        position = position
                    )
                }
            }
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.pref_navigation_lock_screen_presence_title),
                    checked = viewModel.keepScreenUnlockedWhileNavigating,
                    onCheckedChange = viewModel::updateKeepScreenUnlockedWhileNavigating,
                    position = position
                )
            }
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.pref_navigation_lock_screen_when_open_title),
                    checked = viewModel.keepScreenUnlockedWhileOpen,
                    onCheckedChange = viewModel::updateKeepScreenUnlockedWhileOpen,
                    position = position
                )
            }
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.lock_bearing_to_location),
                    summary = stringResource(R.string.lock_bearing_to_location_description),
                    checked = viewModel.lockBearingToLocation,
                    onCheckedChange = viewModel::updateLockBearingToLocation,
                    position = position
                )
            }
            row { position ->
                SettingsPickerRow(
                    label = stringResource(R.string.speedometer),
                    selected = optionLabel(speedometerOptions, viewModel.speedometerType),
                    onClick = {
                        pickOption(
                            context.getString(R.string.speedometer),
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
                    label = stringResource(R.string.bearing_length),
                    selected = formatDistance(viewModel.bearingLength),
                    onClick = {
                        onPickDistance(
                            viewModel.bearingLength,
                            context.getString(R.string.bearing_length),
                            viewModel::updateBearingLength
                        )
                    },
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
                        checked = viewModel.showLinearCompass,
                        onCheckedChange = viewModel::updateShowLinearCompass,
                        position = position
                    )
                }
            } else {
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.show_dial_ticks_on_nearby_radar),
                        summary = stringResource(R.string.show_dial_ticks_on_nearby_radar_summary),
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
                        label = stringResource(R.string.pref_navigation_show_azimuth_indicator_title),
                        summary = stringResource(R.string.pref_navigation_show_azimuth_indicator_summary),
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
                        label = stringResource(R.string.pref_display_multi_beacons_title),
                        summary = stringResource(R.string.pref_display_multi_beacons_summary),
                        checked = viewModel.displayMultipleBeacons,
                        onCheckedChange = viewModel::updateDisplayMultipleBeacons,
                        position = position
                    )
                }
                if (viewModel.displayMultipleBeacons) {
                    row { position ->
                        SettingsPickerRow(
                            label = stringResource(R.string.pref_num_visible_beacons_title),
                            selected = viewModel.numberOfVisibleBeacons.toString(),
                            onClick = {
                                Pickers.number(
                                    context,
                                    context.getString(R.string.pref_num_visible_beacons_title),
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
                            label = stringResource(R.string.pref_max_beacon_distance_title),
                            selected = formatDistance(viewModel.nearbyRadius),
                            onClick = {
                                onPickDistance(
                                    viewModel.nearbyRadius,
                                    context.getString(R.string.pref_max_beacon_distance_title),
                                    viewModel::updateNearbyRadius
                                )
                            },
                            position = position
                        )
                    }
                    row { position ->
                        SettingsSwitchRow(
                            label = stringResource(R.string.pref_nearby_linear_only_title),
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
                    label = stringResource(R.string.pref_high_detail_mode_title),
                    summary = stringResource(R.string.pref_high_detail_mode_summary),
                    checked = viewModel.highDetailMode,
                    onCheckedChange = viewModel::updateHighDetailMode,
                    position = position
                )
            }
            row { position ->
                SettingsActionRow(
                    label = stringResource(R.string.layers),
                    onClick = onShowLayers,
                    position = position
                )
            }
        }
    }
}
