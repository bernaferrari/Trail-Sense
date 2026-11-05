@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.paths.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.andromeda.sense.compass.ICompass
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.sol.units.Bearing
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.declination.IDeclinationStrategy
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.tools.paths.ui.commands.SimplifyPathDialog
import com.kylecorry.trail_sense.tools.paths.ui.commands.SimplifyPathRequest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


import com.kylecorry.trail_sense.shared.navigation.Route

import androidx.compose.material3.MaterialShapes
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.toShape
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextOverflow
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseStat
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseStatGrid
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesKeyValue
import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.MathExtensions.roundPlaces
import com.kylecorry.sol.math.statistics.Statistics
import com.kylecorry.sol.science.geography.Geography
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.debugging.DebugPathElevationsCommand
import com.kylecorry.trail_sense.shared.debugging.isDebug
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.extensions.lineString
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.extensions.range
import com.kylecorry.trail_sense.shared.extensions.withCancelableLoading
import com.kylecorry.trail_sense.shared.io.IOFactory
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.ConfigurableGeoJsonLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.setLayers
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.start
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.stop
import com.kylecorry.trail_sense.shared.navigation.TrailSenseAppNavigation
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.toRelativeDistance
import com.kylecorry.trail_sense.shared.views.ColorScaleContent
import com.kylecorry.trail_sense.shared.views.ColorScaleState
import com.kylecorry.trail_sense.shared.views.ColorPickerDialog
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.sharing.ActionItem
import com.kylecorry.trail_sense.tools.beacons.infrastructure.BeaconNavigator
import com.kylecorry.trail_sense.tools.beacons.infrastructure.IBeaconNavigator
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.map.map_layers.MyLocationGeoJsonSource
import com.kylecorry.trail_sense.tools.map.map_layers.ScaleBarLayer
import com.kylecorry.trail_sense.tools.map.ui.MapView
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.PathPointColoringStyle
import com.kylecorry.trail_sense.tools.paths.domain.beacon.IPathPointBeaconConverter
import com.kylecorry.trail_sense.tools.paths.domain.beacon.TemporaryPathPointBeaconConverter
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingDifficulty
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathFollow
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.paths.domain.PathDirection
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService
import com.kylecorry.trail_sense.tools.paths.infrastructure.commands.AddPathPointCommand
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.paths.ui.commands.ChangePathColorCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.ChangePathLineStyleCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.ChangePointStyleCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.CreateBeaconFromPointCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.DeletePointCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.ExportPathCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.KeepPathCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.MoveIPathCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.NavigateToPointCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.RenamePathCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.SimplifyPathCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.TogglePathVisibilityCommand
import kotlinx.coroutines.Dispatchers
import java.time.Duration
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.platform.testTag
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PathOverviewToolScreen(
    route: Route.PathOverview,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val appNavigator = LocalNavigator.current
    val effects = LocalScreenEffects.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val viewModel = viewModel {
        val userPrefs = UserPreferences(appContext)
        val sensorService = SensorService(appContext)
        val gps = sensorService.getGPS()
        PathOverviewViewModel(
            pathId = route.pathId,
            pathService = PathService.getInstance(appContext),
            navigator = Navigator.getInstance(appContext),
            gps = gps,
            compass = sensorService.getCompass(),
            declinationProvider = DeclinationFactory().getDeclinationStrategy(userPrefs, gps),
            baseDistanceUnits = userPrefs.baseDistanceUnits,
            isDebug = isDebug(),
            debugElevations = { raw, smoothed -> DebugPathElevationsCommand(appContext, raw, smoothed).execute() }
        )
    }
    val prefs = remember(appContext) { UserPreferences(appContext) }
    val formatService = remember(appContext) { FormatService.getInstance(appContext) }
    val pathService = viewModel.pathService
    val path = viewModel.path
    val waypoints = viewModel.waypoints
    val listState = rememberLazyListState()
    var showPathPointsSheet by remember { mutableStateOf(false) }
    var pathColorPickerTarget by remember { mutableStateOf<Path?>(null) }
    var simplifyRequest by remember { mutableStateOf<SimplifyPathRequest?>(null) }
    val simplifyCommand = remember(context, scope) {
        SimplifyPathCommand(context, scope, pathService) { simplifyRequest = it }
    }

    // The map and its layers are rendering objects, so they live with the composition.
    val layer = remember { ConfigurableGeoJsonLayer() }
    val map = remember(context) {
        MapView(context).apply {
            id = R.id.path_image
            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorSecondary))
            keepScreenOn = true
            isInteractive = true
            setLayers(
                listOf(
                    layer,
                    GeoJsonLayer(MyLocationGeoJsonSource(), MyLocationGeoJsonSource.SOURCE_ID),
                    ScaleBarLayer()
                )
            )
        }
    }
    val legend = remember { ColorScaleState() }
    val pointFactory = remember(context, path?.style?.point) {
        getPointFactory(context, path?.style?.point ?: PathPointColoringStyle.None)
    }
    val bounds = remember(waypoints) { Geography.getBounds(waypoints.map { it.coordinate }) }
    val converter: IPathPointBeaconConverter = remember(context) {
        TemporaryPathPointBeaconConverter(context.getString(R.string.waypoint))
    }
    val beaconNavigator: IBeaconNavigator = remember(appContext, appNavigator) {
        BeaconNavigator(
            BeaconService(appContext),
            TrailSenseAppNavigation(appNavigator),
            Navigator.getInstance(appContext)
        )
    }

    DisposableEffect(layer) {
        layer.renderer.configureLineStringRenderer(shouldRenderWithDrawLines = prefs.paths.useFastPathRendering)
        layer.setOnClickListener { feature ->
            if (viewModel.selectedPointId != null) {
                viewModel.deselectPoint()
                return@setOnClickListener true
            }
            val point = viewModel.waypoints.firstOrNull { point -> point.id == feature.id }
            if (point != null) {
                viewModel.selectPoint(point)
                true
            } else {
                false
            }
        }
        onDispose { }
    }

    LaunchedEffect(viewModel) {
        viewModel.navigationStarted.collect {
            appNavigator.navigate(Route.Navigation(navigationStarted = true))
        }
    }

    // The sensors run while the screen is started, as their LiveData observers did.
    LifecycleStartEffect(viewModel) {
        viewModel.startSensors()
        onStopOrDispose {
            viewModel.stopSensors()
        }
    }

    LifecycleResumeEffect(map) {
        map.start()
        onPauseOrDispose {
            map.stop()
        }
    }

    // The map follows where you are and which way you face.
    LaunchedEffect(map) {
        snapshotFlow { Triple(viewModel.userLocation, viewModel.userLocationAccuracy, viewModel.userAzimuth) }
            .collect { (location, accuracy, azimuth) ->
                map.userLocation = location
                map.userLocationAccuracy = accuracy
                azimuth?.let { map.userAzimuth = it }
            }
    }
    LaunchedEffect(bounds) {
        map.fitIntoView(bounds)
    }
    LaunchedEffect(path, waypoints, viewModel.selectedPointId) {
        val current = path ?: return@LaunchedEffect
        val waypointFeatures = waypoints.asBeacons(
            context,
            current.style.point,
            viewModel.selectedPointId
        ).map {
            GeoJsonFeature.point(
                it.coordinate,
                it.id,
                it.name,
                color = it.color,
                icon = it.icon?.id,
                iconColor = Colors.mostContrastingColor(Color.WHITE, Color.BLACK, it.color),
                size = 8f,
                iconSize = 8f * 0.75f,
                isClickable = true,
                strokeColor = Color.TRANSPARENT
            )
        }

        val pathFeature = GeoJsonFeature.lineString(
            waypoints.map { it.coordinate },
            current.id,
            current.name,
            lineStyle = current.style.line,
            color = current.style.color
        )

        layer.setData(GeoJsonFeatureCollection(waypointFeatures + pathFeature))
    }
    LaunchedEffect(path, waypoints, pointFactory) {
        val current = path ?: return@LaunchedEffect
        legend.colorScale = pointFactory.createColorScale(waypoints)
        legend.labels = pointFactory.createLabelMap(waypoints)
        legend.isVisible = current.style.point != PathPointColoringStyle.None
    }

    fun followPath(follow: PathFollow) {
        viewModel.followPath(follow)
    }

    fun viewPoints() {
        scope.launch { listState.animateScrollToItem(0) }
        showPathPointsSheet = true
    }

    fun movePath() {
        val current = viewModel.path ?: return
        scope.launch {
            MoveIPathCommand(context, pathService).execute(current)
        }
    }

    fun navigateDirectlyToWaypoint(point: PathPoint) {
        val selectedPath = viewModel.path ?: return
        tryOrNothing {
            NavigateToPointCommand(scope, converter, beaconNavigator).execute(selectedPath, point)
        }
    }

    fun deleteWaypoint(point: PathPoint) {
        val current = viewModel.path ?: return
        DeletePointCommand(context, scope).execute(current, point)
    }

    fun createBeacon(point: PathPoint) {
        val current = viewModel.path ?: return
        CreateBeaconFromPointCommand(context).execute(current, point)
    }

    fun showPathMenu() {
        val current = viewModel.path ?: return
        val actions = listOf(
            PathAction.Rename,
            PathAction.Keep,
            PathAction.ToggleVisibility,
            PathAction.Export,
            PathAction.Simplify,
            PathAction.ViewPoints,
            PathAction.Move
        )

        val actionItems = actions.mapNotNull { action ->
            val name = when (action) {
                PathAction.Rename -> context.getString(R.string.rename)
                PathAction.Keep -> if (current.temporary) context.getString(R.string.keep_forever) else null
                PathAction.ToggleVisibility -> if (current.style.visible) {
                    context.getString(R.string.hide)
                } else {
                    context.getString(R.string.show)
                }

                PathAction.Export -> context.getString(R.string.export)
                PathAction.Simplify -> context.getString(R.string.simplify)
                PathAction.ViewPoints -> context.getString(R.string.points)
                PathAction.Move -> context.getString(R.string.move_to)
                else -> null
            }
            name?.let {
                ActionItem(it, TrailIcons.MenuDots) {
                    when (action) {
                        PathAction.Export -> ExportPathCommand(
                            context,
                            scope,
                            IOFactory().createGpxService(
                                context,
                                // File pickers need the activity's result launcher, an Activity-only API.
                                requireNotNull(context.findMainActivity())
                            ),
                            pathService
                        ).execute(current)

                        PathAction.Rename -> RenamePathCommand(context, scope, pathService).execute(current)
                        PathAction.Keep -> KeepPathCommand(context, scope, pathService).execute(current)
                        PathAction.ToggleVisibility -> TogglePathVisibilityCommand(context, scope, pathService)
                            .execute(current)

                        PathAction.Simplify -> simplifyCommand.execute(current)
                        PathAction.ViewPoints -> viewPoints()
                        PathAction.Move -> movePath()
                        else -> {
                            // Do nothing
                        }
                    }
                }
            }
        }

        scope.launch {
            effects.chooseAction(current.name ?: context.getString(R.string.path), null, actionItems)?.action?.invoke()
        }
    }

    fun addPoint() {
        scope.launch {
            var wasSuccessful = false
            val job = launch {
                AddPathPointCommand(context, viewModel.pathId).execute()
                wasSuccessful = true
            }

            Alerts.withCancelableLoading(
                context,
                context.getString(R.string.loading),
                onCancel = { job.cancel() }) {
                job.join()
                if (wasSuccessful) {
                    effects.showMessage(context.getString(R.string.point_added))
                }
            }
        }
    }

    fun toggleFullscreen() {
        viewModel.isFullscreen = !viewModel.isFullscreen
        scope.launch {
            delay(30)
            listState.animateScrollToItem(0)
            map.fitIntoView(bounds)
        }
    }

    val units = prefs.baseDistanceUnits
    val model = path?.let { current ->
        val distance = current.metadata.distance.convertTo(units).toRelativeDistance()
        val start = current.metadata.duration?.start
        val end = current.metadata.duration?.end
        val duration = if (start != null && end != null && Duration.between(start, end) > Duration.ofMinutes(1)) {
            Duration.between(start, end)
        } else {
            viewModel.calculatedDuration
        }
        val elevationRange = viewModel.elevationRange
        fun format(d: Distance) = formatService.formatDistance(d, Units.getDecimalPlaces(d.units), false)
        PathOverviewModel(
            title = PathNameFactory(context).getName(current),
            subtitle = viewModel.parentName,
            distance = format(distance),
            duration = formatService.formatDuration(duration, false),
            points = current.metadata.waypoints.toString(),
            difficulty = formatService.formatHikingDifficulty(viewModel.difficulty),
            elevationGain = format(viewModel.elevationGain),
            elevationLoss = format(viewModel.elevationLoss),
            elevationMin = elevationRange?.let { format(it.start) },
            elevationMax = elevationRange?.let { format(it.end) },
            pathColor = current.style.color,
            lineStyle = getLineStyleNames(context)[current.style.line] ?: "",
            pointStyle = getPointStyleNames(context)[current.style.point] ?: ""
        )
    } ?: PathOverviewModel.empty(ContextCompat.getColor(context, R.color.blue))

    val mapHeight = if (viewModel.isFullscreen) {
        val legendHeight = with(density) { 72.dp.roundToPx() }
        listState.layoutInfo.viewportSize.height - legendHeight
    } else {
        with(density) { 250.dp.roundToPx() }
    }

    PathOverviewHost(
        model = model,
        listState = listState,
        onBack = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher?.let { { it.onBackPressed() } },
        onMenuClick = ::showPathMenu,
        onAddPoint = ::addPoint,
        mapPreview = {
            PathMapPreview(
                map = map,
                mapHeightPx = mapHeight,
                isFullscreen = viewModel.isFullscreen,
                onFullscreenClick = ::toggleFullscreen,
                legend = legend
            )
        },
        selectedPoint = viewModel.selectedPoint?.let { point ->
            {
                PathPointRow(
                    point = point,
                    formatService = formatService,
                    baseDistanceUnits = units,
                    onCreateBeacon = ::createBeacon,
                    onDelete = ::deleteWaypoint,
                    onNavigate = { followPath(PathFollow.ToPoint(it.id)) },
                    onNavigateDirect = ::navigateDirectlyToWaypoint,
                    onView = {},
                    position = TrailSenseGroupPosition.Single,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        chart = { chartModifier ->
            PathElevationChart(
                path = viewModel.chartPoints,
                color = path?.style?.color ?: prefs.paths.defaultPathColor.color,
                highlighted = viewModel.selectedPoint,
                baseDistanceUnits = units,
                formatter = formatService,
                onPointClick = viewModel::selectPoint,
                modifier = chartModifier
            )
        },
        onElevationMinClick = {
            waypoints.filter { it.elevation != null }.minByOrNull { it.elevation!! }?.let { viewModel.selectPoint(it) }
        },
        onElevationMaxClick = {
            waypoints.filter { it.elevation != null }.maxByOrNull { it.elevation!! }?.let { viewModel.selectPoint(it) }
        },
        onFollowToStart = { followPath(PathFollow.Direction(PathDirection.ToStart)) },
        onFollowToEnd = { followPath(PathFollow.Direction(PathDirection.ToEnd)) },
        onColorClick = { viewModel.path?.let { pathColorPickerTarget = it } },
        onLineStyleClick = {
            viewModel.path?.let { ChangePathLineStyleCommand(context, scope).execute(it) }
        },
        onPointStyleClick = {
            viewModel.path?.let { ChangePointStyleCommand(context, scope).execute(it) }
        },
        timing = viewModel.debugTiming,
        modifier = modifier.fillMaxSize()
    )

    if (showPathPointsSheet) {
        TrailSenseSheet(
            onDismissRequest = {
                showPathPointsSheet = false
            },
            // The list brings its own gutters
            contentPadding = PaddingValues(0.dp)
        ) {
            PathPointsContent(
                points = waypoints,
                formatService = formatService,
                baseDistanceUnits = units,
                onCreateBeacon = {
                    createBeacon(it)
                    showPathPointsSheet = false
                },
                onDelete = {
                    deleteWaypoint(it)
                },
                onNavigate = {
                    followPath(PathFollow.ToPoint(it.id))
                    showPathPointsSheet = false
                },
                onNavigateDirect = {
                    navigateDirectlyToWaypoint(it)
                    showPathPointsSheet = false
                },
                onView = {
                    viewModel.selectPoint(it)
                    showPathPointsSheet = false
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    val colorPickerPath = pathColorPickerTarget
    if (colorPickerPath != null) {
        ColorPickerDialog(
            title = stringResource(R.string.path_color),
            selectedColor = AppColor.values().firstOrNull {
                it.color == colorPickerPath.style.color
            } ?: AppColor.Gray,
            onColorPicked = {
                if (it != null) {
                    ChangePathColorCommand(scope, pathService).execute(colorPickerPath, it)
                }
                pathColorPickerTarget = null
            },
            onDismiss = {
                pathColorPickerTarget = null
            }
        )
    }

    simplifyRequest?.let { request ->
        SimplifyPathDialog(
            request = request,
            onApply = { simplifyCommand.apply(request.path, it) },
            onDismiss = { simplifyRequest = null }
        )
    }
}

/**
 * A path's overview state: the path, its points (newest first, elevations corrected), the hiking and elevation
 * statistics, the selected point and where you are. Plain Kotlin over the path service, the navigator and the
 * sensors; the map and the chart are drawn by the screen from here.
 */
internal class PathOverviewViewModel(
    val pathId: Long,
    val pathService: PathService,
    private val navigator: Navigator,
    private val gps: IGPS,
    private val compass: ICompass,
    private val declinationProvider: IDeclinationStrategy,
    private val baseDistanceUnits: DistanceUnits,
    private val isDebug: Boolean,
    private val debugElevations: suspend (List<PathPoint>, List<PathPoint>) -> Unit
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
        viewModelScope.launch {
            pathService.getLivePath(pathId).asFlow().collect {
                path = it
                updateParent(it)
            }
        }
        viewModelScope.launch {
            pathService.getWaypointsLive(pathId).asFlow().collectLatest {
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
                .filter { it.time != null }
                .sortedBy { it.time }
                .zipWithNext { a, b -> Duration.between(a.time, b.time).seconds / 60f }

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
private data class PathOverviewModel(
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
private data class PathDataPoint(
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
private fun PathOverviewHost(
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
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val chartHeight = with(density) {
        context.resources.getDimensionPixelSize(R.dimen.chart_height).toDp()
    }
    val elevationGain = PathDataPoint(model.elevationGain, stringResource(R.string.ascent), TrailIcons.ElevationGain)
    val elevationLoss = PathDataPoint(model.elevationLoss, stringResource(R.string.descent), TrailIcons.ElevationLoss)
    val elevationMin = model.elevationMin?.let {
        PathDataPoint(it, stringResource(R.string.lowest_point_elevation), TrailIcons.ElevationLoss, onElevationMinClick)
    }
    val elevationMax = model.elevationMax?.let {
        PathDataPoint(it, stringResource(R.string.highest_point_elevation), TrailIcons.ElevationGain, onElevationMaxClick)
    }

    TrailSensePage(
        title = model.title,
        subtitle = model.subtitle?.ifBlank { null } ?: stringResource(R.string.no_group).takeIf { model.subtitle == null },
        onBack = onBack,
        backLabel = stringResource(R.string.back),
        state = listState,
        modifier = modifier,
        actions = {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.testTag("path_menu_btn")
            ) {
                Icon(
                    painter = rememberVectorPainter(TrailIcons.MenuDots),
                    contentDescription = stringResource(R.string.details)
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddPoint,
                icon = { Icon(rememberVectorPainter(TrailIcons.Add), contentDescription = null) },
                text = { Text(stringResource(R.string.point)) },
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

        item(key = "hero") {
            PathSummaryHero(model)
        }

        trailSenseSection(key = "elevation", title = context.getString(R.string.elevation)) {
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
                TrailSenseGroupHeader(context.getString(R.string.navigate))
                Row(horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
                    PathNavigateButton(
                        label = context.getString(R.string.path_direction_to_start),
                        rotation = 180f,
                        first = true,
                        onClick = onFollowToStart,
                        modifier = Modifier.weight(1f)
                    )
                    PathNavigateButton(
                        label = context.getString(R.string.path_direction_to_end),
                        rotation = 0f,
                        first = false,
                        onClick = onFollowToEnd,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        trailSenseSection(key = "appearance", title = context.getString(R.string.appearance)) {
            row { position ->
                TrailSenseListItem(
                    title = context.getString(R.string.color),
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
                    title = context.getString(R.string.line_style),
                    subtitle = model.lineStyle,
                    position = position,
                    onClick = onLineStyleClick,
                    trailing = { PathChevron() }
                )
            }
            row { position ->
                TrailSenseListItem(
                    title = context.getString(R.string.point_style),
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

/** The map preview, with its color legend tucked under it when points are colored. */
@Composable
private fun PathMapPreview(
    map: MapView,
    mapHeightPx: Int,
    isFullscreen: Boolean,
    onFullscreenClick: () -> Unit,
    legend: ColorScaleState
) {
    val density = LocalDensity.current
    val mapHeight by animateDpAsState(
        targetValue = with(density) { mapHeightPx.toDp() },
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "pathMapHeight"
    )
    val mapBackground = MaterialTheme.colorScheme.surfaceContainerHigh.toArgb()
    SideEffect {
        map.backgroundTintList = ColorStateList.valueOf(mapBackground)
    }
    Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(mapHeight)
                .clip(RoundedCornerShape(TrailSenseShape.groupOuter))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            map.Content(Modifier.fillMaxSize())
            PathIconButton(
                icon = if (isFullscreen) TrailIcons.FullscreenExit else TrailIcons.Recenter,
                contentDescription = R.string.center,
                onClick = onFullscreenClick,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(TrailSenseSpacing.md)
            )
        }
        AnimatedVisibility(
            visible = legend.isVisible,
            enter = expandVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                    fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
            exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                    fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec())
        ) {
            ColorScaleContent(
                state = legend,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            )
        }
    }
}

/**
 * The one fact you open a path for, as plain emphasised type: how far it goes, with how long it took, its points
 * and its difficulty as three quiet readings under it (no card, no badge).
 */
@Composable
private fun PathSummaryHero(model: PathOverviewModel) {
    PlacesKeyValue(
        value = model.distance.ifBlank { stringResource(R.string.dash) },
        verdict = stringResource(R.string.distance).ifBlank { null },
        modifier = Modifier.animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec()),
        below = {
            val stats = listOf(
                PathDataPoint(model.duration, stringResource(R.string.duration), TrailIcons.ToolClock),
                PathDataPoint(model.points, stringResource(R.string.points), TrailIcons.Location),
                PathDataPoint(model.difficulty, stringResource(R.string.difficulty), TrailIcons.Steps)
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
            text = state.title.ifBlank { stringResource(R.string.dash) },
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
private fun PathIconButton(
    icon: ImageVector,
    contentDescription: Int,
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
            contentDescription = stringResource(contentDescription),
            modifier = Modifier.size(TrailSenseSize.leadingIcon)
        )
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

private tailrec fun Context.findMainActivity(): MainActivity? = when (this) {
    is MainActivity -> this
    is ContextWrapper -> baseContext.findMainActivity()
    else -> null
}
