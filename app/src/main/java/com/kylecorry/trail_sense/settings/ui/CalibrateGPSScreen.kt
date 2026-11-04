@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider as ComposeSlider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import android.view.View
import android.widget.TextView
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.andromeda.sense.location.GPS
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.luna.time.Throttle
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.CustomGPS
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.gps.GPSAccuracyFilter
import com.kylecorry.trail_sense.shared.sensors.gps.GPSLocationSource
import com.kylecorry.trail_sense.shared.sensors.gps.GPSPowerMode
import com.kylecorry.trail_sense.shared.sensors.gps.modules.CacheGPSModule
import com.kylecorry.trail_sense.shared.views.Slider
import java.time.Duration
import kotlin.math.roundToInt
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedGPS
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideGPS
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseConnectedChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.beacons.infrastructure.BeaconPickers
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.ClosestBeaconSort
import kotlinx.coroutines.launch

@Composable
fun CalibrateGPSToolScreen(host: TrailSenseDestinationHost) {
    val controller = remember(host) { CalibrateGPSController(host) }
    val lifecycleOwner = LocalLifecycleOwner.current

    androidx.compose.runtime.DisposableEffect(controller, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> controller.onResume()
                Lifecycle.Event.ON_PAUSE -> controller.onPause()
                Lifecycle.Event.ON_DESTROY -> controller.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.onPause()
            controller.onDestroy()
        }
    }

    CalibrateGPSContent(controller)
}

private class CalibrateGPSController(private val host: TrailSenseDestinationHost) {

    val prefs by lazy { UserPreferences(host.requireContext()) }
    private val sensorService by lazy { SensorService(host.requireContext()) }
    private val throttle = Throttle(20)
    private val formatService by lazy { FormatService.getInstance(host.requireContext()) }

    private var gps: IGPS = sensorService.getGPS()
    private var realGps: IGPS = getRealGPS()
    var locationOverride by mutableStateOf(prefs.gps.locationOverride)
        private set
    var isOverrideLocationLoading by mutableStateOf(false)
        private set

    private var wasUsingRealGPS = shouldUseRealGPS()
    private var wasUsingCachedGPS = shouldUseCachedGPS()

    private var setLocationSummary: ((String) -> Unit)? = null
    private var setAccuracySummary: ((String?) -> Unit)? = null
    private var setPermissionVisible: ((Boolean) -> Unit)? = null
    private var setAutoLocationEnabled: ((Boolean) -> Unit)? = null
    private var setOverrideEnabled: ((Boolean) -> Unit)? = null

    fun setLocationSummary(updater: (String) -> Unit) {
        setLocationSummary = updater
    }

    fun setAccuracySummary(updater: (String?) -> Unit) {
        setAccuracySummary = updater
    }

    fun setPermissionVisible(updater: (Boolean) -> Unit) {
        setPermissionVisible = updater
    }

    fun setAutoLocationEnabled(updater: (Boolean) -> Unit) {
        setAutoLocationEnabled = updater
    }

    fun setOverrideEnabled(updater: (Boolean) -> Unit) {
        setOverrideEnabled = updater
    }

    fun onResume() {
        if (gps.hasValidReading) {
            update()
        }
        startGPS()
    }

    fun onPause() {
        stopGPS()
        stopOverrideLocationAutofill()
    }

    fun onDestroy() {
        setLocationSummary = null
        setAccuracySummary = null
        setPermissionVisible = null
        setAutoLocationEnabled = null
        setOverrideEnabled = null
    }

    fun formatLocation(): String {
        return formatService.formatLocation(gps.location)
    }

    fun formatAccuracy(): String? {
        return gps.horizontalAccuracy?.let {
            val accuracyStr = formatService.formatDistance(
                Distance.meters(it).convertTo(prefs.baseDistanceUnits)
            )
            host.getString(R.string.accuracy_distance_format, accuracyStr)
        }
    }

    fun isLocationOverrideEnabled(): Boolean {
        return !isAutoGPSPreferenceEnabled() || prefs.gps.locationSource == GPSLocationSource.Manual
    }

    fun isAutoGPSPreferenceEnabled(): Boolean {
        return sensorService.hasLocationPermission()
    }

    fun goBack() {
        host.goBack()
    }

    fun openAppSettings() {
        host.startActivity(Intents.appSettings(host.requireContext()))
    }

    fun onAutoLocationChanged() {
        resetGPS()
        update()
    }

    fun clearCache() {
        host.lifecycleOwner.lifecycleScope.launch {
            CacheGPSModule.clearCache()
        }
    }

    fun accuracyFilterSummary(): String {
        val filter = prefs.gps.accuracyFilter
        val accuracy = filter.minAccuracy
        val wait = filter.maxAccuracyWait
        return if (accuracy == null || wait == null) {
            host.getString(R.string.none)
        } else {
            host.getString(
                R.string.gps_accuracy_filter_summary,
                formatService.formatElevation(Distance.meters(accuracy)),
                formatService.formatDuration(wait, includeSeconds = true)
            )
        }
    }

    fun showAccuracyFilterDialog(onChanged: () -> Unit) {
        val context = host.requireContext()
        val view = View.inflate(context, R.layout.view_gps_accuracy_filter, null)
        val accuracyLabel = view.findViewById<TextView>(R.id.accuracy_label)
        val waitLabel = view.findViewById<TextView>(R.id.wait_label)
        val accuracySlider = view.findViewById<Slider>(R.id.accuracy_slider)
        val waitSlider = view.findViewById<Slider>(R.id.wait_slider)
        val current = prefs.gps.accuracyFilter
        val distanceUnits = prefs.baseDistanceUnits
        val minimumAccuracy = Distance.meters(GPSAccuracyFilter.MIN_ACCURACY_METERS.toFloat())
            .convertTo(distanceUnits).value.roundToInt()
        val maximumAccuracy = Distance.meters(GPSAccuracyFilter.MAX_ACCURACY_METERS.toFloat())
            .convertTo(distanceUnits).value.roundToInt()

        var accuracy = Distance.meters(
            current.minAccuracy ?: GPSAccuracyFilter.Default.minAccuracy!!
        ).convertTo(distanceUnits).value.roundToInt().coerceIn(minimumAccuracy, maximumAccuracy)
        var wait = current.maxAccuracyWait?.seconds?.toInt()
            ?: GPSAccuracyFilter.Default.maxAccuracyWait!!.seconds.toInt()

        accuracySlider.valueFrom = minimumAccuracy.toFloat()
        accuracySlider.valueTo = maximumAccuracy.toFloat()
        accuracySlider.stepSize = 1f
        accuracySlider.value = accuracy.toFloat()
        accuracySlider.addOnChangeListener { _, value, _ ->
            accuracy = value.toInt()
            accuracyLabel.text = context.getString(
                R.string.gps_accuracy_target,
                formatService.formatDistance(Distance.from(accuracy.toFloat(), distanceUnits))
            )
        }

        waitSlider.valueFrom = GPSAccuracyFilter.MIN_WAIT_SECONDS.toFloat()
        waitSlider.valueTo = GPSAccuracyFilter.MAX_WAIT_SECONDS.toFloat()
        waitSlider.stepSize = 1f
        waitSlider.value = wait.toFloat()
        waitSlider.addOnChangeListener { _, value, _ ->
            wait = value.toInt()
            waitLabel.text = context.getString(
                R.string.gps_accuracy_wait,
                formatService.formatDuration(Duration.ofSeconds(wait.toLong()), includeSeconds = true)
            )
        }

        accuracyLabel.text = context.getString(
            R.string.gps_accuracy_target,
            formatService.formatDistance(Distance.from(accuracy.toFloat(), distanceUnits))
        )
        waitLabel.text = context.getString(
            R.string.gps_accuracy_wait,
            formatService.formatDuration(Duration.ofSeconds(wait.toLong()), includeSeconds = true)
        )

        Alerts.dialog(
            context,
            context.getString(R.string.pref_gps_accuracy_requirement_title),
            contentView = view
        ) { cancelled ->
            if (!cancelled) {
                val accuracyMeters = Distance.from(accuracy.toFloat(), distanceUnits).meters().value
                prefs.gps.accuracyFilter = GPSAccuracyFilter.custom(accuracyMeters, wait)
                onChanged()
            }
        }
    }

    fun setLocationOverride(coordinate: Coordinate?) {
        val updated = coordinate ?: Coordinate.zero
        locationOverride = updated
        prefs.gps.locationOverride = updated
        resetGPS()
        update()
    }

    fun autofillOverrideLocation() {
        if (isOverrideLocationLoading) {
            return
        }
        isOverrideLocationLoading = true
        realGps.start(::onOverrideLocationUpdate)
    }

    fun realLocation(): Coordinate {
        return realGps.location
    }

    fun putBoolean(key: Int, value: Boolean) {
        PreferencesSubsystem.getInstance(host.requireContext()).preferences.putBoolean(
            host.getString(key),
            value
        )
    }

    private fun resetGPS() {
        stopGPS()
        gps = sensorService.getGPS()
        startGPS()
    }

    private fun startGPS() {
        gps.start(::onLocationUpdate)
    }

    private fun stopGPS() {
        gps.stop(::onLocationUpdate)
    }

    private fun onLocationUpdate(): Boolean {
        update()
        return true
    }

    private fun resetRealGPS() {
        stopOverrideLocationAutofill()
        realGps = getRealGPS()
    }

    private fun getRealGPS(): IGPS {
        return when {
            shouldUseRealGPS() -> CustomGPS(host.requireContext())
            shouldUseCachedGPS() -> CachedGPS(host.requireContext())
            else -> OverrideGPS(host.requireContext())
        }
    }

    private fun shouldUseCachedGPS(): Boolean {
        return sensorService.hasLocationPermission() && !GPS.isAvailable(host.requireContext())
    }

    private fun shouldUseRealGPS(): Boolean {
        return GPS.isAvailable(host.requireContext())
    }

    private fun onOverrideLocationUpdate(): Boolean {
        setLocationOverride(realGps.location)
        isOverrideLocationLoading = false
        return false
    }

    private fun stopOverrideLocationAutofill() {
        realGps.stop(::onOverrideLocationUpdate)
        isOverrideLocationLoading = false
    }

    private fun update() {
        if (throttle.isThrottled()) {
            return
        }

        val useReal = shouldUseRealGPS()
        val useCached = shouldUseCachedGPS()

        if (useReal != wasUsingRealGPS || useCached != wasUsingCachedGPS) {
            resetRealGPS()
            resetGPS()
            wasUsingCachedGPS = useCached
            wasUsingRealGPS = useReal
        }

        setPermissionVisible?.invoke(!isAutoGPSPreferenceEnabled())
        setAutoLocationEnabled?.invoke(isAutoGPSPreferenceEnabled())
        setOverrideEnabled?.invoke(isLocationOverrideEnabled())
        setLocationSummary?.invoke(formatService.formatLocation(gps.location))
        setAccuracySummary?.invoke(formatAccuracy())
    }
}

@Composable
private fun CalibrateGPSContent(controller: CalibrateGPSController) {
    val prefs = controller.prefs
    var locationSummary by remember { mutableStateOf(controller.formatLocation()) }
    var accuracySummary by remember { mutableStateOf(controller.formatAccuracy()) }
    var autoLocation by remember {
        mutableStateOf(prefs.gps.locationSource == GPSLocationSource.GPS)
    }
    var accuracyFilterSummary by remember { mutableStateOf(controller.accuracyFilterSummary()) }
    var permissionVisible by remember { mutableStateOf(!controller.isAutoGPSPreferenceEnabled()) }
    var autoLocationEnabled by remember { mutableStateOf(controller.isAutoGPSPreferenceEnabled()) }
    var overrideEnabled by remember { mutableStateOf(controller.isLocationOverrideEnabled()) }
    val context = LocalContext.current
    val cache = remember(context) { PreferencesSubsystem.getInstance(context).preferences }
    var powerMode by remember { mutableStateOf(prefs.gps.powerMode) }
    var smoothing by remember { mutableIntStateOf(prefs.gps.smoothing) }
    val gpsSettingsEnabled = autoLocationEnabled && autoLocation
    val powerModes = listOf(
        TrailSenseChoice(GPSPowerMode.Low, stringResource(R.string.gps_power_usage_low)),
        TrailSenseChoice(GPSPowerMode.Balanced, stringResource(R.string.gps_power_usage_balanced)),
        TrailSenseChoice(GPSPowerMode.High, stringResource(R.string.gps_power_usage_high))
    )

    controller.setLocationSummary { locationSummary = it }
    controller.setAccuracySummary { accuracySummary = it }
    controller.setPermissionVisible { permissionVisible = it }
    controller.setAutoLocationEnabled { autoLocationEnabled = it }
    controller.setOverrideEnabled { overrideEnabled = it }

    val gpsTitle = stringResource(R.string.gps)
    val overrideTitle = stringResource(R.string.pref_gps_override_title)
    val permissionFooter = stringResource(R.string.gps_permission_explanation)
    val overrideFooter = stringResource(R.string.gps_override_explanation)
    val gpsFooter = stringResource(R.string.gps_accuracy_tip)
    val powerTitle = stringResource(R.string.pref_gps_power_usage_title)
    val satelliteIcon = painterResource(R.drawable.satellite)
    val usingGps = autoLocation && autoLocationEnabled

    TrailSensePage(
        title = gpsTitle,
        onBack = controller::goBack,
        backLabel = stringResource(R.string.back)
    ) {
        item("hero") {
            CalibrationHero(
                label = stringResource(R.string.location),
                value = locationSummary,
                icon = satelliteIcon,
                status = stringResource(if (usingGps) R.string.gps else R.string.manual),
                supportingText = accuracySummary.takeIf { usingGps },
                valueStyle = MaterialTheme.typography.headlineSmall
            )
        }

        if (permissionVisible) {
            trailSenseSection("permission", footer = permissionFooter) {
                row { position ->
                    SettingsActionRow(
                        label = stringResource(R.string.request_permission),
                        onClick = controller::openAppSettings,
                        position = position
                    )
                }
            }
        }

        trailSenseSection("gps", title = gpsTitle, footer = gpsFooter.takeIf { gpsSettingsEnabled }) {
            if (autoLocationEnabled) {
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_auto_location_title),
                        checked = autoLocation,
                        onCheckedChange = {
                            autoLocation = it
                            prefs.gps.locationSource =
                                if (it) GPSLocationSource.GPS else GPSLocationSource.Manual
                            overrideEnabled = controller.isLocationOverrideEnabled()
                            controller.onAutoLocationChanged()
                        },
                        position = position
                    )
                }
            }
            if (gpsSettingsEnabled) {
                row { position ->
                    TrailSenseGroupCard(position = position) {
                        Text(
                            text = powerTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = TrailSenseSpacing.md)
                        )
                        TrailSenseConnectedChoice(
                            options = powerModes,
                            selected = powerMode,
                            label = powerTitle,
                            onSelect = { selected ->
                                if (selected != powerMode) {
                                    cache.putString(
                                        context.getString(R.string.pref_gps_power_usage),
                                        selected.id.toString()
                                    )
                                    powerMode = selected
                                    controller.onAutoLocationChanged()
                                }
                            }
                        )
                    }
                }
                row { position ->
                    CalibrationSliderRow(
                        title = stringResource(R.string.smoothing),
                        summary = stringResource(R.string.smoothing_summary),
                        value = smoothing,
                        range = 0..100,
                        onValueChange = { updated ->
                            smoothing = updated
                            cache.putInt(context.getString(R.string.pref_gps_smoothing), updated)
                        },
                        position = position
                    )
                }
            }
            if (autoLocation) {
                row { position ->
                    SettingsPickerRow(
                        label = stringResource(R.string.pref_gps_accuracy_requirement_title),
                        selected = accuracyFilterSummary,
                        onClick = {
                            controller.showAccuracyFilterDialog {
                                accuracyFilterSummary = controller.accuracyFilterSummary()
                            }
                        },
                        position = position
                    )
                }
                row { position ->
                    SettingsActionRow(
                        label = stringResource(R.string.clear_cache),
                        onClick = controller::clearCache,
                        position = position
                    )
                }
            }
        }

        if (overrideEnabled) {
            trailSenseSection("override", title = overrideTitle, footer = overrideFooter) {
                row { position ->
                    TrailSenseGroupCard(
                        position = position,
                        contentPadding = PaddingValues(
                            start = TrailSenseSpacing.sm,
                            end = TrailSenseSpacing.sm,
                            top = TrailSenseSpacing.lg,
                            bottom = TrailSenseSpacing.xs
                        )
                    ) {
                        GPSOverrideCoordinateInput(
                            coordinate = controller.locationOverride,
                            enabled = true,
                            isLoading = controller.isOverrideLocationLoading,
                            locationProvider = controller::realLocation,
                            onCoordinateChange = controller::setLocationOverride,
                            onAutoFill = controller::autofillOverrideLocation
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GPSOverrideCoordinateInput(
    coordinate: Coordinate,
    enabled: Boolean,
    isLoading: Boolean,
    locationProvider: () -> Coordinate,
    onCoordinateChange: (Coordinate?) -> Unit,
    onAutoFill: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formatter = remember { FormatService.getInstance(context) }
    val scope = rememberCoroutineScope()
    var coordinateText by remember {
        mutableStateOf(formatter.formatLocation(coordinate))
    }
    val parsedCoordinate = remember(coordinateText) {
        Coordinate.parse(coordinateText)
    }
    var showHelp by remember { mutableStateOf(false) }

    LaunchedEffect(coordinate) {
        if (coordinate != Coordinate.zero && coordinate != parsedCoordinate) {
            coordinateText = formatter.formatLocation(coordinate)
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(40.dp)
                    .testTag("gps_loading")
            )
        } else {
            IconButton(
                onClick = {
                    Pickers.item(
                        context,
                        context.getString(R.string.autofill_source),
                        listOf(
                            context.getString(R.string.gps),
                            context.getString(R.string.beacon)
                        ),
                        0
                    ) { selection ->
                        when (selection) {
                            0 -> onAutoFill()
                            1 -> scope.launch {
                                val beacon = BeaconPickers.pickBeacon(
                                    context,
                                    sort = ClosestBeaconSort(
                                        BeaconService(context),
                                        locationProvider
                                    )
                                ) ?: return@launch
                                onCoordinateChange(beacon.coordinate)
                            }
                        }
                    }
                },
                enabled = enabled,
                modifier = Modifier
                    .size(TrailSenseSize.minimumTouchTarget)
                    .testTag("gps_btn")
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_location),
                    contentDescription = stringResource(R.string.gps)
                )
            }
        }
        Spacer(modifier = Modifier.size(8.dp))
        OutlinedTextField(
            value = coordinateText,
            onValueChange = {
                coordinateText = it
                onCoordinateChange(Coordinate.parse(it))
            },
            modifier = Modifier
                .weight(1f)
                .testTag("coordinate_input"),
            label = { Text(stringResource(R.string.location)) },
            isError = coordinateText.isNotBlank() && parsedCoordinate == null,
            supportingText = {
                if (coordinateText.isNotBlank() && parsedCoordinate == null) {
                    Text(stringResource(R.string.coordinate_input_invalid_location))
                }
            },
            singleLine = true,
            enabled = enabled && !isLoading
        )
        Spacer(modifier = Modifier.size(8.dp))
        IconButton(
            onClick = { showHelp = true },
            enabled = enabled,
            modifier = Modifier
                .size(TrailSenseSize.minimumTouchTarget)
                .testTag("coordinate_input_help_btn")
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_help),
                contentDescription = stringResource(R.string.location_input_help_title)
            )
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            title = { Text(stringResource(R.string.location_input_help_title)) },
            text = { Text(stringResource(R.string.location_input_help)) }
        )
    }
}
