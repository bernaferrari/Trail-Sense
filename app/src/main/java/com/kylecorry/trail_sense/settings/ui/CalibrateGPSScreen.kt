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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.pickers.Pickers
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
import com.kylecorry.trail_sense.shared.sensors.overrides.CachedGPS
import com.kylecorry.trail_sense.shared.sensors.overrides.OverrideGPS
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
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
    var locationOverride by mutableStateOf(prefs.locationOverride)
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
        return !isAutoGPSPreferenceEnabled() || !prefs.useAutoLocation
    }

    fun isAutoGPSPreferenceEnabled(): Boolean {
        return sensorService.hasLocationPermission()
    }

    fun openAppSettings() {
        host.startActivity(Intents.appSettings(host.requireContext()))
    }

    fun onAutoLocationChanged() {
        resetGPS()
        update()
    }

    fun clearCache() {
        val cache = PreferencesSubsystem.getInstance(host.requireContext()).preferences
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
        var locationSummary by remember {
            mutableStateOf(controller.formatLocation())
        }
        var accuracySummary by remember {
            mutableStateOf(controller.formatAccuracy())
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
        controller.setAccuracySummary { accuracySummary = it }
        controller.setPermissionVisible { permissionVisible = it }
        controller.setAutoLocationEnabled { autoLocationEnabled = it }
        controller.setOverrideEnabled { overrideEnabled = it }

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
                    verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xl)
                ) {
                item("title") {
                    Text(
                        text = stringResource(R.string.gps),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(
                            top = TrailSenseSpacing.sm,
                            bottom = TrailSenseSpacing.xs
                        )
                    )
                }

                item("status_hero") {
                    GPSStatusHero(
                        location = locationSummary,
                        accuracy = accuracySummary
                    )
                }

                item("gps_settings") {
                    CalibrateGPSSection(title = stringResource(R.string.gps)) {
                        GPSChipSwitchRow(
                            icon = painterResource(R.drawable.ic_location),
                            label = stringResource(R.string.pref_auto_location_title),
                            checked = autoLocation,
                            enabled = autoLocationEnabled,
                            onCheckedChange = {
                                autoLocation = it
                                prefs.useAutoLocation = it
                                overrideEnabled = controller.isLocationOverrideEnabled()
                                controller.onAutoLocationChanged()
                            }
                        )
                        TrailSenseDivider()
                        GPSChipSwitchRow(
                            icon = painterResource(R.drawable.satellite),
                            label = stringResource(R.string.pref_require_satellites_title),
                            summary = stringResource(R.string.pref_require_satellites_description),
                            checked = requireSatellites,
                            enabled = autoLocation,
                            onCheckedChange = {
                                requireSatellites = it
                                controller.putBoolean(R.string.pref_require_satellites, it)
                            }
                        )
                        TrailSenseDivider()
                        if (permissionVisible) {
                            GPSChipActionRow(
                                icon = painterResource(R.drawable.lock),
                                label = stringResource(R.string.request_permission),
                                onClick = controller::openAppSettings
                            )
                            TrailSenseDivider()
                        }
                        GPSChipActionRow(
                            icon = painterResource(R.drawable.ic_delete),
                            label = stringResource(R.string.clear_cache),
                            enabled = autoLocation,
                            onClick = controller::clearCache
                        )
                    }
                }

                item("override") {
                    CalibrateGPSSection(
                        title = stringResource(R.string.pref_gps_override_title)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(TrailSenseShape.groupInner),
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Column(
                                modifier = Modifier.padding(
                                horizontal = TrailSenseSpacing.lg,
                                vertical = TrailSenseSpacing.lg
                            )
                            ) {
                            GPSOverrideCoordinateInput(
                                coordinate = controller.locationOverride,
                                enabled = overrideEnabled,
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
        }
}

@Composable
private fun GPSStatusHero(
    location: String,
    accuracy: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(TrailSenseSpacing.xl),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.satellite),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.width(TrailSenseSpacing.lg))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
            ) {
                Text(
                    text = stringResource(R.string.location),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = location,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (accuracy != null) {
                    Text(
                        text = accuracy,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun GPSIconChip(icon: Painter) {
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
}

@Composable
private fun GPSChipSwitchRow(
    icon: Painter,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true
) {
    val containerColor = if (checked && enabled) {
        lerp(
            MaterialTheme.colorScheme.surfaceContainerLow,
            MaterialTheme.colorScheme.primaryContainer,
            0.35f
        )
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(TrailSenseShape.groupInner),
        color = containerColor
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GPSIconChip(icon)
        Spacer(Modifier.width(TrailSenseSpacing.lg))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(TrailSenseSpacing.md))
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled
        )
    }
    }
}

@Composable
private fun GPSChipActionRow(
    icon: Painter,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(TrailSenseShape.groupInner),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GPSIconChip(icon)
        Spacer(Modifier.width(TrailSenseSpacing.lg))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(TrailSenseSpacing.md))
        Box(
            modifier = Modifier.size(TrailSenseSize.minimumTouchTarget),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    }
}

@Composable
private fun CalibrateGPSSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(
                start = TrailSenseSpacing.md,
                bottom = TrailSenseSpacing.sm
            )
        )
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
private fun TrailSenseDivider() {
    Spacer(modifier = Modifier.height(0.dp))
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
