package com.kylecorry.trail_sense.shared.views.compose

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.util.Linkify.WEB_URLS
import android.view.View
import android.widget.NumberPicker
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.text.toSpannable
import androidx.core.text.util.LinkifyCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.andromeda.core.system.Screen
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.luna.text.toFloatCompat
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.receivers.StartOnBootController
import com.kylecorry.trail_sense.settings.infrastructure.CameraPreferences
import com.kylecorry.trail_sense.settings.infrastructure.ErrorPreferences
import com.kylecorry.trail_sense.settings.licenses.Library
import com.kylecorry.trail_sense.settings.licenses.Licenses
import com.kylecorry.trail_sense.settings.ui.SettingsActionRow
import com.kylecorry.trail_sense.settings.ui.SettingsSwitchRow
import com.kylecorry.trail_sense.settings.ui.SingleSwitchSettingsScreen
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.ErrorBannerReason
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.tiles.TileManager
import com.kylecorry.trail_sense.shared.views.ColorPickerDialog
import com.kylecorry.trail_sense.shared.views.DurationPickerDialog
import com.kylecorry.trail_sense.tools.WhistleScreen
import com.kylecorry.trail_sense.tools.battery.infrastructure.BatteryLogWorker
import com.kylecorry.trail_sense.tools.battery.infrastructure.LowPowerMode
import com.kylecorry.trail_sense.tools.experimentation.ExperimentationContent
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem
import com.kylecorry.trail_sense.tools.guide.ui.GuideScreen
import com.kylecorry.trail_sense.tools.level.ui.LevelScreen

class TrailSenseComposeDestinationFragment : TrailSenseComposeFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        when (requireArguments().getString(ARG_DESTINATION)) {
            DESTINATION_LOCAL_MESSAGING -> openApp(
                requireContext(),
                "com.kylecorry.trail_sense_comms",
                "4c285dfe-1c8b-45eb-bb79-3f1d2eb6ae48"
            )
            DESTINATION_LOCAL_TALK -> openApp(
                requireContext(),
                "com.kylecorry.trail_sense_comms",
                "b76f32bf-6a72-4992-a741-0c9bf19ebd11"
            )
        }
    }

    @Composable
    override fun FragmentContent() {
        when (requireArguments().getString(ARG_DESTINATION)) {
            DESTINATION_LEVEL -> LevelScreen()
            DESTINATION_WHISTLE -> WhistleScreen()
            DESTINATION_CELL_SIGNAL_SETTINGS -> CellSignalSettings()
            DESTINATION_CLOCK_SETTINGS -> ClockSettings()
            DESTINATION_TIDE_SETTINGS -> TideSettings()
            DESTINATION_TURN_BACK_SETTINGS -> TurnBackSettings()
            DESTINATION_WATER_BOIL_SETTINGS -> WaterBoilSettings()
            DESTINATION_LICENSES -> LicensesScreen()
            DESTINATION_BEACON_SETTINGS -> BeaconSettings()
            DESTINATION_BUBBLE_LEVEL_SETTINGS -> BubbleLevelSettings()
            DESTINATION_CAMERA_SETTINGS -> CameraSettings()
            DESTINATION_ERROR_SETTINGS -> ErrorSettings()
            DESTINATION_CLINOMETER_SETTINGS -> ClinometerSettings()
            DESTINATION_PRIVACY_SETTINGS -> PrivacySettings()
            DESTINATION_SENSOR_SETTINGS -> SensorSettings()
            DESTINATION_RULER_SETTINGS -> RulerSettings()
            DESTINATION_FLASHLIGHT_SETTINGS -> FlashlightSettings()
            DESTINATION_POWER_SETTINGS -> PowerSettings()
            DESTINATION_GUIDE -> GuideScreen(
                name = requireArguments().getString("guide_name", ""),
                resource = requireArguments().getInt("guide_contents")
            )
            DESTINATION_EXPERIMENTATION -> ExperimentationContent()
        }
    }

    @Composable
    private fun BubbleLevelSettings() {
        val prefs = remember { AppServiceRegistry.get<UserPreferences>().bubbleLevel }
        val formatter = remember { AppServiceRegistry.get<FormatService>() }
        var threshold by remember { mutableIntStateOf(prefs.threshold.toInt()) }
        val title = stringResource(R.string.pref_bubble_level_threshold_title)
        SettingsList(title = stringResource(R.string.tool_bubble_level_title)) {
            item("threshold") {
                SettingsActionRow(
                    label = title,
                    summary = formatter.formatDegrees(threshold.toFloat()),
                    onClick = {
                        pickBubbleLevelThreshold(title, threshold) {
                            threshold = it
                        }
                    },
                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                )
            }
        }
    }

    @Composable
    private fun BeaconSettings() {
        val prefs = remember { UserPreferences(requireContext()) }
        var defaultColor by remember {
            mutableStateOf(prefs.beacons.defaultBeaconColor)
        }
        var showDefaultColorPicker by remember { mutableStateOf(false) }
        val defaultColorTitle = stringResource(R.string.pref_default_beacon_color_title)
        SettingsList(title = stringResource(R.string.beacons)) {
            item("default_color") {
                SettingsActionRow(
                    label = defaultColorTitle,
                    onClick = {
                        showDefaultColorPicker = true
                    },
                    trailing = {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    Color(defaultColor.color),
                                    CircleShape
                                )
                        )
                    },
                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                )
            }
        }

        if (showDefaultColorPicker) {
            ColorPickerDialog(
                title = defaultColorTitle,
                selectedColor = defaultColor,
                onColorPicked = {
                    if (it != null) {
                        prefs.beacons.defaultBeaconColor = it
                        defaultColor = it
                    }
                    showDefaultColorPicker = false
                },
                onDismiss = {
                    showDefaultColorPicker = false
                }
            )
        }
    }

    @Composable
    private fun CameraSettings() {
        val prefs = remember { UserPreferences(requireContext()).camera }
        val projectionNames = projectionTypeNames()
        val projectionTypes = projectionNames.keys.toList()
        var projectionType by remember { mutableStateOf(prefs.projectionType) }
        val title = stringResource(R.string.projection_type)
        SettingsList(title = stringResource(R.string.camera)) {
            item("projection") {
                SettingsActionRow(
                    label = title,
                    summary = projectionNames.getValue(projectionType),
                    onClick = {
                        Pickers.item(
                            requireContext(),
                            title,
                            projectionNames.values.toList(),
                            projectionTypes.indexOf(projectionType)
                        ) {
                            if (it != null) {
                                projectionType = projectionTypes[it]
                                prefs.projectionType = projectionTypes[it]
                            }
                        }
                    },
                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                )
            }
        }
    }

    @Composable
    private fun ErrorSettings() {
        val prefs = remember { ErrorPreferences(requireContext()) }
        val errors = remember { ErrorBannerReason.values().sortedBy { it.id } }
        var values by remember {
            mutableStateOf(errors.associateWith { prefs.canShowError(it) })
        }

        SettingsList(title = stringResource(R.string.error_banners)) {
            items(errors, key = { it.id }) { error ->
                TrailSenseSwitchRow(
                    label = getErrorDisplayName(error),
                    checked = values[error] ?: true,
                    onCheckedChange = { canShow ->
                        prefs.setCanShowError(error, canShow)
                        values = values + (error to canShow)
                    },
                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                )
                TrailSenseDivider()
            }
        }
    }

    @Composable
    private fun ClinometerSettings() {
        val prefs = remember { UserPreferences(requireContext()) }
        val formatter = remember { FormatService.getInstance(requireContext()) }
        var lockWithVolume by remember {
            mutableStateOf(prefs.clinometer.lockWithVolumeButtons)
        }
        var baselineDistance by remember {
            mutableStateOf(prefs.clinometer.baselineDistance)
        }
        val baselineTitle = stringResource(R.string.pref_clinometer_baseline_distance_title)

        SettingsList(title = stringResource(R.string.clinometer_title)) {
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
                    summary = formatDistance(formatter, baselineDistance),
                    onClick = {
                        pickBaselineDistance(prefs, formatter, baselineTitle, baselineDistance) {
                            baselineDistance = it
                        }
                    },
                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                )
            }
        }
    }

    @Composable
    private fun PrivacySettings() {
        val prefs = remember { UserPreferences(requireContext()) }
        val lifecycleOwner = LocalLifecycleOwner.current
        var isLocationMocked by remember { mutableStateOf(isLocationMocked(prefs)) }
        var isScreenshotProtectionOn by remember {
            mutableStateOf(prefs.privacy.isScreenshotProtectionOn)
        }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    isLocationMocked = isLocationMocked(prefs)
                    isScreenshotProtectionOn = prefs.privacy.isScreenshotProtectionOn
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        SettingsList(title = stringResource(R.string.privacy)) {
            item("location") {
                TrailSenseListItem(
                    title = stringResource(R.string.location),
                    subtitle = if (isLocationMocked) {
                        stringResource(R.string.location_mocked)
                    } else {
                        stringResource(R.string.location_not_mocked)
                    },
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = {
                        findNavController().navigateWithAnimation(
                            R.id.action_privacy_settings_to_gps_calibration
                        )
                    },
                    animateEntrance = false
                )
                TrailSenseDivider()
            }
            item("screenshot_protection") {
                TrailSenseSwitchRow(
                    label = stringResource(R.string.pref_privacy_screenshot_protection_title),
                    checked = isScreenshotProtectionOn,
                    onCheckedChange = {
                        isScreenshotProtectionOn = it
                        prefs.privacy.isScreenshotProtectionOn = it
                        Screen.setAllowScreenshots(requireActivity().window, !it)
                    },
                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                )
                TrailSenseDivider()
            }
        }
    }

    @Composable
    private fun SensorSettings() {
        val hasBarometer = remember { Sensors.hasBarometer(requireContext()) }
        val items = remember(hasBarometer) {
            sensorSettingsItems(hasBarometer)
        }

        SettingsList(title = stringResource(R.string.sensors)) {
            items.forEach { item ->
                item(item.key) {
                    TrailSenseListItem(
                        title = stringResource(item.title),
                        icon = painterResource(item.icon),
                        onClick = {
                            findNavController().navigate(item.destination)
                        },
                        animateEntrance = false,
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                    TrailSenseDivider()
                }
            }
        }
    }

    @Composable
    private fun RulerSettings() {
        val prefs = remember { UserPreferences(requireContext()).ruler }
        var scale by remember { mutableFloatStateOf(prefs.rulerScale) }
        var flipped by remember { mutableStateOf(prefs.rulerFlipped) }
        var showCalibrationDialog by remember { mutableStateOf(false) }
        val calibrationTitle = stringResource(R.string.pref_ruler_calibration_title)

        SettingsList(title = stringResource(R.string.tool_ruler_title)) {
            item("calibration") {
                SettingsActionRow(
                    label = calibrationTitle,
                    summary = formatScale(scale),
                    onClick = {
                        showCalibrationDialog = true
                    },
                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                )
            }
            item("flip") {
                SettingsSwitchRow(
                    label = stringResource(R.string.pref_ruler_flip_direction_title),
                    checked = flipped,
                    onCheckedChange = {
                        flipped = it
                        prefs.rulerFlipped = it
                    },
                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                )
                TrailSenseDivider()
            }
        }

        if (showCalibrationDialog) {
            RulerScaleDialog(
                title = calibrationTitle,
                current = scale,
                onDismiss = { showCalibrationDialog = false },
                onConfirm = {
                    scale = it
                    prefs.rulerScale = it
                    showCalibrationDialog = false
                }
            )
        }
    }

    @Composable
    private fun FlashlightSettings() {
        val formatter = remember { FormatService.getInstance(requireContext()) }
        val prefs = remember { UserPreferences(requireContext()).flashlight }
        val hasFlashlight = remember {
            FlashlightSubsystem.getInstance(requireContext()).isAvailable()
        }
        var toggleWithVolume by remember {
            mutableStateOf(prefs.toggleWithVolumeButtons)
        }
        var shouldTimeout by remember { mutableStateOf(prefs.shouldTimeout) }
        var timeout by remember { mutableStateOf(prefs.timeout) }
        var controlScreenWithVolume by remember {
            mutableStateOf(prefs.controlScreenFlashlightWithVolumeButtons)
        }
        var showTimeoutPicker by remember { mutableStateOf(false) }
        val timeoutTitle = stringResource(R.string.pref_flashlight_timeout_title)

        SettingsList(title = stringResource(R.string.flashlight_title)) {
            if (hasFlashlight) {
                item("toggle_volume") {
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_flashlight_toggle_with_volume_title),
                        summary = stringResource(R.string.pref_flashlight_toggle_with_volume_summary),
                        checked = toggleWithVolume,
                        onCheckedChange = {
                            toggleWithVolume = it
                            prefs.toggleWithVolumeButtons = it
                        },
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                    TrailSenseDivider()
                }
                item("should_timeout") {
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_flashlight_should_timeout_title),
                        checked = shouldTimeout,
                        onCheckedChange = {
                            shouldTimeout = it
                            prefs.shouldTimeout = it
                        },
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                    TrailSenseDivider()
                }
                item("timeout") {
                    SettingsActionRow(
                        label = timeoutTitle,
                        summary = formatter.formatDuration(
                            timeout,
                            short = false,
                            includeSeconds = true
                        ),
                        enabled = shouldTimeout,
                        onClick = {
                            showTimeoutPicker = true
                        },
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                }
            }
            item("screen_volume") {
                SettingsSwitchRow(
                    label = stringResource(R.string.pref_flashlight_control_screen_with_volume_title),
                    checked = controlScreenWithVolume,
                    onCheckedChange = {
                        controlScreenWithVolume = it
                        prefs.controlScreenFlashlightWithVolumeButtons = it
                    },
                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                )
                TrailSenseDivider()
            }
        }

        if (showTimeoutPicker) {
            DurationPickerDialog(
                title = timeoutTitle,
                duration = timeout,
                showSeconds = true,
                onDurationPicked = {
                    if (it != null && !it.isZero) {
                        prefs.timeout = it
                        timeout = it
                    }
                    showTimeoutPicker = false
                },
                onDismiss = {
                    showTimeoutPicker = false
                }
            )
        }
    }

    @Composable
    private fun PowerSettings() {
        val prefs = remember { UserPreferences(requireContext()) }
        var lowPowerMode by remember { mutableStateOf(prefs.isLowPowerModeOn) }
        var autoLowPower by remember { mutableStateOf(prefs.power.autoLowPower) }
        var startOnBoot by remember { mutableStateOf(prefs.power.startOnBoot) }
        var tilesEnabled by remember { mutableStateOf(prefs.power.areTilesEnabled) }
        var batteryLogEnabled by remember { mutableStateOf(prefs.power.enableBatteryLog) }

        SettingsList(title = stringResource(R.string.tool_battery_title)) {
            item("low_power") {
                SettingsSwitchRow(
                    label = stringResource(R.string.pref_low_power_mode_title),
                    summary = stringResource(R.string.pref_low_power_mode_summary),
                    checked = lowPowerMode,
                    onCheckedChange = {
                        lowPowerMode = it
                        setLowPowerMode(prefs, it)
                    },
                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                )
                TrailSenseDivider()
            }
            item("auto_low_power") {
                SettingsSwitchRow(
                    label = stringResource(R.string.pref_auto_low_power_title),
                    summary = stringResource(R.string.pref_auto_low_power_summary),
                    checked = autoLowPower,
                    onCheckedChange = {
                        autoLowPower = it
                        prefs.power.autoLowPower = it
                    },
                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                )
                TrailSenseDivider()
            }
            item("start_on_boot") {
                SettingsSwitchRow(
                    label = stringResource(R.string.pref_start_on_boot_title),
                    checked = startOnBoot,
                    onCheckedChange = {
                        startOnBoot = it
                        prefs.power.startOnBoot = it
                        setStartOnBoot(it)
                    },
                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                )
                TrailSenseDivider()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                item("tiles") {
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_tiles_enabled_title),
                        summary = stringResource(R.string.pref_tiles_enabled_summary),
                        checked = tilesEnabled,
                        onCheckedChange = {
                            tilesEnabled = it
                            prefs.power.areTilesEnabled = it
                            TileManager().setTilesEnabled(requireContext(), it)
                        },
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                    TrailSenseDivider()
                }
            }
            item("battery_log") {
                SettingsSwitchRow(
                    label = stringResource(R.string.pref_tiles_battery_log),
                    summary = stringResource(R.string.pref_tiles_enabled_battery_log_summary),
                    checked = batteryLogEnabled,
                    onCheckedChange = {
                        batteryLogEnabled = it
                        prefs.power.enableBatteryLog = it
                        BatteryLogWorker.enableBatteryLog(requireContext(), it)
                    },
                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                )
                TrailSenseDivider()
            }
        }
    }

    @Composable
    private fun RulerScaleDialog(
        title: String,
        current: Float,
        onDismiss: () -> Unit,
        onConfirm: (Float) -> Unit
    ) {
        var value by remember(current) { mutableStateOf(formatScale(current)) }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = title)
            },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(text = stringResource(R.string.pref_ruler_calibration_summary)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm((value.toFloatCompat() ?: 1f).coerceAtLeast(0f))
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            }
        )
    }

    @Composable
    private fun SettingsList(
        title: String,
        content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
    ) {
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
                    TrailSenseSectionHeader(title = title)
                }
                content()
            }
        }
    }

    @Composable
    private fun LicensesScreen() {
        SettingsList(title = stringResource(R.string.pref_open_source_licenses_title)) {
            items(Licenses.libraries, key = { it.name }) { library ->
                SettingsActionRow(
                    label = library.name,
                    summary = library.url,
                    onClick = { showLicense(library) },
                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                )
            }
        }
    }

    @Composable
    private fun CellSignalSettings() {
        val prefs = remember { UserPreferences(requireContext()).cellSignal }
        var populateCache by remember { mutableStateOf(prefs.populateCache) }
        SingleSwitchSettingsScreen(
            title = stringResource(R.string.cell_signal),
            label = stringResource(R.string.pref_cell_signal_refresh_cache_title),
            summary = stringResource(R.string.pref_cell_signal_refresh_cache_summary),
            checked = populateCache,
            onCheckedChange = {
                populateCache = it
                prefs.populateCache = it
            }
        )
    }

    @Composable
    private fun ClockSettings() {
        val prefs = remember { UserPreferences(requireContext()).clock }
        var showAnalogClock by remember { mutableStateOf(prefs.showAnalogClock) }
        SingleSwitchSettingsScreen(
            title = stringResource(R.string.tool_clock_title),
            label = stringResource(R.string.pref_enable_analog_title),
            checked = showAnalogClock,
            onCheckedChange = {
                showAnalogClock = it
                prefs.showAnalogClock = it
            }
        )
    }

    @Composable
    private fun TideSettings() {
        val prefs = remember { UserPreferences(requireContext()).tides }
        var showNearestTide by remember { mutableStateOf(prefs.showNearestTide) }
        SingleSwitchSettingsScreen(
            title = stringResource(R.string.tides),
            label = stringResource(R.string.pref_show_nearest_tide_title),
            checked = showNearestTide,
            onCheckedChange = {
                showNearestTide = it
                prefs.showNearestTide = it
            }
        )
    }

    @Composable
    private fun TurnBackSettings() {
        val prefs = remember { UserPreferences(requireContext()).turnBack }
        var useAlarm by remember { mutableStateOf(prefs.useAlarm) }
        SingleSwitchSettingsScreen(
            title = stringResource(R.string.tool_turn_back),
            label = stringResource(R.string.use_alarm_for_turn_back_alert),
            summary = stringResource(R.string.alarm_description),
            checked = useAlarm,
            onCheckedChange = {
                useAlarm = it
                prefs.useAlarm = it
            }
        )
    }

    @Composable
    private fun WaterBoilSettings() {
        val prefs = remember { UserPreferences(requireContext()).waterBoilTimer }
        var useAlarm by remember { mutableStateOf(prefs.useAlarm) }
        SingleSwitchSettingsScreen(
            title = stringResource(R.string.water_boil_timer),
            label = stringResource(R.string.use_alarm_for_water_boil_timer),
            summary = stringResource(R.string.alarm_description),
            checked = useAlarm,
            onCheckedChange = {
                useAlarm = it
                prefs.useAlarm = it
            }
        )
    }

    private fun openApp(context: Context, packageName: String, tool: String) {
        if (!Package.isPackageInstalled(context, packageName)) return
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.putExtras(Bundle().apply {
            putString("tool", tool)
        })
        context.startActivity(intent)
    }

    private fun showLicense(library: Library) {
        val content = (library.url + "\n\n" + library.license()).toSpannable()
        LinkifyCompat.addLinks(content, WEB_URLS)

        Alerts.dialog(
            requireContext(),
            library.name,
            content,
            cancelText = null,
            allowLinks = true
        )
    }

    private fun pickBubbleLevelThreshold(title: String, current: Int, onChange: (Int) -> Unit) {
        val picker = NumberPicker(requireContext())
        picker.minValue = 0
        picker.maxValue = 10
        picker.value = current
        picker.wrapSelectorWheel = false

        Alerts.dialog(
            requireContext(),
            title,
            contentView = picker
        ) { cancelled ->
            if (!cancelled) {
                AppServiceRegistry.get<UserPreferences>().bubbleLevel.threshold =
                    picker.value.toFloat()
                onChange(picker.value)
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

    private fun formatDistance(formatter: FormatService, distance: Distance?): String {
        return distance?.let {
            formatter.formatDistance(
                it,
                Units.getDecimalPlaces(it.units)
            )
        } ?: getString(R.string.dash)
    }

    private fun isLocationMocked(prefs: UserPreferences): Boolean {
        return !prefs.useAutoLocation || !SensorService(requireContext()).hasLocationPermission()
    }

    private fun setLowPowerMode(prefs: UserPreferences, enabled: Boolean) {
        if (enabled) {
            prefs.power.userEnabledLowPower = true
            LowPowerMode(requireContext()).enable()
        } else {
            prefs.power.userEnabledLowPower = false
            LowPowerMode(requireContext()).disable()
        }
    }

    private fun setStartOnBoot(enabled: Boolean) {
        val controller = StartOnBootController(requireContext())
        if (enabled) {
            controller.enable()
        } else {
            controller.disable()
        }
    }

    private fun formatScale(scale: Float): String {
        return scale.toString()
    }

    private fun projectionTypeNames(): Map<CameraPreferences.ProjectionType, String> {
        return mapOf(
            CameraPreferences.ProjectionType.EstimatedIntrinsics to
                    "${getString(R.string.projection_estimated_intrinsics)} (${
                        getString(R.string.default_string)
                    })",
            CameraPreferences.ProjectionType.ManufacturerIntrinsics to
                    getString(R.string.projection_manufacturer_intrinsics),
            CameraPreferences.ProjectionType.Perspective to getString(R.string.projection_perspective),
            CameraPreferences.ProjectionType.Linear to getString(R.string.projection_linear)
        )
    }

    private fun sensorSettingsItems(hasBarometer: Boolean): List<SensorSettingsItem> {
        return listOfNotNull(
            SensorSettingsItem(
                key = "compass",
                title = R.string.pref_compass_sensor_title,
                icon = R.drawable.ic_compass_icon,
                destination = R.id.calibrateCompassFragment
            ),
            SensorSettingsItem(
                key = "gps",
                title = R.string.gps,
                icon = R.drawable.satellite,
                destination = R.id.calibrateGPSFragment
            ),
            SensorSettingsItem(
                key = "altimeter",
                title = R.string.pref_altimeter_calibration_title,
                icon = R.drawable.ic_altitude,
                destination = R.id.calibrateAltimeterFragment
            ),
            if (hasBarometer) {
                SensorSettingsItem(
                    key = "barometer",
                    title = R.string.barometer,
                    icon = R.drawable.ic_barometer,
                    destination = R.id.calibrateBarometerFragment
                )
            } else {
                null
            },
            SensorSettingsItem(
                key = "temperature",
                title = R.string.tool_thermometer_title,
                icon = R.drawable.thermometer,
                destination = R.id.thermometerSettingsFragment
            ),
            SensorSettingsItem(
                key = "camera",
                title = R.string.camera,
                icon = R.drawable.ic_camera,
                destination = R.id.cameraSettingsFragment
            ),
            SensorSettingsItem(
                key = "cell_signal",
                title = R.string.cell_signal,
                icon = R.drawable.signal_cellular_3,
                destination = R.id.cellSignalSettingsFragment
            ),
            SensorSettingsItem(
                key = "details",
                title = R.string.pref_sensor_details_title,
                icon = R.drawable.ic_sensors,
                destination = R.id.sensorDetailsFragment
            )
        )
    }

    private data class SensorSettingsItem(
        val key: String,
        val title: Int,
        @DrawableRes val icon: Int,
        @IdRes val destination: Int
    )

    @Composable
    private fun getErrorDisplayName(reason: ErrorBannerReason): String {
        return when (reason) {
            ErrorBannerReason.NoGPS -> stringResource(R.string.gps_unavailable)
            ErrorBannerReason.LocationNotSet -> stringResource(R.string.location_not_set)
            ErrorBannerReason.CompassPoor -> stringResource(R.string.compass_accuracy)
            ErrorBannerReason.NoCompass -> stringResource(R.string.compass_unavailable)
            ErrorBannerReason.GPSTimeout -> stringResource(R.string.gps_timeouts)
        }
    }

    companion object {
        const val ARG_DESTINATION = "compose_destination"
        const val DESTINATION_LEVEL = "level"
        const val DESTINATION_WHISTLE = "whistle"
        const val DESTINATION_LOCAL_MESSAGING = "local_messaging"
        const val DESTINATION_LOCAL_TALK = "local_talk"
        const val DESTINATION_CELL_SIGNAL_SETTINGS = "cell_signal_settings"
        const val DESTINATION_CLOCK_SETTINGS = "clock_settings"
        const val DESTINATION_TIDE_SETTINGS = "tide_settings"
        const val DESTINATION_TURN_BACK_SETTINGS = "turn_back_settings"
        const val DESTINATION_WATER_BOIL_SETTINGS = "water_boil_settings"
        const val DESTINATION_LICENSES = "licenses"
        const val DESTINATION_BEACON_SETTINGS = "beacon_settings"
        const val DESTINATION_BUBBLE_LEVEL_SETTINGS = "bubble_level_settings"
        const val DESTINATION_CAMERA_SETTINGS = "camera_settings"
        const val DESTINATION_ERROR_SETTINGS = "error_settings"
        const val DESTINATION_CLINOMETER_SETTINGS = "clinometer_settings"
        const val DESTINATION_PRIVACY_SETTINGS = "privacy_settings"
        const val DESTINATION_SENSOR_SETTINGS = "sensor_settings"
        const val DESTINATION_RULER_SETTINGS = "ruler_settings"
        const val DESTINATION_FLASHLIGHT_SETTINGS = "flashlight_settings"
        const val DESTINATION_POWER_SETTINGS = "power_settings"
        const val DESTINATION_GUIDE = "guide"
        const val DESTINATION_EXPERIMENTATION = "experimentation"
    }
}
