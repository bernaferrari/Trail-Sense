@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.paths.ui

import com.kylecorry.trail_sense.tools.navigation.infrastructure.INavigator
import com.kylecorry.trail_sense.shared.text.appPluralString
import org.jetbrains.compose.resources.StringResource
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.shared.text.AppStrings

import kotlinx.coroutines.IO

import com.kylecorry.trail_sense.shared.views.compose.chartHeight

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.andromeda.sense.compass.CompassSensor
import com.kylecorry.andromeda.sense.location.GpsSensor
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.sol.math.MathExtensions.roundPlaces
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.statistics.Statistics
import com.kylecorry.sol.science.geography.Geography
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.debugging.isDebug
import com.kylecorry.trail_sense.shared.declination.IDeclinationStrategy
import com.kylecorry.trail_sense.shared.events.EventData
import com.kylecorry.trail_sense.shared.events.ToolBroadcasts
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.extensions.range
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.icons.trail.BaselineStop24
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.start
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.stop
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseStat
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseStatGrid
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesKeyValue
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathFollow
import com.kylecorry.trail_sense.tools.paths.PathsBroadcasts
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingDifficulty
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A path's overview state: the path, its points (newest first, elevations corrected), the hiking and elevation
 * statistics, the selected point and where you are. Plain Kotlin over the path service, the navigator and the
 * sensors; the map and the chart are drawn by the screen from here.
 */
class PathOverviewViewModel(
    val pathId: Long,
    val pathService: IPathService,
    private val navigator: INavigator,
    private val gps: GpsSensor,
    private val compass: CompassSensor,
    private val declinationProvider: IDeclinationStrategy,
    private val baseDistanceUnits: DistanceUnits,
    private val isDebug: Boolean,
    private val debugElevations: suspend (List<PathPoint>, List<PathPoint>) -> Unit,
    private val isBacktrackOn: () -> Boolean,
    private val stopBacktrack: suspend () -> Unit
) : ViewModel() {

    private val hikingService = HikingService
    private val paceFactor = 1.75f

    var path by mutableStateOf<Path?>(null)
        private set

    /** The path's group name; "" until loaded, null for no group. */
    var parentName by mutableStateOf<String?>("")
        private set

    /** The points, newest first, with their elevations corrected and slopes filled in. */
    var waypoints by mutableStateOf<List<PathPoint>>(emptyList())
        private set

    /** The points in walking order, for the elevation chart. */
    var chartPoints by mutableStateOf<List<PathPoint>>(emptyList())
        private set

    var calculatedDuration by mutableStateOf<Duration>(Duration.ZERO)
        private set
    var elevationGain by mutableStateOf(Distance.meters(0f))
        private set
    var elevationLoss by mutableStateOf(Distance.meters(0f))
        private set
    var elevationRange by mutableStateOf<Range<Distance>?>(null)
        private set
    var difficulty by mutableStateOf(HikingDifficulty.Easy)
        private set
    var selectedPoint by mutableStateOf<PathPoint?>(null)
        private set
    val selectedPointId: Long?
        get() = selectedPoint?.id
    var isFullscreen by mutableStateOf(false)
    var debugTiming by mutableStateOf<String?>(null)
        private set

    var userLocation by mutableStateOf(gps.location)
        private set
    var userLocationAccuracy by mutableStateOf(gps.horizontalAccuracy?.let { Distance.meters(it) })
        private set
    var userAzimuth by mutableStateOf<Bearing?>(null)
        private set

    /** Whether Backtrack is recording into this path right now. */
    var isRecording by mutableStateOf(false)
        private set

    private val onBacktrackChanged: (EventData) -> Unit = { refreshRecording() }

    private val _navigationStarted = Channel<Unit>(Channel.BUFFERED)

    /** One event each time navigation along the path starts, so the screen can open the compass. */
    val navigationStarted: Flow<Unit> = _navigationStarted.receiveAsFlow()

    private val onGpsReading: () -> Boolean = {
        updateDeclination()
        userLocation = gps.location
        userLocationAccuracy = gps.horizontalAccuracy?.let { Distance.meters(it) }
        true
    }

    private val onCompassReading: () -> Boolean = {
        userAzimuth = compass.bearing
        true
    }

    init {
        ToolBroadcasts.subscribe(PathsBroadcasts.BACKTRACK_STATE_CHANGED, onBacktrackChanged)
        refreshRecording()
        viewModelScope.launch {
            pathService.getLivePath(pathId).collect {
                path = it
                updateParent(it)
            }
        }
        viewModelScope.launch {
            pathService.getWaypointsLive(pathId).collectLatest {
                onWaypointsChanged(it)
            }
        }
    }

    fun startSensors() {
        // The last known location until the GPS reports
        userLocation = gps.location
        userLocationAccuracy = gps.horizontalAccuracy?.let { Distance.meters(it) }
        gps.start(onGpsReading)
        compass.start(onCompassReading)
    }

    fun stopSensors() {
        gps.stop(onGpsReading)
        compass.stop(onCompassReading)
    }

    override fun onCleared() {
        stopSensors()
        ToolBroadcasts.unsubscribe(PathsBroadcasts.BACKTRACK_STATE_CHANGED, onBacktrackChanged)
    }

    fun refreshRecording() {
        viewModelScope.launch {
            isRecording = onIO { pathService.getBacktrackPathId() == pathId && isBacktrackOn() }
        }
    }

    /** Turns Backtrack off, which ends the recording of this path. */
    fun stopRecording() {
        viewModelScope.launch {
            onIO { stopBacktrack() }
            refreshRecording()
        }
    }

    /** Selects [point], or clears the selection if it is the selected one. */
    fun selectPoint(point: PathPoint) {
        selectedPoint = if (selectedPoint?.id == point.id) null else point
    }

    fun deselectPoint() {
        selectedPoint = null
    }

    fun followPath(follow: PathFollow) {
        val selectedPath = path ?: return
        viewModelScope.launch {
            navigator.navigateAlongPath(selectedPath, follow)
            _navigationStarted.send(Unit)
        }
    }

    private fun updateParent(path: Path?) {
        path ?: return
        viewModelScope.launch {
            parentName = withContext(Dispatchers.IO) { pathService.getGroup(path.parentId) }?.name
        }
    }

    private fun updateDeclination() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                compass.declination = declinationProvider.getDeclination()
            }
        }
    }

    private suspend fun onWaypointsChanged(raw: List<PathPoint>) {
        val corrected = withContext(Dispatchers.Default) {
            hikingService.correctElevations(raw.sortedBy { it.id }).reversed()
        }
        withContext(Dispatchers.IO) {
            debugElevations(raw, corrected)
        }

        val selected = selectedPointId
        if (selected != null && raw.find { it.id == selected } == null) {
            deselectPoint()
        }

        withContext(Dispatchers.Default) {
            val inOrder = corrected.reversed()
            val gainLoss = hikingService.getElevationLossGain(inOrder)
            elevationGain = gainLoss.second.convertTo(baseDistanceUnits)
            elevationLoss = gainLoss.first.convertTo(baseDistanceUnits)
            elevationRange = inOrder.mapNotNull { it.elevation?.let { Distance.meters(it).convertTo(baseDistanceUnits) } }
                .range()
            val slopes = hikingService.getSlopes(inOrder)
            slopes.forEach {
                it.first.slope = it.third
            }
            val first = slopes.lastOrNull()
            first?.first?.slope = first.third

            difficulty = hikingService.getHikingDifficulty(inOrder)
            calculatedDuration = hikingService.getHikingDuration(inOrder, paceFactor)
        }

        waypoints = corrected
        chartPoints = corrected.reversed()
        updateDebugStats(corrected)
    }

    private suspend fun updateDebugStats(points: List<PathPoint>) {
        if (!isDebug) {
            return
        }

        val text = withContext(Dispatchers.Default) {
            val readings = points
                .mapNotNull { it.time }
                .sorted()
                .zipWithNext { a, b -> (b - a).inWholeSeconds / 60f }

            if (readings.isEmpty()) {
                return@withContext null
            }

            val mean = Statistics.mean(readings).roundPlaces(2)
            val stdev = Statistics.stdev(readings, mean = mean).roundPlaces(2)
            val max = readings.maxOrNull()?.roundPlaces(2) ?: 0f
            val median = Statistics.median(readings).roundPlaces(2)
            val quantile75 = Statistics.quantile(readings, 0.75f).roundPlaces(2)
            val quantile90 = Statistics.quantile(readings, 0.9f).roundPlaces(2)
            "Mean: $mean\n" +
                    "Stdev: $stdev\n" +
                    "Max: $max\n" +
                    "Median: $median\n" +
                    "75th: $quantile75\n" +
                    "90th: $quantile90"
        }
        // The timing card shows in debug builds, empty until there are timed points.
        debugTiming = text ?: debugTiming ?: ""
    }
}

/** What the overview shows, as text ready for the page. */
data class PathOverviewModel(
    val title: String,
    val subtitle: String?,
    val distance: String,
    val duration: String,
    val points: String,
    val difficulty: String,
    val elevationGain: String,
    val elevationLoss: String,
    val elevationMin: String?,
    val elevationMax: String?,
    val pathColor: Int,
    val lineStyle: String,
    val pointStyle: String
) {
    companion object {
        fun empty(pathColor: Int) = PathOverviewModel(
            title = "",
            subtitle = "",
            distance = "",
            duration = "",
            points = "",
            difficulty = "",
            elevationGain = "",
            elevationLoss = "",
            elevationMin = null,
            elevationMax = null,
            pathColor = pathColor,
            lineStyle = "",
            pointStyle = ""
        )
    }
}

/** A labelled value: what it is, its value, and its icon. */
data class PathDataPoint(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val onClick: (() -> Unit)? = null
)

/**
 * The path detail page: the map preview stays on top (it can grow to fill the screen), then the one fact you came for
 * (how far, how long) in a hero card, then the elevation profile, how to follow the path, and how it is drawn.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PathOverviewHost(
    model: PathOverviewModel,
    listState: LazyListState,
    onBack: (() -> Unit)?,
    onMenuClick: () -> Unit,
    onAddPoint: () -> Unit,
    mapPreview: @Composable () -> Unit,
    selectedPoint: (@Composable () -> Unit)?,
    chart: @Composable (Modifier) -> Unit,
    onElevationMinClick: () -> Unit,
    onElevationMaxClick: () -> Unit,
    onFollowToStart: () -> Unit,
    onFollowToEnd: () -> Unit,
    onColorClick: () -> Unit,
    onLineStyleClick: () -> Unit,
    onPointStyleClick: () -> Unit,
    timing: String?,
    modifier: Modifier = Modifier,
    isRecording: Boolean = false,
    onStopRecording: () -> Unit = {}
) {
    val density = LocalDensity.current
    val chartHeight = chartHeight()
    val elevationGain = PathDataPoint(model.elevationGain, appString(Res.string.ascent), TrailIcons.ElevationGain)
    val elevationLoss = PathDataPoint(model.elevationLoss, appString(Res.string.descent), TrailIcons.ElevationLoss)
    val elevationMin = model.elevationMin?.let {
        PathDataPoint(it, appString(Res.string.lowest_point_elevation), TrailIcons.ElevationLoss, onElevationMinClick)
    }
    val elevationMax = model.elevationMax?.let {
        PathDataPoint(it, appString(Res.string.highest_point_elevation), TrailIcons.ElevationGain, onElevationMaxClick)
    }

    TrailSensePage(
        title = model.title,
        subtitle = model.subtitle?.ifBlank { null } ?: appString(Res.string.no_group).takeIf { model.subtitle == null },
        onBack = onBack,
        backLabel = appString(Res.string.back),
        state = listState,
        modifier = modifier,
        actions = {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.testTag("path_menu_btn")
            ) {
                Icon(
                    painter = rememberVectorPainter(TrailIcons.MenuDots),
                    contentDescription = appString(Res.string.details)
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddPoint,
                icon = { Icon(rememberVectorPainter(TrailIcons.Add), contentDescription = null) },
                text = { Text(appString(Res.string.point)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_point_btn")
            )
        }
    ) {
        item(key = "map") {
            mapPreview()
        }

        if (selectedPoint != null) {
            item(key = "selected_point") {
                Box(Modifier.animateItem()) {
                    selectedPoint()
                }
            }
        }

        if (isRecording) {
            item(key = "recording") {
                PathRecordingBanner(onStop = onStopRecording, modifier = Modifier.animateItem())
            }
        }

        item(key = "hero") {
            PathSummaryHero(model)
        }

        trailSenseSection(key = "elevation", title = getAppService<AppStrings>().get(Res.string.elevation)) {
            row { _ ->
                TrailSenseStatGrid(
                    stats = listOf(
                        TrailSenseStat(
                            label = elevationGain.description,
                            value = elevationGain.title.ifBlank { "–" },
                            tone = TrailSenseTone.Tertiary,
                            icon = elevationGain.icon
                        ),
                        TrailSenseStat(
                            label = elevationLoss.description,
                            value = elevationLoss.title.ifBlank { "–" },
                            tone = TrailSenseTone.Primary,
                            icon = elevationLoss.icon
                        )
                    ),
                    columns = 2
                )
            }
            row { position ->
                TrailSenseGroupCard(
                    position = position,
                    contentPadding = PaddingValues(
                        start = TrailSenseSpacing.sm,
                        end = TrailSenseSpacing.lg,
                        top = TrailSenseSpacing.lg,
                        bottom = TrailSenseSpacing.sm
                    )
                ) {
                    chart(
                        Modifier
                            .fillMaxWidth()
                            .height(chartHeight)
                    )
                }
            }
            if (elevationMin != null) {
                row { position -> PathStatRow(elevationMin, position) }
            }
            if (elevationMax != null) {
                row { position -> PathStatRow(elevationMax, position) }
            }
        }

        item(key = "navigate") {
            Column {
                TrailSenseGroupHeader(getAppService<AppStrings>().get(Res.string.navigate))
                Row(horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
                    PathNavigateButton(
                        label = getAppService<AppStrings>().get(Res.string.path_direction_to_start),
                        rotation = 180f,
                        first = true,
                        onClick = onFollowToStart,
                        modifier = Modifier.weight(1f)
                    )
                    PathNavigateButton(
                        label = getAppService<AppStrings>().get(Res.string.path_direction_to_end),
                        rotation = 0f,
                        first = false,
                        onClick = onFollowToEnd,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        trailSenseSection(key = "appearance", title = getAppService<AppStrings>().get(Res.string.appearance)) {
            row { position ->
                TrailSenseListItem(
                    title = getAppService<AppStrings>().get(Res.string.color),
                    position = position,
                    onClick = onColorClick,
                    trailing = {
                        Surface(
                            modifier = Modifier
                                .padding(end = TrailSenseSpacing.sm)
                                .size(32.dp),
                            shape = MaterialShapes.Cookie9Sided.toShape(),
                            color = ComposeColor(model.pathColor)
                        ) {}
                    },
                    modifier = Modifier.testTag("path_color")
                )
            }
            row { position ->
                TrailSenseListItem(
                    title = getAppService<AppStrings>().get(Res.string.line_style),
                    subtitle = model.lineStyle,
                    position = position,
                    onClick = onLineStyleClick,
                    trailing = { PathChevron() }
                )
            }
            row { position ->
                TrailSenseListItem(
                    title = getAppService<AppStrings>().get(Res.string.point_style),
                    subtitle = model.pointStyle,
                    position = position,
                    onClick = onPointStyleClick,
                    trailing = { PathChevron() }
                )
            }
        }

        if (timing != null) {
            item(key = "timing") {
                TrailSenseGroupCard(position = TrailSenseGroupPosition.Single) {
                    Text(
                        text = timing,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Room for the floating button over the last row.
        item(key = "fab_spacer") { Spacer(Modifier.height(56.dp)) }
    }
}

/**
 * The one fact you open a path for, as plain emphasised type: how far it goes, with how long it took, its points
 * and its difficulty as three quiet readings under it (no card, no badge).
 */
@Composable
private fun PathSummaryHero(model: PathOverviewModel) {
    PlacesKeyValue(
        value = model.distance.ifBlank { appString(Res.string.dash) },
        verdict = appString(Res.string.distance).ifBlank { null },
        modifier = Modifier.animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec()),
        below = {
            val stats = listOf(
                PathDataPoint(model.duration, appString(Res.string.duration), TrailIcons.ToolClock),
                PathDataPoint(model.points, appString(Res.string.points), TrailIcons.Location),
                PathDataPoint(model.difficulty, appString(Res.string.difficulty), TrailIcons.Steps)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)) {
                stats.forEach { state ->
                    PathHeroStat(state, modifier = Modifier.weight(1f))
                }
            }
        }
    )
}

@Composable
private fun PathHeroStat(
    state: PathDataPoint,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
    ) {
        Text(
            text = state.title.ifBlank { appString(Res.string.dash) },
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = state.description,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** One half of the navigate pair: an arrow in a cookie and where it takes you. */
@Composable
private fun PathNavigateButton(
    label: String,
    rotation: Float,
    first: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outer = 24.dp
    val inner = 6.dp
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(
            topStart = if (first) outer else inner,
            bottomStart = if (first) outer else inner,
            topEnd = if (first) inner else outer,
            bottomEnd = if (first) inner else outer
        ),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 76.dp)
                .padding(horizontal = TrailSenseSpacing.md, vertical = TrailSenseSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowWell(androidx.compose.ui.graphics.vector.rememberVectorPainter(TrailIcons.NavigationArrow), rotation = rotation)
            Spacer(Modifier.width(TrailSenseSpacing.md))
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** A value as a row: the label on the left, the value trailing; tapping it selects that point. */
@Composable
private fun PathStatRow(state: PathDataPoint, position: TrailSenseGroupPosition) {
    TrailSenseListItem(
        title = state.description,
        icon = rememberVectorPainter(state.icon),
        position = position,
        onClick = state.onClick,
        trailing = {
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(end = TrailSenseSpacing.xs)
            )
        }
    )
}

@Composable
private fun PathChevron() {
    Icon(
        painter = rememberVectorPainter(TrailIcons.DropDown),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(end = TrailSenseSpacing.xs)
            .size(20.dp)
    )
}

@Composable
fun PathIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier.size(TrailSenseSize.minimumTouchTarget),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Icon(
            painter = rememberVectorPainter(icon),
            contentDescription = contentDescription,
            modifier = Modifier.size(TrailSenseSize.leadingIcon)
        )
    }
}

/** Shown while Backtrack records into this path, with a button that stops it. */
@Composable
private fun PathRecordingBanner(onStop: () -> Unit, modifier: Modifier = Modifier) {
    TrailSenseGroupCard(
        position = TrailSenseGroupPosition.Single,
        modifier = modifier.testTag("path_recording_banner"),
        contentPadding = PaddingValues(
            start = TrailSenseSpacing.lg,
            end = TrailSenseSpacing.sm,
            top = TrailSenseSpacing.xs,
            bottom = TrailSenseSpacing.xs
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = appString(Res.string.recording),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            FilledTonalIconButton(
                onClick = onStop,
                modifier = Modifier.testTag("stop_recording_btn")
            ) {
                Icon(
                    painter = rememberVectorPainter(TrailIcons.BaselineStop24),
                    contentDescription = appString(Res.string.stop)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PathOverviewHostPreview() {
    TrailSenseTheme {
        PathOverviewHost(
            model = PathOverviewModel.empty(0),
            listState = rememberLazyListState(),
            onBack = {},
            onMenuClick = {},
            onAddPoint = {},
            mapPreview = {},
            selectedPoint = null,
            chart = {},
            onElevationMinClick = {},
            onElevationMaxClick = {},
            onFollowToStart = {},
            onFollowToEnd = {},
            onColorClick = {},
            onLineStyleClick = {},
            onPointStyleClick = {},
            timing = null
        )
    }
}

