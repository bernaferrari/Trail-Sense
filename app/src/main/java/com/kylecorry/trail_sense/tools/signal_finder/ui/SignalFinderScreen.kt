@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.signal_finder.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp
import com.kylecorry.trail_sense.shared.icons.trail.NavigationArrow
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpotlight
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGroupShape
import com.kylecorry.trail_sense.shared.views.compose.trailSenseToneColors
import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.shared.icons.trail.SignalCellular1
import com.kylecorry.trail_sense.shared.icons.trail.SignalCellular2
import com.kylecorry.trail_sense.shared.icons.trail.SignalCellular3
import com.kylecorry.trail_sense.shared.icons.trail.SignalCellularOutline
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.icons.trail.CellTower
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInstrumentScaffold
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseLinkedText
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseReading
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
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
        hasLoaded = controller.hasLoaded,
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

    /** Set once the first tower search finishes, so "No cell towers found" never flashes before it. */
    var hasLoaded by mutableStateOf(false)
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
                TrailIcons.SignalCellularOutline,
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
            hasLoaded = true
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

/**
 * Signal finder: the strongest signal is the hero (its strength large, network and service under it); every signal
 * is a row in one group, nearby towers another group with their two actions, and the source note sits at the foot.
 * Loading shows as a small spinner in the top bar so the list never jumps.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SignalFinderScreen(
    signals: List<CellSignal>,
    towers: List<ApproximateCoordinate>,
    location: Coordinate,
    loading: Boolean,
    hasLoaded: Boolean,
    disclaimer: CharSequence,
    formatter: FormatService,
    prefs: UserPreferences,
    onNavigateToTower: (ApproximateCoordinate) -> Unit,
    onCreateBeacon: (ApproximateCoordinate) -> Unit,
    modifier: Modifier = Modifier
) {
    // Presentation-only: the strongest registered/available signal is surfaced as the hero.
    val bestSignal = signals.maxByOrNull { it.strength }
    val signalsTitle = stringResource(R.string.cell_signal)
    val towersTitle = stringResource(R.string.cell_towers)

    TrailSenseInstrumentScaffold(
        title = stringResource(R.string.tool_signal_finder),
        modifier = modifier.testTag("title"),
        actions = {
            if (loading) {
                Box(
                    modifier = Modifier.size(TrailSenseSize.minimumTouchTarget),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.LoadingIndicator(modifier = Modifier.size(28.dp))
                }
            }
        }
    ) {
        if (hasLoaded && signals.isEmpty() && towers.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    TrailSenseEmptyState(
                        icon = TrailIcons.CellTower,
                        title = stringResource(R.string.no_cell_towers_found),
                        description = stringResource(R.string.signal_finder_empty_hint)
                    )
                }
                SignalDisclaimer(disclaimer)
            }
            return@TrailSenseInstrumentScaffold
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = TrailSenseSize.maximumContentWidth)
                    .fillMaxSize()
                    .testTag("list"),
                contentPadding = PaddingValues(
                    start = TrailSenseSpacing.lg,
                    end = TrailSenseSpacing.lg,
                    top = TrailSenseSpacing.sm,
                    bottom = TrailSenseSpacing.xxxl
                ),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
            ) {
                if (bestSignal != null) {
                    item("hero") {
                        SignalHero(signal = bestSignal, formatter = formatter)
                    }
                }

                // A single signal is already the hero; the list only earns its place with two or more.
                if (signals.size > 1) {
                    trailSenseSection(key = "signals", title = signalsTitle) {
                        signals.forEach { signal ->
                            row { position ->
                                CellSignalRow(signal, formatter, prefs, position)
                            }
                        }
                    }
                }

                if (towers.isNotEmpty()) {
                    trailSenseSection(key = "towers", title = towersTitle) {
                        row { position ->
                            TowerRadar(towers = towers, location = location, position = position)
                        }
                        towers.forEachIndexed { index, tower ->
                            row { position ->
                                CellTowerRow(
                                    index = index,
                                    tower = tower,
                                    location = location,
                                    formatter = formatter,
                                    prefs = prefs,
                                    position = position,
                                    onNavigate = { onNavigateToTower(tower) },
                                    onCreateBeacon = { onCreateBeacon(tower) }
                                )
                            }
                        }
                    }
                }

                item("disclaimer") {
                    SignalDisclaimer(disclaimer)
                }
            }
        }
    }
}

/**
 * The hero: the strongest signal as a Spotlight toned by its quality (good, caution, error), its network and
 * service under it, and four rounded bars that fill with the strength.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SignalHero(
    signal: CellSignal,
    formatter: FormatService,
    modifier: Modifier = Modifier
) {
    val tone = strengthTone(signal.strength)
    val colors = trailSenseToneColors(tone)
    TrailSenseSpotlight(
        title = formatter.formatPercentage(signal.strength),
        titleStyle = MaterialTheme.typography.displayMedium,
        eyebrow = formatter.formatCellNetwork(signal.network),
        supportingText = if (signal.isRegistered) {
            stringResource(R.string.full_service)
        } else {
            stringResource(R.string.emergency_calls_only)
        },
        tone = tone,
        modifier = modifier,
        badge = {
            TrailSenseShapeBadge(
                icon = cellQualityIcon(signal.quality),
                shape = MaterialShapes.Cookie9Sided.toShape(),
                size = 64.dp,
                containerColor = colors.accent,
                contentColor = colors.onAccent
            )
        },
        trailing = {
            SignalBars(strength = signal.strength, color = colors.content, track = colors.content.copy(alpha = 0.22f))
        }
    )
}

@Composable
private fun SignalBars(strength: Float, color: Color, track: Color) {
    val level by animateFloatAsState((strength / 100f).coerceIn(0f, 1f), MaterialTheme.motionScheme.slowSpatialSpec(), label = "bars")
    Canvas(Modifier.size(width = 56.dp, height = 48.dp)) {
        val count = 4
        val gap = 4.dp.toPx()
        val w = (size.width - gap * (count - 1)) / count
        repeat(count) { i ->
            val h = size.height * (i + 1) / count
            val x = i * (w + gap)
            val filled = level >= (i + 0.5f) / count
            drawRoundRect(
                if (filled) color else track,
                Offset(x, size.height - h),
                Size(w, h),
                CornerRadius(w / 2f)
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CellSignalRow(
    signal: CellSignal,
    formatter: FormatService,
    prefs: UserPreferences,
    position: TrailSenseGroupPosition
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
    val colors = trailSenseToneColors(strengthTone(signal.strength))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = trailSenseGroupShape(position),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrailSenseShapeBadge(
                icon = cellQualityIcon(signal.quality),
                shape = MaterialShapes.Cookie6Sided.toShape(),
                size = 44.dp,
                containerColor = colors.container,
                contentColor = colors.content
            )
            Spacer(Modifier.width(TrailSenseSpacing.lg))
            Column(Modifier.weight(1f)) {
                Text(
                    formatter.formatCellNetwork(signal.network),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(TrailSenseSpacing.sm))
            Text(
                formatter.formatPercentage(signal.strength),
                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
                color = colors.accent
            )
        }
    }
}

/**
 * The nearby towers on a little radar, north up: you in the middle, each tower a numbered dot at its bearing,
 * pushed out by distance (20 km at the rim). The numbers match the rows under it.
 */
@Composable
private fun TowerRadar(
    towers: List<ApproximateCoordinate>,
    location: Coordinate,
    position: TrailSenseGroupPosition
) {
    val scheme = MaterialTheme.colorScheme
    val ring = scheme.primary.copy(alpha = 0.16f)
    val dot = scheme.primary
    val onDot = scheme.onPrimary
    val you = scheme.tertiary
    val labelSize = with(androidx.compose.ui.platform.LocalDensity.current) { 12.sp.toPx() }
    val north = scheme.onSurfaceVariant
    TrailSenseGroupCard(position = position) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            val radius = size.minDimension / 2f - 14.dp.toPx()
            listOf(1f, 0.66f, 0.33f).forEach {
                drawCircle(ring, radius * it, center, style = Stroke(width = if (it == 1f) 10.dp.toPx() else 1.5.dp.toPx()))
            }
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = labelSize
                isFakeBoldText = true
            }
            paint.color = north.toArgb()
            drawContext.canvas.nativeCanvas.drawText("N", center.x, center.y - radius - 4.dp.toPx() + labelSize / 3f, paint)
            drawCircle(you, 7.dp.toPx(), center)
            // Towers close together are nudged apart (outward, then sideways) so no two markers overlap.
            val placed = mutableListOf<Offset>()
            val markerGap = 28.dp.toPx()
            towers.forEachIndexed { index, tower ->
                val bearing = Math.toRadians(location.bearingTo(tower.coordinate).value.toDouble() - 90)
                var fraction = (location.distanceTo(tower.coordinate) / 20_000f).coerceIn(0.12f, 1f)
                var angle = bearing
                fun at() = Offset(
                    center.x + kotlin.math.cos(angle).toFloat() * radius * fraction,
                    center.y + kotlin.math.sin(angle).toFloat() * radius * fraction
                )
                var p = at()
                var tries = 0
                while (placed.any { (it - p).getDistance() < markerGap } && tries < 24) {
                    if (fraction < 1f) {
                        fraction = (fraction + 0.08f).coerceAtMost(1f)
                    } else {
                        angle += 0.18
                    }
                    p = at()
                    tries++
                }
                placed += p
                drawCircle(dot, 12.dp.toPx(), p)
                paint.color = onDot.toArgb()
                drawContext.canvas.nativeCanvas.drawText((index + 1).toString(), p.x, p.y + labelSize / 3f, paint)
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CellTowerRow(
    index: Int,
    tower: ApproximateCoordinate,
    location: Coordinate,
    formatter: FormatService,
    prefs: UserPreferences,
    position: TrailSenseGroupPosition,
    onNavigate: () -> Unit,
    onCreateBeacon: () -> Unit
) {
    val distance = Distance.meters(location.distanceTo(tower.coordinate))
        .convertTo(prefs.baseDistanceUnits)
        .toRelativeDistance()
    val direction = location.bearingTo(tower.coordinate)
    val scheme = MaterialTheme.colorScheme

    TrailSenseGroupCard(
        position = position,
        contentPadding = PaddingValues(TrailSenseSpacing.lg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TrailSenseShapeBadge(
                text = (index + 1).toString(),
                shape = MaterialShapes.Cookie9Sided.toShape(),
                size = 44.dp
            )
            Spacer(Modifier.width(TrailSenseSpacing.lg))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)) {
                Text(
                    text = stringResource(R.string.cell_tower),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)) {
                    TowerChip(formatter.formatDistance(distance, Units.getDecimalPlaces(distance.units)), primary = true)
                    TowerChip(
                        "${formatter.formatDegrees(direction.value, replace360 = true)} ${
                            formatter.formatDirection(direction.direction)
                        }",
                        primary = false
                    )
                }
            }
        }
        Spacer(Modifier.height(TrailSenseSpacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCreateBeacon) {
                Text(stringResource(R.string.create_beacon))
            }
            FilledTonalButton(onClick = onNavigate) {
                Icon(TrailIcons.NavigationArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(TrailSenseSpacing.sm))
                Text(stringResource(R.string.navigate))
            }
        }
    }
}

@Composable
private fun TowerChip(text: String, primary: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = if (primary) scheme.primaryContainer else scheme.secondaryContainer,
        contentColor = if (primary) scheme.onPrimaryContainer else scheme.onSecondaryContainer
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = TrailSenseSpacing.md, vertical = TrailSenseSpacing.xs)
        )
    }
}

/** Colour follows how usable the signal is: green from half strength up, amber below, red only when it is weak. */
private fun strengthTone(strength: Float): TrailSenseTone = when {
    strength >= 50f -> TrailSenseTone.Good
    strength >= 25f -> TrailSenseTone.Caution
    else -> TrailSenseTone.Error
}

private fun qualityTone(quality: com.kylecorry.andromeda.core.sensors.Quality?): TrailSenseTone = when (quality) {
    com.kylecorry.andromeda.core.sensors.Quality.Poor -> TrailSenseTone.Error
    com.kylecorry.andromeda.core.sensors.Quality.Moderate -> TrailSenseTone.Caution
    com.kylecorry.andromeda.core.sensors.Quality.Good -> TrailSenseTone.Good
    else -> TrailSenseTone.Secondary
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
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center
    )
}

private fun cellQualityIcon(quality: com.kylecorry.andromeda.core.sensors.Quality?): ImageVector = when (quality) {
    com.kylecorry.andromeda.core.sensors.Quality.Poor -> TrailIcons.SignalCellular1
    com.kylecorry.andromeda.core.sensors.Quality.Moderate -> TrailIcons.SignalCellular2
    com.kylecorry.andromeda.core.sensors.Quality.Good -> TrailIcons.SignalCellular3
    else -> TrailIcons.SignalCellularOutline
}
