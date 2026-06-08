package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.beacons.infrastructure.BeaconPickers
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.ClosestBeaconSort
import kotlinx.coroutines.launch

class CalibrateGPSFragment : TrailSenseComposeFragment() {

    private val prefs by lazy { UserPreferences(requireContext()) }
    private val sensorService by lazy { SensorService(requireContext()) }
    private val throttle = Throttle(20)
    private val formatService by lazy { FormatService.getInstance(requireContext()) }

    private lateinit var gps: IGPS
    private lateinit var realGps: IGPS
    private var locationOverride by mutableStateOf(Coordinate.zero)
    private var isOverrideLocationLoading by mutableStateOf(false)

    private var wasUsingRealGPS = false
    private var wasUsingCachedGPS = false

    private var setLocationSummary: ((String) -> Unit)? = null
    private var setPermissionVisible: ((Boolean) -> Unit)? = null
    private var setAutoLocationEnabled: ((Boolean) -> Unit)? = null
    private var setOverrideEnabled: ((Boolean) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wasUsingRealGPS = shouldUseRealGPS()
        wasUsingCachedGPS = shouldUseCachedGPS()
        gps = sensorService.getGPS()
        realGps = getRealGPS()
        locationOverride = prefs.locationOverride
    }

    @Composable
    override fun FragmentContent() {
        var locationSummary by remember {
            mutableStateOf(formatService.formatLocation(gps.location))
        }
        var autoLocation by remember { mutableStateOf(prefs.useAutoLocation) }
        var requireSatellites by remember { mutableStateOf(prefs.requiresSatellites) }
        var permissionVisible by remember {
            mutableStateOf(!isAutoGPSPreferenceEnabled())
        }
        var autoLocationEnabled by remember {
            mutableStateOf(isAutoGPSPreferenceEnabled())
        }
        var overrideEnabled by remember {
            mutableStateOf(isLocationOverrideEnabled())
        }

        setLocationSummary = { locationSummary = it }
        setPermissionVisible = { permissionVisible = it }
        setAutoLocationEnabled = { autoLocationEnabled = it }
        setOverrideEnabled = { overrideEnabled = it }

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
                            overrideEnabled = isLocationOverrideEnabled()
                            resetGPS()
                            update()
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
                            putBoolean(R.string.pref_require_satellites, it)
                        },
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                    TrailSenseDivider()
                }
                if (permissionVisible) {
                    item("permission") {
                        SettingsActionRow(
                            label = stringResource(R.string.request_permission),
                            onClick = {
                                startActivity(Intents.appSettings(requireContext()))
                            },
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
                        onClick = ::clearCache,
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
                            coordinate = locationOverride,
                            enabled = overrideEnabled,
                            isLoading = isOverrideLocationLoading,
                            locationProvider = { realGps.location },
                            onCoordinateChange = ::setLocationOverride,
                            onAutoFill = ::autofillOverrideLocation,
                            modifier = Modifier.padding(top = TrailSenseSpacing.sm)
                        )
                    }
                    TrailSenseDivider()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (gps.hasValidReading) {
            update()
        }
        startGPS()
    }

    override fun onPause() {
        super.onPause()
        stopGPS()
        stopOverrideLocationAutofill()
    }

    override fun onDestroyView() {
        setLocationSummary = null
        setPermissionVisible = null
        setAutoLocationEnabled = null
        setOverrideEnabled = null
        super.onDestroyView()
    }

    private fun resetGPS() {
        stopGPS()
        gps = sensorService.getGPS()
        startGPS()
    }

    private fun startGPS() {
        gps.start(this::onLocationUpdate)
    }

    private fun stopGPS() {
        gps.stop(this::onLocationUpdate)
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
            shouldUseRealGPS() -> CustomGPS(requireContext())
            shouldUseCachedGPS() -> CachedGPS(requireContext())
            else -> OverrideGPS(requireContext())
        }
    }

    private fun isLocationOverrideEnabled(): Boolean {
        return !isAutoGPSPreferenceEnabled() || !prefs.useAutoLocation
    }

    private fun isAutoGPSPreferenceEnabled(): Boolean {
        return sensorService.hasLocationPermission()
    }

    private fun shouldUseCachedGPS(): Boolean {
        return sensorService.hasLocationPermission() && !GPS.isAvailable(requireContext())
    }

    private fun shouldUseRealGPS(): Boolean {
        return GPS.isAvailable(requireContext())
    }

    private fun clearCache() {
        val cache = PreferencesSubsystem.getInstance(requireContext()).preferences
        cache.remove(CustomGPS.LAST_ALTITUDE)
        cache.remove(CustomGPS.LAST_UPDATE)
        cache.remove(CustomGPS.LAST_SPEED)
        cache.remove(CustomGPS.LAST_LONGITUDE)
        cache.remove(CustomGPS.LAST_LATITUDE)
    }

    private fun setLocationOverride(coordinate: Coordinate?) {
        val updated = coordinate ?: Coordinate.zero
        locationOverride = updated
        prefs.locationOverride = updated
        resetGPS()
        update()
    }

    private fun autofillOverrideLocation() {
        if (isOverrideLocationLoading) {
            return
        }
        isOverrideLocationLoading = true
        realGps.start(this::onOverrideLocationUpdate)
    }

    private fun onOverrideLocationUpdate(): Boolean {
        setLocationOverride(realGps.location)
        isOverrideLocationLoading = false
        return false
    }

    private fun stopOverrideLocationAutofill() {
        if (::realGps.isInitialized) {
            realGps.stop(this::onOverrideLocationUpdate)
        }
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

    private fun putBoolean(key: Int, value: Boolean) {
        PreferencesSubsystem.getInstance(requireContext()).preferences.putBoolean(
            getString(key),
            value
        )
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
