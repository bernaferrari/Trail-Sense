package com.kylecorry.trail_sense.shared.views.compose

import android.content.Context
import android.content.Intent
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
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
import com.kylecorry.trail_sense.shared.views.ColorPickerDialog
import com.kylecorry.trail_sense.tools.WhistleScreen
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
    }
}
