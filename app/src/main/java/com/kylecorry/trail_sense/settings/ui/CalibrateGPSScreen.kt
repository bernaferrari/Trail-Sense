@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.settings.ui

import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDialog
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun CalibrateGPSToolScreen() {
    val context = LocalContext.current.applicationContext
    val viewModel = viewModel {
        CalibrateGPSViewModel(
            prefs = UserPreferences(context),
            sensorService = SensorService(context),
            formatService = FormatService.getInstance(context),
            isGpsAvailable = { GPS.isAvailable(context) },
            createRealGps = { source ->
                when (source) {
                    RealGpsSource.Device -> CustomGPS(context)
                    RealGpsSource.Cached -> CachedGPS(context)
                    RealGpsSource.Override -> OverrideGPS(context)
                }
            }
        )
    }

    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose { viewModel.onPause() }
    }

    CalibrateGPSContent(viewModel)
}

/** Where the override's GPS autofill reads its location from. */
internal enum class RealGpsSource { Device, Cached, Override }

/**
 * The GPS page's state: the location and its accuracy while the page is resumed, whether location permission and the
 * override apply, the accuracy filter, and filling the override from the real GPS.
 */
internal class CalibrateGPSViewModel(
    val prefs: UserPreferences,
    private val sensorService: SensorService,
    private val formatService: FormatService,
    private val isGpsAvailable: () -> Boolean,
    private val createRealGps: (RealGpsSource) -> IGPS
) : ViewModel() {

    private val throttle = Throttle(20)

    private var gps: IGPS = sensorService.getGPS()
    private var realGps: IGPS = getRealGPS()
    var locationOverride by mutableStateOf(prefs.gps.locationOverride)
        private set
    var isOverrideLocationLoading by mutableStateOf(false)
        private set

    var locationSummary by mutableStateOf(formatService.formatLocation(gps.location))
        private set

    /** The horizontal accuracy as a distance in the user's units, or null when the GPS doesn't report one. */
    var accuracy by mutableStateOf(formatAccuracy())
        private set
    var permissionVisible by mutableStateOf(!isAutoGPSPreferenceEnabled())
        private set
    var autoLocationEnabled by mutableStateOf(isAutoGPSPreferenceEnabled())
        private set
    var overrideEnabled by mutableStateOf(isLocationOverrideEnabled())
        private set
    var accuracyFilter by mutableStateOf(prefs.gps.accuracyFilter)
        private set

    private var wasUsingRealGPS = shouldUseRealGPS()
    private var wasUsingCachedGPS = shouldUseCachedGPS()

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

    override fun onCleared() {
        onPause()
    }

    private fun formatAccuracy(): String? {
        return gps.horizontalAccuracy?.let {
            formatService.formatDistance(
                Distance.meters(it).convertTo(prefs.baseDistanceUnits)
            )
        }
    }

    private fun isLocationOverrideEnabled(): Boolean {
        return !isAutoGPSPreferenceEnabled() || prefs.gps.locationSource == GPSLocationSource.Manual
    }

    private fun isAutoGPSPreferenceEnabled(): Boolean {
        return sensorService.hasLocationPermission()
    }

    fun setAutoLocation(enabled: Boolean) {
        prefs.gps.locationSource = if (enabled) GPSLocationSource.GPS else GPSLocationSource.Manual
        overrideEnabled = isLocationOverrideEnabled()
        onAutoLocationChanged()
    }

    fun onAutoLocationChanged() {
        resetGPS()
        update()
    }

    fun clearCache() {
        viewModelScope.launch {
            CacheGPSModule.clearCache()
        }
    }

    /** The accuracy slider's range, whole steps in the user's distance units. */
    fun accuracyFilterRange(): IntRange {
        val units = prefs.baseDistanceUnits
        val minimum = Distance.meters(GPSAccuracyFilter.MIN_ACCURACY_METERS.toFloat())
            .convertTo(units).value.roundToInt()
        val maximum = Distance.meters(GPSAccuracyFilter.MAX_ACCURACY_METERS.toFloat())
            .convertTo(units).value.roundToInt()
        return minimum..maximum
    }

    /** The saved (or default) target accuracy, in the user's distance units, and wait, in seconds. */
    fun currentAccuracyFilter(): Pair<Int, Int> {
        val current = prefs.gps.accuracyFilter
        val range = accuracyFilterRange()
        val accuracy = Distance.meters(
            current.minAccuracy ?: GPSAccuracyFilter.Default.minAccuracy!!
        ).convertTo(prefs.baseDistanceUnits).value.roundToInt().coerceIn(range.first, range.last)
        val wait = current.maxAccuracyWait?.seconds?.toInt()
            ?: GPSAccuracyFilter.Default.maxAccuracyWait!!.seconds.toInt()
        return accuracy to wait
    }

    /** A target accuracy in the user's distance units, as a distance. */
    fun accuracyTargetDistance(accuracy: Int): Distance {
        return Distance.from(accuracy.toFloat(), prefs.baseDistanceUnits)
    }

    fun saveAccuracyFilter(accuracy: Int, wait: Int) {
        val accuracyMeters = Distance.from(accuracy.toFloat(), prefs.baseDistanceUnits).meters().value
        prefs.gps.accuracyFilter = GPSAccuracyFilter.custom(accuracyMeters, wait)
        accuracyFilter = prefs.gps.accuracyFilter
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
        return createRealGps(
            when {
                shouldUseRealGPS() -> RealGpsSource.Device
                shouldUseCachedGPS() -> RealGpsSource.Cached
                else -> RealGpsSource.Override
            }
        )
    }

    private fun shouldUseCachedGPS(): Boolean {
        return sensorService.hasLocationPermission() && !isGpsAvailable()
    }

    private fun shouldUseRealGPS(): Boolean {
        return isGpsAvailable()
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

        permissionVisible = !isAutoGPSPreferenceEnabled()
        autoLocationEnabled = isAutoGPSPreferenceEnabled()
        overrideEnabled = isLocationOverrideEnabled()
        locationSummary = formatService.formatLocation(gps.location)
        accuracy = formatAccuracy()
    }
}

@Composable
private fun CalibrateGPSContent(viewModel: CalibrateGPSViewModel) {
    val prefs = viewModel.prefs
    val context = LocalContext.current
    val formatService = remember(context) { FormatService.getInstance(context) }
    val goBack = rememberGoBack()
    val locationSummary = viewModel.locationSummary
    val accuracySummary = viewModel.accuracy?.let { stringResource(R.string.accuracy_distance_format, it) }
    var autoLocation by remember {
        mutableStateOf(prefs.gps.locationSource == GPSLocationSource.GPS)
    }
    val filter = viewModel.accuracyFilter
    val filterAccuracy = filter.minAccuracy
    val filterWait = filter.maxAccuracyWait
    val accuracyFilterSummary = if (filterAccuracy == null || filterWait == null) {
        stringResource(R.string.none)
    } else {
        stringResource(
            R.string.gps_accuracy_filter_summary,
            formatService.formatElevation(Distance.meters(filterAccuracy)),
            formatService.formatDuration(filterWait, includeSeconds = true)
        )
    }
    var showAccuracyFilter by remember { mutableStateOf(false) }
    val permissionVisible = viewModel.permissionVisible
    val autoLocationEnabled = viewModel.autoLocationEnabled
    val overrideEnabled = viewModel.overrideEnabled
    val cache = remember(context) { PreferencesSubsystem.getInstance(context).preferences }
    var powerMode by remember { mutableStateOf(prefs.gps.powerMode) }
    var smoothing by remember { mutableIntStateOf(prefs.gps.smoothing) }
    val gpsSettingsEnabled = autoLocationEnabled && autoLocation
    val powerModes = listOf(
        TrailSenseChoice(GPSPowerMode.Low, stringResource(R.string.gps_power_usage_low)),
        TrailSenseChoice(GPSPowerMode.Balanced, stringResource(R.string.gps_power_usage_balanced)),
        TrailSenseChoice(GPSPowerMode.High, stringResource(R.string.gps_power_usage_high))
    )

    val gpsTitle = stringResource(R.string.gps)
    val overrideTitle = stringResource(R.string.pref_gps_override_title)
    val permissionFooter = stringResource(R.string.gps_permission_explanation)
    val overrideFooter = stringResource(R.string.gps_override_explanation)
    val gpsFooter = stringResource(R.string.gps_accuracy_tip)
    val powerTitle = stringResource(R.string.pref_gps_power_usage_title)
    val satelliteIcon = rememberVectorPainter(TrailIcons.Satellite)
    val usingGps = autoLocation && autoLocationEnabled

    TrailSensePage(
        title = gpsTitle,
        onBack = goBack,
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
                        onClick = { context.startActivity(Intents.appSettings(context)) },
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
                            viewModel.setAutoLocation(it)
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
                            style = MaterialTheme.typography.titleMedium,
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
                                    viewModel.onAutoLocationChanged()
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
                        onClick = { showAccuracyFilter = true },
                        position = position
                    )
                }
                row { position ->
                    SettingsActionRow(
                        label = stringResource(R.string.clear_cache),
                        onClick = viewModel::clearCache,
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
                            coordinate = viewModel.locationOverride,
                            enabled = true,
                            isLoading = viewModel.isOverrideLocationLoading,
                            locationProvider = viewModel::realLocation,
                            onCoordinateChange = viewModel::setLocationOverride,
                            onAutoFill = viewModel::autofillOverrideLocation
                        )
                    }
                }
            }
        }
    }

    if (showAccuracyFilter) {
        GPSAccuracyFilterDialog(
            viewModel = viewModel,
            onDismiss = { showAccuracyFilter = false },
            onSave = { accuracy, wait ->
                showAccuracyFilter = false
                viewModel.saveAccuracyFilter(accuracy, wait)
            }
        )
    }
}

/** The target accuracy and how long to wait for it, each on a slider with its value above. */
@Composable
private fun GPSAccuracyFilterDialog(
    viewModel: CalibrateGPSViewModel,
    onDismiss: () -> Unit,
    onSave: (accuracy: Int, wait: Int) -> Unit
) {
    val context = LocalContext.current
    val formatService = remember(context) { FormatService.getInstance(context) }
    val range = remember(viewModel) { viewModel.accuracyFilterRange() }
    val initial = remember(viewModel) { viewModel.currentAccuracyFilter() }
    var accuracy by remember { mutableIntStateOf(initial.first) }
    var wait by remember { mutableIntStateOf(initial.second) }
    val waitRange = GPSAccuracyFilter.MIN_WAIT_SECONDS..GPSAccuracyFilter.MAX_WAIT_SECONDS

    TrailSenseDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.pref_gps_accuracy_requirement_title),
        confirmText = stringResource(android.R.string.ok),
        onConfirm = { onSave(accuracy, wait) },
        dismissText = stringResource(android.R.string.cancel)
    ) {
        Column {
            Text(
                text = stringResource(
                    R.string.gps_accuracy_target,
                    formatService.formatDistance(viewModel.accuracyTargetDistance(accuracy))
                ),
                style = MaterialTheme.typography.bodyLarge
            )
            ComposeSlider(
                value = accuracy.toFloat(),
                onValueChange = { accuracy = it.roundToInt().coerceIn(range.first, range.last) },
                valueRange = range.first.toFloat()..range.last.toFloat()
            )
            Text(
                text = stringResource(
                    R.string.gps_accuracy_wait,
                    formatService.formatDuration(Duration.ofSeconds(wait.toLong()), includeSeconds = true)
                ),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = TrailSenseSpacing.sm)
            )
            ComposeSlider(
                value = wait.toFloat(),
                onValueChange = { wait = it.roundToInt().coerceIn(waitRange.first, waitRange.last) },
                valueRange = waitRange.first.toFloat()..waitRange.last.toFloat()
            )
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
                    imageVector = TrailIcons.Location,
                    contentDescription = stringResource(R.string.gps)
                )
            }
        }
        Spacer(modifier = Modifier.size(8.dp))
        TrailSenseInputField(
            value = coordinateText,
            onValueChange = {
                coordinateText = it
                onCoordinateChange(Coordinate.parse(it))
            },
            modifier = Modifier
                .weight(1f)
                .testTag("coordinate_input"),
            label = stringResource(R.string.location),
            keyboardType = KeyboardType.Text,
            isError = coordinateText.isNotBlank() && parsedCoordinate == null,
            errorText = stringResource(R.string.coordinate_input_invalid_location),
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
                imageVector = TrailIcons.Help,
                contentDescription = stringResource(R.string.location_input_help_title)
            )
        }
    }

    if (showHelp) {
        TrailSenseDialog(
            onDismissRequest = { showHelp = false },
            title = stringResource(R.string.location_input_help_title),
            confirmText = stringResource(android.R.string.ok)
        ) {
            Text(stringResource(R.string.location_input_help))
        }
    }
}
