@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.graphics.shapes.toPath
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
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.material.icons.rounded.AddLocationAlt
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
        host.findNavigator().openTool(Tools.NAVIGATION)
    }

    fun createBeacon(tower: ApproximateCoordinate) {
        val bundle = Bundle().apply {
            putParcelable("initial_location", GeoUri(tower.coordinate))
        }
        host.findNavigator().navigate(R.id.placeBeaconFragment, bundle)
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
    // One question: how good is my signal here, and which way is the nearest tower?
    // Presentation-only: the strongest registered/available signal is the reading.
    val bestSignal = signals.maxByOrNull { it.strength }
    val otherSignalsTitle = stringResource(R.string.signal_finder_other_signals)
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
                // Nearest first, so the numbers read as a ranking and the first row is the one to head for.
                val sortedTowers = towers.sortedBy { location.distanceTo(it.coordinate) }

                // The live object: the towers around you on a radar (or the signal bars when there are none), with the
                // reading right under it.
                item("instrument") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
                    ) {
                        if (sortedTowers.isNotEmpty()) {
                            TowerRadar(
                                strength = bestSignal?.strength,
                                towers = sortedTowers,
                                location = location,
                                formatter = formatter,
                                prefs = prefs
                            )
                        } else if (bestSignal != null) {
                            SignalBars(
                                strength = bestSignal.strength,
                                color = MaterialTheme.colorScheme.primary,
                                track = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier
                                    .padding(vertical = TrailSenseSpacing.xl)
                                    .size(width = 140.dp, height = 120.dp)
                            )
                        }
                        if (bestSignal != null) {
                            SignalReading(
                                signal = bestSignal,
                                nearestTower = sortedTowers.firstOrNull(),
                                location = location,
                                formatter = formatter,
                                prefs = prefs
                            )
                        }
                    }
                }

                // The strongest signal is already the reading above; the list holds only the others, so no network
                // and percentage is shown twice.
                val otherSignals = signals.filter { it !== bestSignal }
                if (otherSignals.isNotEmpty()) {
                    trailSenseSection(key = "signals", title = otherSignalsTitle) {
                        otherSignals.forEach { signal ->
                            row { position ->
                                CellSignalRow(signal, formatter, prefs, position)
                            }
                        }
                    }
                }

                if (sortedTowers.isNotEmpty()) {
                    trailSenseSection(key = "towers", title = towersTitle) {
                        sortedTowers.forEachIndexed { index, tower ->
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
 * The answer: the strongest signal's strength as the one display value, a plain verdict on it (or "Emergency calls
 * only" in error when the phone has no service), and one quiet line with its network and the way to the nearest tower.
 */
@Composable
private fun SignalReading(
    signal: CellSignal,
    nearestTower: ApproximateCoordinate?,
    location: Coordinate,
    formatter: FormatService,
    prefs: UserPreferences,
    modifier: Modifier = Modifier
) {
    val verdict = stringResource(
        when {
            !signal.isRegistered -> R.string.emergency_calls_only
            signal.strength >= 75f -> R.string.signal_verdict_strong
            signal.strength >= 50f -> R.string.signal_verdict_good
            signal.strength >= 25f -> R.string.signal_verdict_fair
            else -> R.string.signal_verdict_weak
        }
    )
    val towerLine = nearestTower?.let { tower ->
        val distance = Distance.meters(location.distanceTo(tower.coordinate))
            .convertTo(prefs.baseDistanceUnits)
            .toRelativeDistance()
        stringResource(
            R.string.signal_finder_nearest_tower,
            formatter.formatDistance(distance, Units.getDecimalPlaces(distance.units)),
            formatter.formatDirection(location.bearingTo(tower.coordinate).direction)
        )
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("signal_reading"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
    ) {
        Text(
            text = formatter.formatPercentage(signal.strength),
            style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Text(
            text = verdict,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (signal.isRegistered) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Text(
            text = formatter.join(
                formatter.formatCellNetwork(signal.network),
                towerLine,
                separator = FormatService.Separator.Dot
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SignalBars(strength: Float, color: Color, track: Color, modifier: Modifier = Modifier) {
    val level by animateFloatAsState((strength / 100f).coerceIn(0f, 1f), MaterialTheme.motionScheme.slowSpatialSpec(), label = "bars")
    Canvas(modifier) {
        val count = 4
        val gap = 8.dp.toPx()
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
                        " – ${
                            formatter.formatDistance(
                                maxDistance,
                                Units.getDecimalPlaces(maxDistance.units)
                            )
                        }"
                    } else {
                        ""
                    }
        },
        if (signal.isRegistered) {
            stringResource(R.string.full_service)
        } else {
            stringResource(R.string.emergency_calls_only)
        },
        separator = FormatService.Separator.Dot
    )
    TrailSenseListItem(
        title = formatter.formatCellNetwork(signal.network),
        subtitle = description,
        icon = rememberVectorPainter(cellQualityIcon(signal.quality)),
        position = position,
        trailing = {
            Text(
                formatter.formatPercentage(signal.strength),
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold
            )
        }
    )
}

/**
 * The nearby towers on a radar, north up: you in the middle, distance rings with their ranges named, and each tower
 * as a numbered cookie at its bearing, pushed out by distance (20 km at the rim). The nearest tower is the one accent:
 * larger, in tertiary, with a line from you to it. A ring pulses out from you, faster the stronger the signal.
 */
@Composable
private fun TowerRadar(
    strength: Float?,
    towers: List<ApproximateCoordinate>,
    location: Coordinate,
    formatter: FormatService,
    prefs: UserPreferences
) {
    val scheme = MaterialTheme.colorScheme
    val disc = scheme.surfaceContainerLow
    val ringLine = scheme.outlineVariant
    val dot = scheme.primaryContainer
    val onDot = scheme.onPrimaryContainer
    // The towers' shape, the same Cookie6 as the list's badges, normalised to a unit square.
    val cookie = remember { MaterialShapes.Cookie6Sided.toPath() }
    val nearest = scheme.tertiary
    val onNearest = scheme.onTertiary
    val you = scheme.onSurface
    val labelSize = with(androidx.compose.ui.platform.LocalDensity.current) { 12.sp.toPx() }
    val ink = scheme.onSurface
    val rangeLabels = listOf(20_000f, 13_200f, 6_600f).map {
        formatter.formatDistance(
            Distance.meters(it).convertTo(prefs.baseDistanceUnits).toRelativeDistance(),
            0
        )
    }
    // A ring leaves you and spreads to the rim, faster the stronger the signal: the radar shows the reading too.
    val period = strength?.let { (3200 - 20 * it.coerceIn(0f, 100f)).toInt() } ?: 0
    val pulse = if (period > 0) {
        val p by androidx.compose.animation.core.rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(period, easing = LinearEasing), RepeatMode.Restart),
            label = "pulse"
        )
        p
    } else {
        -1f
    }
    val radarDescription = stringResource(R.string.cell_towers)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(300.dp)
            .testTag("signal_radar")
            .semantics { contentDescription = radarDescription }
    ) {
            val radius = size.minDimension / 2f - 10.dp.toPx()
            drawCircle(disc, radius, center)
            listOf(1f, 0.66f, 0.33f).forEach {
                drawCircle(ringLine, radius * it, center, style = Stroke(width = 1.5.dp.toPx()))
            }
            if (pulse >= 0f) {
                drawCircle(
                    scheme.primary.copy(alpha = 0.35f * (1f - pulse)),
                    radius * pulse,
                    center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = labelSize
                isFakeBoldText = true
            }
            paint.color = ink.toArgb()
            drawContext.canvas.nativeCanvas.drawText("N", center.x, center.y - radius + labelSize + 4.dp.toPx(), paint)
            // Range labels on each ring along the one diagonal furthest from every tower, each with a halo in the
            // disc's colour, so no tower ever sits on a label.
            val bearings = towers.map { location.bearingTo(it.coordinate).value }
            val labelBearing = listOf(135f, 45f, 225f, 315f).maxBy { candidate ->
                bearings.minOfOrNull { b ->
                    val d = kotlin.math.abs(((b - candidate) % 360f + 540f) % 360f - 180f)
                    d
                } ?: 180f
            }
            val labelAngle = Math.toRadians(labelBearing.toDouble() - 90)
            val halo = android.graphics.Paint(paint).apply {
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 4.dp.toPx()
                strokeJoin = android.graphics.Paint.Join.ROUND
                color = disc.toArgb()
                isFakeBoldText = false
            }
            paint.isFakeBoldText = false
            paint.color = ink.copy(alpha = 0.7f).toArgb()
            listOf(1f, 0.66f, 0.33f).forEachIndexed { i, f ->
                // The outer ring's label sits just inside the rim.
                val r = radius * f - if (f == 1f) labelSize else 0f
                val x = center.x + kotlin.math.cos(labelAngle).toFloat() * r
                val y = center.y + kotlin.math.sin(labelAngle).toFloat() * r + labelSize / 3f
                drawContext.canvas.nativeCanvas.drawText(rangeLabels[i], x, y, halo)
                drawContext.canvas.nativeCanvas.drawText(rangeLabels[i], x, y, paint)
            }
            paint.isFakeBoldText = true
            // Towers close together are nudged apart (outward, then sideways) so no two markers overlap.
            // You count as a marker too, so no tower sits on your dot.
            val placed = mutableListOf(center)
            val markerGap = 32.dp.toPx()
            towers.forEach { tower ->
                val bearingDegrees = location.bearingTo(tower.coordinate).value
                val bearing = Math.toRadians(bearingDegrees.toDouble() - 90)
                var fraction = (location.distanceTo(tower.coordinate) / 20_000f).coerceIn(0.14f, 1f)
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
            }
            val markers = placed.drop(1)

            // The answer to "which way?": a line from you to the nearest tower.
            markers.firstOrNull()?.let { target ->
                drawLine(
                    nearest.copy(alpha = 0.55f),
                    center,
                    target,
                    strokeWidth = 3.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }

            // Every tower is a numbered cookie (the same shape as its row's badge); the nearest is larger and in the
            // one accent, the rest are quiet tonal ones.
            markers.forEachIndexed { index, p ->
                val isNearest = index == 0
                val markerSize = (if (isNearest) 36.dp else 28.dp).toPx()
                val marker = android.graphics.Path(cookie).apply {
                    transform(
                        android.graphics.Matrix().apply {
                            setScale(markerSize, markerSize)
                            postTranslate(p.x - markerSize / 2f, p.y - markerSize / 2f)
                        }
                    )
                }
                if (isNearest) {
                    drawCircle(nearest.copy(alpha = 0.18f), markerSize * 0.85f, p)
                }
                drawPath(marker.asComposePath(), if (isNearest) nearest else dot)
                paint.color = (if (isNearest) onNearest else onDot).toArgb()
                drawContext.canvas.nativeCanvas.drawText((index + 1).toString(), p.x, p.y + labelSize / 3f, paint)
            }

            // You, on top of the line: a dot ringed in the disc's colour.
            drawCircle(disc, 10.dp.toPx(), center)
            drawCircle(you, 7.dp.toPx(), center)
            drawCircle(scheme.surfaceContainerLowest, 3.dp.toPx(), center)
    }
}

/**
 * A tower as one compact 72 dp row: its number in a scallop (the nearest in the radar's accent), the distance and
 * the bearing. The row navigates; its one button saves the tower as a beacon.
 */
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
    val isNearest = index == 0
    val container = scheme.surfaceContainer
    val content = scheme.onSurface

    // Tapping the row navigates to the tower; the one trailing button saves it as a beacon. No second arrow beside
    // the bearing: the radar already shows the way.
    Surface(
        onClick = onNavigate,
        modifier = Modifier.fillMaxWidth(),
        shape = trailSenseGroupShape(position),
        color = container,
        contentColor = content
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 72.dp)
                .padding(start = TrailSenseSpacing.lg, end = TrailSenseSpacing.sm, top = TrailSenseSpacing.sm, bottom = TrailSenseSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrailSenseShapeBadge(
                text = (index + 1).toString(),
                shape = MaterialShapes.Cookie6Sided.toShape(),
                size = 40.dp,
                // The same colours as its dot on the radar, so the number and the dot read as one tower.
                containerColor = if (isNearest) scheme.tertiary else scheme.primaryContainer,
                contentColor = if (isNearest) scheme.onTertiary else scheme.onPrimaryContainer
            )
            Spacer(Modifier.width(TrailSenseSpacing.lg))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = formatter.formatDistance(distance, Units.getDecimalPlaces(distance.units)),
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = if (isNearest) {
                        stringResource(
                            R.string.signal_finder_nearest_bearing,
                            "${formatter.formatDegrees(direction.value, replace360 = true)} ${formatter.formatDirection(direction.direction)}"
                        )
                    } else {
                        "${formatter.formatDegrees(direction.value, replace360 = true)} ${formatter.formatDirection(direction.direction)}"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                    color = content.copy(alpha = 0.78f),
                    maxLines = 1
                )
            }
            androidx.compose.material3.IconButton(onClick = onCreateBeacon) {
                Icon(
                    androidx.compose.material.icons.Icons.Rounded.AddLocationAlt,
                    contentDescription = stringResource(R.string.create_beacon),
                    tint = scheme.onSurfaceVariant
                )
            }
        }
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
