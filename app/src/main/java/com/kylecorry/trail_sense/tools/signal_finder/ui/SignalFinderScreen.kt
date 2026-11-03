@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.signal_finder.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.trail_sense.shared.ErrorBannerReason
import com.kylecorry.trail_sense.shared.views.UserError
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.signal.CellSignal
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.ApproximateCoordinate
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.shared.sensors.CellSignalUtils
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseLinkedText
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.signal_finder.infrastructure.CellTowerModel
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SignalFinderToolScreen(host: TrailSenseDestinationHost) {
    val controller = remember(host) {
        SignalFinderController(host)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(controller, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> controller.onResume()
                Lifecycle.Event.ON_PAUSE -> controller.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            controller.onResume()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.onDestroy()
        }
    }

    SignalFinderScreen(
        signals = controller.signals,
        towers = controller.nearbyTowers,
        location = controller.location,
        loading = controller.loading,
        disclaimer = controller.disclaimer,
        formatter = controller.formatter,
        prefs = controller.prefs,
        onNavigateToTower = controller::navigateToTower,
        onCreateBeacon = controller::createBeacon
    )
}

private class SignalFinderController(private val host: TrailSenseDestinationHost) {

    private val sensorService by lazy { SensorService(host.requireContext()) }
    private val cellSignal by lazy { sensorService.getCellSignal() }
    private val gps by lazy { sensorService.getGPS() }
    val formatter by lazy { FormatService.getInstance(host.requireContext()) }
    val prefs by lazy { UserPreferences(host.requireContext()) }
    private val markdown by lazy { DependencyRegistry.get<MarkdownService>() }
    private val navigator by lazy { Navigator.getInstance(host.requireContext()) }
    private val handler = Handler(Looper.getMainLooper())

    var signals by mutableStateOf(emptyList<CellSignal>())
        private set
    var nearbyTowers by mutableStateOf(emptyList<ApproximateCoordinate>())
        private set
    var location by mutableStateOf(Coordinate.zero)
        private set
    var loading by mutableStateOf(false)
        private set
    val disclaimer by lazy {
        markdown.toMarkdown(host.getString(R.string.cell_tower_disclaimer))
    }
    private var lastTowerLocation: Coordinate? = null
    private var towerJob: Job? = null
    private var locationPermission: Boolean? = null

    private val sensorListener = {
        refreshReadings()
        true
    }
    private val refreshRunnable = object : Runnable {
        override fun run() {
            refreshReadings()
            handler.postDelayed(this, 1000)
        }
    }

    fun onResume() {
        val hasPermission = sensorService.hasLocationPermission()
        if (locationPermission != hasPermission) {
            locationPermission = hasPermission
            updateLocationPermissionError(hasPermission)
        }
        cellSignal.start(sensorListener)
        gps.start(sensorListener)
        refreshReadings()
        handler.removeCallbacks(refreshRunnable)
        handler.postDelayed(refreshRunnable, 1000)
    }

    fun onPause() {
        handler.removeCallbacks(refreshRunnable)
        cellSignal.stop(sensorListener)
        gps.stop(sensorListener)
        towerJob?.cancel()
    }

    fun onDestroy() {
        handler.removeCallbacks(refreshRunnable)
        cellSignal.stop(sensorListener)
        gps.stop(sensorListener)
        towerJob?.cancel()
        host.mainActivity.errorBanner.dismiss(ErrorBannerReason.LocationPermissionDenied)
    }

    private fun updateLocationPermissionError(hasPermission: Boolean) {
        val banner = host.mainActivity.errorBanner
        if (hasPermission) {
            banner.dismiss(ErrorBannerReason.LocationPermissionDenied)
            return
        }

        banner.report(
            UserError(
                ErrorBannerReason.LocationPermissionDenied,
                host.getString(R.string.location_required_for_cell_signals),
                R.drawable.signal_cellular_outline,
                host.getString(R.string.settings)
            ) {
                banner.dismiss(ErrorBannerReason.LocationPermissionDenied)
                host.startActivity(Intents.appSettings(host.requireContext()))
            }
        )
    }

    private fun refreshReadings() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            handler.post { refreshReadings() }
            return
        }

        signals = cellSignal.signals
        val currentLocation = gps.location
        location = currentLocation

        val lastLocation = lastTowerLocation
        if (lastLocation == null || lastLocation.distanceTo(currentLocation) >= 100f) {
            loadNearbyTowers(currentLocation)
        }
    }

    private fun loadNearbyTowers(currentLocation: Coordinate) {
        lastTowerLocation = currentLocation
        towerJob?.cancel()
        towerJob = host.lifecycleScope.launch {
            loading = true
            val towers = withContext(Dispatchers.IO) {
                CellTowerModel.getTowers(
                    CoordinateBounds.from(Geofence(currentLocation, Distance.kilometers(20f))),
                    5
                ).sortedBy { currentLocation.distanceTo(it.coordinate) }
            }
            nearbyTowers = towers
            loading = false
        }
    }

    fun navigateToTower(tower: ApproximateCoordinate) {
        navigator.navigateTo(tower.coordinate, host.getString(R.string.cell_tower))
        host.findNavController().openTool(Tools.NAVIGATION)
    }

    fun createBeacon(tower: ApproximateCoordinate) {
        val bundle = Bundle().apply {
            putParcelable("initial_location", GeoUri(tower.coordinate))
        }
        host.findNavController().navigate(R.id.placeBeaconFragment, bundle)
    }
}

@Composable
private fun SignalFinderScreen(
    signals: List<CellSignal>,
    towers: List<ApproximateCoordinate>,
    location: Coordinate,
    loading: Boolean,
    disclaimer: CharSequence,
    formatter: FormatService,
    prefs: UserPreferences,
    onNavigateToTower: (ApproximateCoordinate) -> Unit,
    onCreateBeacon: (ApproximateCoordinate) -> Unit,
    modifier: Modifier = Modifier
) {
    // Presentation-only: the strongest registered/available signal is surfaced as
    // the hero. Derived purely from the existing list, no state or behavior change.
    val bestSignal = signals.maxByOrNull { it.strength }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = TrailSenseSize.maximumContentWidth)
                    .fillMaxSize()
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("title")
                    .padding(
                        start = TrailSenseSpacing.screen,
                        end = TrailSenseSpacing.screen,
                        top = TrailSenseSpacing.lg,
                        bottom = TrailSenseSpacing.sm
                    )
            ) {
                Text(
                    text = stringResource(R.string.cell_towers),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (loading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = TrailSenseSpacing.xs)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(TrailSenseSpacing.sm))
                        Text(
                            text = stringResource(R.string.loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (signals.isEmpty() && towers.isEmpty()) {
                    TrailSenseEmptyState(
                        icon = Icons.Default.Sensors,
                        title = stringResource(R.string.no_cell_towers_found)
                    )
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("list"),
                    contentPadding = PaddingValues(
                        start = TrailSenseSpacing.screen,
                        end = TrailSenseSpacing.screen,
                        top = TrailSenseSpacing.sm,
                        bottom = TrailSenseSpacing.lg
                    ),
                    verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
                ) {
                    if (bestSignal != null) {
                        item {
                            SignalHeroCard(
                                signal = bestSignal,
                                formatter = formatter
                            )
                        }
                    }

                    if (signals.isNotEmpty()) {
                        item {
                            SectionLabel(stringResource(R.string.cell_towers))
                        }
                        item {
                            TrailSenseCard {
                                signals.forEachIndexed { index, signal ->
                                    if (index > 0) {
                                        Spacer(Modifier.height(TrailSenseSpacing.xs))
                                    }
                                    CellSignalRow(signal, formatter, prefs)
                                }
                            }
                        }
                    }

                    if (towers.isNotEmpty()) {
                        item {
                            SectionLabel(stringResource(R.string.cell_tower))
                        }
                        items(towers) { tower ->
                            TrailSenseCard {
                                CellTowerRow(
                                    tower = tower,
                                    location = location,
                                    formatter = formatter,
                                    prefs = prefs,
                                    onNavigate = { onNavigateToTower(tower) },
                                    onCreateBeacon = { onCreateBeacon(tower) }
                                )
                            }
                        }
                    }
                }
            }

            SignalDisclaimer(disclaimer)
        }
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(
            start = TrailSenseSpacing.xs,
            top = TrailSenseSpacing.sm,
            bottom = TrailSenseSpacing.xxs
        )
    )
}

@Composable
private fun SignalHeroCard(
    signal: CellSignal,
    formatter: FormatService,
    modifier: Modifier = Modifier
) {
    // Strength accent animates between quality colors (tertiary -> error) as the
    // best signal changes. Color comes from the shared quality palette.
    val strengthColor by animateColorAsState(
        targetValue = Color(CustomUiUtils.getQualityColor(signal.quality)),
        animationSpec = tween(durationMillis = 250),
        label = "heroStrength"
    )

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
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(
                            CellSignalUtils.getCellQualityImage(signal.quality)
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = strengthColor
                    )
                }
            }

            Spacer(Modifier.width(TrailSenseSpacing.lg))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatter.formatCellNetwork(signal.network),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(TrailSenseSpacing.xxs))
                Text(
                    text = formatter.formatPercentage(signal.strength),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFeatureSettings = "tnum"
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = if (signal.isRegistered) {
                        stringResource(R.string.full_service)
                    } else {
                        stringResource(R.string.emergency_calls_only)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = strengthColor
                )
            }
        }
    }
}

@Composable
private fun CellSignalRow(
    signal: CellSignal,
    formatter: FormatService,
    prefs: UserPreferences,
    modifier: Modifier = Modifier
) {
    val distance = signal.timingDistanceMeters?.let {
        Distance.meters(it).convertTo(prefs.baseDistanceUnits).toRelativeDistance()
    }
    val maxDistance = distance?.let {
        signal.timingDistanceErrorMeters?.let { error ->
            val errorDistance = Distance.meters(error).convertTo(distance.units)
            Distance.from(distance.value + errorDistance.value, distance.units).toRelativeDistance()
        }
    }
    val description = formatter.join(
        formatter.formatPercentage(signal.strength),
        distance?.let {
            formatter.formatDistance(distance, Units.getDecimalPlaces(distance.units)) +
                    if (maxDistance != null) {
                        " - ${
                            formatter.formatDistance(
                                maxDistance,
                                Units.getDecimalPlaces(maxDistance.units)
                            )
                        }"
                    } else {
                        ""
                    }
        },
        formatter.formatTime(signal.time),
        if (signal.isRegistered) {
            stringResource(R.string.full_service)
        } else {
            stringResource(R.string.emergency_calls_only)
        },
        separator = FormatService.Separator.Dot
    )

    SignalFinderRow(
        title = formatter.formatCellNetwork(signal.network),
        description = description,
        icon = CellSignalUtils.getCellQualityImage(signal.quality),
        tint = Color(CustomUiUtils.getQualityColor(signal.quality)),
        modifier = modifier
    )
}

@Composable
private fun CellTowerRow(
    tower: ApproximateCoordinate,
    location: Coordinate,
    formatter: FormatService,
    prefs: UserPreferences,
    onNavigate: () -> Unit,
    onCreateBeacon: () -> Unit,
    modifier: Modifier = Modifier
) {
    val distance = Distance.meters(location.distanceTo(tower.coordinate))
        .convertTo(prefs.baseDistanceUnits)
        .toRelativeDistance()
    val direction = location.bearingTo(tower.coordinate)
    val description = formatter.join(
        formatter.formatDistance(distance, Units.getDecimalPlaces(distance.units)),
        "${formatter.formatDegrees(direction.value, replace360 = true)} ${
            formatter.formatDirection(direction.direction)
        }",
        separator = FormatService.Separator.Dot
    )

    SignalFinderRow(
        title = stringResource(R.string.cell_tower),
        description = description,
        icon = R.drawable.cell_tower,
        tint = MaterialTheme.colorScheme.onPrimaryContainer,
        trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)) {
                TextButton(onClick = onNavigate) {
                    Text(stringResource(R.string.navigate))
                }
                TextButton(onClick = onCreateBeacon) {
                    Text(stringResource(R.string.create_beacon))
                }
            }
        },
        modifier = modifier
    )
}

@Composable
private fun SignalFinderRow(
    title: String,
    description: String,
    icon: Int,
    tint: Color,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = tint
                    )
                }
            }
            Spacer(Modifier.width(TrailSenseSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TrailSenseSpacing.sm),
                horizontalArrangement = Arrangement.End
            ) {
                trailing()
            }
        }
    }
}

@Composable
private fun SignalDisclaimer(
    disclaimer: CharSequence,
    modifier: Modifier = Modifier
) {
    TrailSenseLinkedText(
        text = disclaimer,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = TrailSenseSpacing.lg,
                end = TrailSenseSpacing.lg,
                bottom = TrailSenseSpacing.lg
            )
            .testTag("disclaimer"),
        style = MaterialTheme.typography.labelSmall,
        textAlign = TextAlign.Center
    )
}
