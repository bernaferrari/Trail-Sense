@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.navigation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.SettingsSwitchRow
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
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
                    var bearingLength by remember {
                        mutableStateOf(controller.relative(prefs.bearingDistance))
                    }
                    val maxBearingLength = remember {
                        controller.formatDistance(
                            controller.relative(Destination.Bearing.maxBearingDistance)
                        )
                    }

                    LaunchedEffect(hasCompass) {
                        if (!hasCompass) {
                            displayMultipleBeacons = true
                            controller.putBoolean(R.string.pref_display_multi_beacons, true)
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = TrailSenseSize.maximumContentWidth),
                                contentPadding = PaddingValues(
                                    start = TrailSenseSpacing.screen,
                                    end = TrailSenseSpacing.screen,
                                    top = TrailSenseSpacing.lg,
                                    bottom = TrailSenseSpacing.xxxl
                                ),
                                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
                            ) {
                            item("title") {
                                Text(
                                    text = stringResource(R.string.navigation),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(
                                        top = TrailSenseSpacing.sm,
                                        bottom = TrailSenseSpacing.xs
                                    )
                                )
                            }
                            item("general_section") {
                                NavSettingsSection(
                                    title = stringResource(R.string.general),
                                    icon = painterResource(R.drawable.ic_category_navigation)
                                ) {
                                    NavPickerRow(
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
                                        }
                                    )
                                    NavDivider()
                                    NavPickerRow(
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
                                        }
                                    )
                                }
                            }

                            item("navigation_section") {
                                NavSettingsSection(title = stringResource(R.string.navigation)) {
                                    if (hasCompass) {
                                        SettingsSwitchRow(
                                            label = stringResource(R.string.pref_show_calibrate_on_navigate_dialog_title),
                                            checked = showCalibrationDialog,
                                            onCheckedChange = {
                                                showCalibrationDialog = it
                                                controller.putBoolean(
                                                    R.string.pref_show_calibrate_on_navigate_dialog,
                                                    it
                                                )
                                            }
                                        )
                                        NavDivider()
                                    }
                                    SettingsSwitchRow(
                                        label = stringResource(R.string.pref_navigation_lock_screen_presence_title),
                                        checked = keepScreenUnlockedWhileNavigating,
                                        onCheckedChange = {
                                            keepScreenUnlockedWhileNavigating = it
                                            controller.putBoolean(
                                                R.string.pref_navigation_lock_screen_presence,
                                                it
                                            )
                                        }
                                    )
                                    NavDivider()
                                    SettingsSwitchRow(
                                        label = stringResource(R.string.pref_navigation_lock_screen_when_open_title),
                                        checked = keepScreenUnlockedWhileOpen,
                                        onCheckedChange = {
                                            keepScreenUnlockedWhileOpen = it
                                            controller.putBoolean(R.string.pref_navigation_keep_unlocked, it)
                                        }
                                    )
                                    NavDivider()
                                    SettingsSwitchRow(
                                        label = stringResource(R.string.lock_bearing_to_location),
                                        summary = stringResource(R.string.lock_bearing_to_location_description),
                                        checked = lockBearingToLocation,
                                        onCheckedChange = {
                                            lockBearingToLocation = it
                                            prefs.lockBearingToLocation = it
                                        }
                                    )
                                    NavDivider()
                                    NavPickerRow(
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
                                        }
                                    )
                                    NavDivider()
                                    NavActionRow(
                                        label = stringResource(R.string.bearing_length),
                                        summary = controller.formatDistance(bearingLength) + "\n" +
                                            stringResource(R.string.bearing_length_description) + "\n" +
                                            stringResource(R.string.bearing_length_max, maxBearingLength),
                                        onClick = {
                                            controller.pickBearingLength(bearingLength) {
                                                bearingLength = it
                                            }
                                        }
                                    )
                                }
                            }

                            item("compass_section") {
                                NavSettingsSection(
                                    title = stringResource(R.string.pref_compass_sensor_title),
                                    icon = painterResource(R.drawable.ic_compass_icon)
                                ) {
                                    if (hasCompass) {
                                        SettingsSwitchRow(
                                            label = stringResource(R.string.pref_show_linear_compass_title),
                                            summary = stringResource(R.string.pref_show_linear_compass_summary),
                                            checked = showLinearCompass,
                                            onCheckedChange = {
                                                showLinearCompass = it
                                                controller.putBoolean(R.string.pref_show_linear_compass, it)
                                            }
                                        )
                                        NavDivider()
                                    } else {
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
                                            }
                                        )
                                        NavDivider()
                                    }
                                    NavPickerRow(
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
                                        }
                                    )
                                    if (hasCompass) {
                                        NavDivider()
                                        SettingsSwitchRow(
                                            label = stringResource(R.string.pref_display_multi_beacons_title),
                                            summary = stringResource(R.string.pref_display_multi_beacons_summary),
                                            checked = displayMultipleBeacons,
                                            onCheckedChange = {
                                                displayMultipleBeacons = it
                                                controller.putBoolean(R.string.pref_display_multi_beacons, it)
                                            }
                                        )
                                        NavDivider()
                                        NavPickerRow(
                                            label = stringResource(R.string.pref_num_visible_beacons_title),
                                            selected = numberOfVisibleBeacons.toString(),
                                            enabled = displayMultipleBeacons,
                                            onClick = {
                                                controller.pickVisibleBeaconCount(numberOfVisibleBeacons) {
                                                    numberOfVisibleBeacons = it
                                                }
                                            }
                                        )
                                        NavDivider()
                                        NavPickerRow(
                                            label = stringResource(R.string.pref_max_beacon_distance_title),
                                            selected = controller.formatDistance(nearbyRadius),
                                            enabled = displayMultipleBeacons,
                                            onClick = {
                                                controller.pickNearbyRadius(nearbyRadius) {
                                                    nearbyRadius = it
                                                }
                                            }
                                        )
                                        NavDivider()
                                        SettingsSwitchRow(
                                            label = stringResource(R.string.pref_nearby_linear_only_title),
                                            checked = nearbyLinearOnly,
                                            enabled = displayMultipleBeacons,
                                            onCheckedChange = {
                                                nearbyLinearOnly = it
                                                controller.putBoolean(R.string.pref_nearby_linear_only, it)
                                            }
                                        )
                                    }
                                    NavDivider()
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
                                        }
                                    )
                                    if (hasCompass) {
                                        NavDivider()
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
                                            }
                                        )
                                    }
                                }
                            }

                            item("map_section") {
                                NavSettingsSection(
                                    title = stringResource(R.string.map),
                                    icon = painterResource(R.drawable.maps)
                                ) {
                                    SettingsSwitchRow(
                                        label = stringResource(R.string.pref_high_detail_mode_title),
                                        summary = stringResource(R.string.pref_high_detail_mode_summary),
                                        checked = highDetailMode,
                                        onCheckedChange = {
                                            highDetailMode = it
                                            controller.putBoolean(R.string.pref_navigation_high_detail_mode, it)
                                        }
                                    )
                                    NavDivider()
                                    NavActionRow(
                                        label = stringResource(R.string.layers),
                                        onClick = controller::showLayerSheet
                                    )
                                }
                            }
                            }
                        }
                    }
}

@Composable
private fun NavSettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.padding(
                start = TrailSenseSpacing.md,
                bottom = TrailSenseSpacing.sm
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(TrailSenseSize.leadingIconContainer)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(TrailSenseSize.leadingIcon)
                        )
                    }
                }
                Spacer(Modifier.width(TrailSenseSpacing.md))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = Color.Transparent
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
                content()
            }
        }
    }
}

@Composable
private fun NavDivider() {
    Spacer(modifier = Modifier.height(0.dp))
}

@Composable
private fun NavPickerRow(
    label: String,
    selected: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    NavActionRow(
        label = label,
        summary = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    )
}

@Composable
private fun NavActionRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true
) {
    TrailSenseListItem(
        title = label,
        subtitle = summary,
        trailing = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        onClick = if (enabled) onClick else null,
        animateEntrance = false,
        modifier = modifier.alpha(if (enabled) 1f else 0.5f)
    )
}
