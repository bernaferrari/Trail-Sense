@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.sense.location.GPS
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.luna.time.Throttle
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.CustomGPS
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedGPS
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideGPS
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.beacons.infrastructure.BeaconPickers
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.ClosestBeaconSort
import kotlinx.coroutines.launch

@Composable
fun CalibrateGPSToolScreen(fragment: Fragment) {
    val controller = remember(fragment) { CalibrateGPSController(fragment) }
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

private class CalibrateGPSController(private val fragment: Fragment) {

    val prefs by lazy { UserPreferences(fragment.requireContext()) }
    private val sensorService by lazy { SensorService(fragment.requireContext()) }
    private val throttle = Throttle(20)
    private val formatService by lazy { FormatService.getInstance(fragment.requireContext()) }

    private var gps: IGPS = sensorService.getGPS()
    private var realGps: IGPS = getRealGPS()
    var locationOverride by mutableStateOf(prefs.locationOverride)
        private set
    var isOverrideLocationLoading by mutableStateOf(false)
        private set

    private var wasUsingRealGPS = shouldUseRealGPS()
    private var wasUsingCachedGPS = shouldUseCachedGPS()

    private var setLocationSummary: ((String) -> Unit)? = null
    private var setPermissionVisible: ((Boolean) -> Unit)? = null
    private var setAutoLocationEnabled: ((Boolean) -> Unit)? = null
    private var setOverrideEnabled: ((Boolean) -> Unit)? = null

    fun setLocationSummary(updater: (String) -> Unit) {
        setLocationSummary = updater
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
        setPermissionVisible = null
        setAutoLocationEnabled = null
        setOverrideEnabled = null
    }

    fun formatLocation(): String {
        return formatService.formatLocation(gps.location)
    }

    fun isLocationOverrideEnabled(): Boolean {
        return !isAutoGPSPreferenceEnabled() || !prefs.useAutoLocation
    }

    fun isAutoGPSPreferenceEnabled(): Boolean {
        return sensorService.hasLocationPermission()
    }

    fun openAppSettings() {
        fragment.startActivity(Intents.appSettings(fragment.requireContext()))
    }

    fun onAutoLocationChanged() {
        resetGPS()
        update()
    }

    fun clearCache() {
        val cache = PreferencesSubsystem.getInstance(fragment.requireContext()).preferences
        cache.remove(CustomGPS.LAST_ALTITUDE)
        cache.remove(CustomGPS.LAST_UPDATE)
        cache.remove(CustomGPS.LAST_SPEED)
        cache.remove(CustomGPS.LAST_LONGITUDE)
        cache.remove(CustomGPS.LAST_LATITUDE)
    }

    fun setLocationOverride(coordinate: Coordinate?) {
        val updated = coordinate ?: Coordinate.zero
        locationOverride = updated
        prefs.locationOverride = updated
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
        PreferencesSubsystem.getInstance(fragment.requireContext()).preferences.putBoolean(
            fragment.getString(key),
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
            shouldUseRealGPS() -> CustomGPS(fragment.requireContext())
            shouldUseCachedGPS() -> CachedGPS(fragment.requireContext())
            else -> OverrideGPS(fragment.requireContext())
        }
    }

    private fun shouldUseCachedGPS(): Boolean {
        return sensorService.hasLocationPermission() && !GPS.isAvailable(fragment.requireContext())
    }

    private fun shouldUseRealGPS(): Boolean {
        return GPS.isAvailable(fragment.requireContext())
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
    }
}

@Composable
private fun CalibrateGPSContent(controller: CalibrateGPSController) {
        val prefs = controller.prefs
        var locationSummary by remember {
            mutableStateOf(controller.formatLocation())
        }
        var autoLocation by remember { mutableStateOf(prefs.useAutoLocation) }
        var requireSatellites by remember { mutableStateOf(prefs.requiresSatellites) }
        var permissionVisible by remember {
            mutableStateOf(!controller.isAutoGPSPreferenceEnabled())
        }
        var autoLocationEnabled by remember {
            mutableStateOf(controller.isAutoGPSPreferenceEnabled())
        }
        var overrideEnabled by remember {
            mutableStateOf(controller.isLocationOverrideEnabled())
        }

        controller.setLocationSummary { locationSummary = it }
        controller.setPermissionVisible { permissionVisible = it }
        controller.setAutoLocationEnabled { autoLocationEnabled = it }
        controller.setOverrideEnabled { overrideEnabled = it }

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
                    TrailSenseSectionHeader(title = stringResource(R.string.gps))
                }
                item("location") {
                    TrailSenseListItem(
                        title = stringResource(R.string.location),
                        subtitle = locationSummary,
                        icon = painterResource(R.drawable.satellite),
                        animateEntrance = false,
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                    TrailSenseDivider()
                }
                item("auto_location") {
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_auto_location_title),
                        checked = autoLocation,
                        enabled = autoLocationEnabled,
                        onCheckedChange = {
                            autoLocation = it
                            prefs.useAutoLocation = it
                            overrideEnabled = controller.isLocationOverrideEnabled()
                            controller.onAutoLocationChanged()
                        },
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                    TrailSenseDivider()
                }
                item("require_satellites") {
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_require_satellites_title),
                        summary = stringResource(R.string.pref_require_satellites_description),
                        checked = requireSatellites,
                        enabled = autoLocation,
                        onCheckedChange = {
                            requireSatellites = it
                            controller.putBoolean(R.string.pref_require_satellites, it)
                        },
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                    TrailSenseDivider()
                }
                if (permissionVisible) {
                    item("permission") {
                        SettingsActionRow(
                            label = stringResource(R.string.request_permission),
                            onClick = controller::openAppSettings,
                            modifier = Modifier.padding(
                                horizontal = TrailSenseSpacing.lg
                            )
                        )
                    }
                }
                item("clear_cache") {
                    SettingsActionRow(
                        label = stringResource(R.string.clear_cache),
                        enabled = autoLocation,
                        onClick = controller::clearCache,
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                }
                item("override") {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = TrailSenseSpacing.xl,
                            vertical = TrailSenseSpacing.md
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.pref_gps_override_title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        GPSOverrideCoordinateInput(
                            coordinate = controller.locationOverride,
                            enabled = overrideEnabled,
                            isLoading = controller.isOverrideLocationLoading,
                            locationProvider = controller::realLocation,
                            onCoordinateChange = controller::setLocationOverride,
                            onAutoFill = controller::autofillOverrideLocation,
                            modifier = Modifier.padding(top = TrailSenseSpacing.sm)
                        )
                    }
                    TrailSenseDivider()
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
                modifier = Modifier.testTag("gps_btn")
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
            modifier = Modifier.testTag("coordinate_input_help_btn")
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
