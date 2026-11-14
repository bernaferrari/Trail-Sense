@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.paths.ui

import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.views.compose.from

import com.kylecorry.trail_sense.shared.views.compose.chartHeight

import android.content.Context
import android.content.ContextWrapper
import android.content.res.ColorStateList
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
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
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.colors.ColorInts
import com.kylecorry.trail_sense.shared.debugging.DebugPathElevationsCommand
import com.kylecorry.trail_sense.shared.debugging.isDebug
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.declination.IDeclinationStrategy
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.events.EventData
import com.kylecorry.trail_sense.shared.events.ToolBroadcasts
import com.kylecorry.trail_sense.shared.extensions.lineString
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.extensions.range
import com.kylecorry.trail_sense.shared.extensions.withCancelableLoading
import com.kylecorry.trail_sense.shared.getInstance
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.icons.trail.BaselineStop24
import com.kylecorry.trail_sense.shared.io.IOFactory
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.ConfigurableGeoJsonLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.setLayers
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.start
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.stop
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.navigation.TrailSenseAppNavigation
import com.kylecorry.trail_sense.shared.navigation.rememberSystemBack
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sharing.ActionItem
import com.kylecorry.trail_sense.shared.toRelativeDistance
import com.kylecorry.trail_sense.shared.views.ColorPickerDialog
import com.kylecorry.trail_sense.shared.views.ColorScaleContent
import com.kylecorry.trail_sense.shared.views.ColorScaleState
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSheet
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseStat
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseStatGrid
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.beacons.infrastructure.BeaconNavigator
import com.kylecorry.trail_sense.tools.beacons.infrastructure.IBeaconNavigator
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.IBeaconService
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesKeyValue
import com.kylecorry.trail_sense.tools.map.map_layers.MyLocationGeoJsonSource
import com.kylecorry.trail_sense.tools.map.map_layers.ScaleBarLayer
import com.kylecorry.trail_sense.tools.map.ui.MapView
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathFollow
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.paths.PathsToolRegistration
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathDirection
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.PathPointColoringStyle
import com.kylecorry.trail_sense.tools.paths.domain.beacon.IPathPointBeaconConverter
import com.kylecorry.trail_sense.tools.paths.domain.beacon.TemporaryPathPointBeaconConverter
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingDifficulty
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService
import com.kylecorry.trail_sense.tools.paths.infrastructure.BacktrackScheduler
import com.kylecorry.trail_sense.tools.paths.infrastructure.commands.AddPathPointCommand
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
import com.kylecorry.trail_sense.tools.paths.ui.commands.ReplacePathElevationsCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.SimplifyPathCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.SimplifyPathDialog
import com.kylecorry.trail_sense.tools.paths.ui.commands.SimplifyPathRequest
import com.kylecorry.trail_sense.tools.paths.ui.commands.TogglePathVisibilityCommand
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCatalog
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
        val sensorService = getAppService<SensorService>()
        val gps = sensorService.getGPS()
        PathOverviewViewModel(
            pathId = route.pathId,
            pathService = getAppService<IPathService>(),
            navigator = Navigator.getInstance(appContext),
            gps = gps,
            compass = sensorService.getCompass(),
            declinationProvider = DeclinationFactory().getDeclinationStrategy(userPrefs, gps),
            baseDistanceUnits = userPrefs.baseDistanceUnits,
            isDebug = isDebug(),
            debugElevations = { raw, smoothed -> DebugPathElevationsCommand(appContext, raw, smoothed).execute() },
            isBacktrackOn = { BacktrackScheduler.isOn(appContext) },
            stopBacktrack = { getAppService<ToolCatalog>().getService(PathsToolRegistration.SERVICE_BACKTRACK)?.disable() }
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
    val layer = remember { ConfigurableGeoJsonLayer(prefs.paths.useFastPathRendering) }
    val map = remember(context) {
        MapView(context).apply {
            id = R.id.path_image
            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorSecondary))
            keepScreenOn = true
            isInteractive = true
            setLayers(
                listOf(
                    layer,
                    GeoJsonLayer(MyLocationGeoJsonSource(), MyLocationGeoJsonSource.SOURCE_ID, prefs.paths.useFastPathRendering),
                    ScaleBarLayer(prefs.baseDistanceUnits, formatService)
                )
            )
        }
    }
    val legend = remember { ColorScaleState() }
    val pointFactory = remember(context, path?.style?.point) {
        getPointFactory(path?.style?.point ?: PathPointColoringStyle.None)
    }
    val bounds = remember(waypoints) { Geography.getBounds(waypoints.map { it.coordinate }) }
    val converter: IPathPointBeaconConverter = remember(context) {
        TemporaryPathPointBeaconConverter(context.getString(R.string.waypoint))
    }
    val beaconNavigator: IBeaconNavigator = remember(appContext, appNavigator) {
        BeaconNavigator(
            getAppService<IBeaconService>(),
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
            current.style.point,
            viewModel.selectedPointId
        ).map {
            GeoJsonFeature.point(
                it.coordinate,
                it.id,
                it.name,
                color = it.color,
                icon = it.icon?.id,
                iconColor = Colors.mostContrastingColor(ColorInts.White, ColorInts.Black, it.color),
                size = 8f,
                iconSize = 8f * 0.75f,
                isClickable = true,
                strokeColor = ColorInts.Transparent
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
            MoveIPathCommand(AppHost.from(context), pathService).execute(current)
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
        DeletePointCommand(scope).execute(current, point)
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
            PathAction.ReplaceElevations,
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
                PathAction.ReplaceElevations -> context.getString(R.string.replace_elevations)
                PathAction.ViewPoints -> context.getString(R.string.points)
                PathAction.Move -> context.getString(R.string.move_to)
                else -> null
            }
            name?.let {
                ActionItem(it, TrailIcons.MenuDots) {
                    when (action) {
                        PathAction.Export -> ExportPathCommand(
                            AppHost.from(context),
                            scope,
                            IOFactory().createGpxService(
                                context,
                                // File pickers need the activity's result launcher, an Activity-only API.
                                requireNotNull(context.findMainActivity())
                            ),
                            pathService
                        ).execute(current)

                        PathAction.Rename -> RenamePathCommand(scope, pathService).execute(current)
                        PathAction.Keep -> KeepPathCommand(scope, pathService).execute(current)
                        PathAction.ToggleVisibility -> TogglePathVisibilityCommand(scope, pathService)
                            .execute(current)

                        PathAction.Simplify -> simplifyCommand.execute(current)
                        PathAction.ReplaceElevations -> ReplacePathElevationsCommand(scope, pathService)
                            .execute(current)
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
                AddPathPointCommand(viewModel.pathId).execute()
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
        val duration = if (start != null && end != null && (end - start) > 1.minutes) {
            (end - start)
        } else {
            viewModel.calculatedDuration
        }
        val elevationRange = viewModel.elevationRange
        fun format(d: Distance) = formatService.formatDistance(d, Units.getDecimalPlaces(d.units), false)
        PathOverviewModel(
            title = PathNameFactory().getName(current),
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
            lineStyle = getLineStyleNames()[current.style.line] ?: "",
            pointStyle = getPointStyleNames()[current.style.point] ?: ""
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
        onBack = rememberSystemBack(),
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
                modifier = chartModifier.testTag("chart")
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
            viewModel.path?.let { ChangePathLineStyleCommand(scope).execute(it) }
        },
        onPointStyleClick = {
            viewModel.path?.let { ChangePointStyleCommand(scope).execute(it) }
        },
        timing = viewModel.debugTiming,
        modifier = modifier.fillMaxSize(),
        isRecording = viewModel.isRecording,
        onStopRecording = viewModel::stopRecording
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
                contentDescription = stringResource(R.string.center),
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

private tailrec fun Context.findMainActivity(): MainActivity? = when (this) {
    is MainActivity -> this
    is ContextWrapper -> baseContext.findMainActivity()
    else -> null
}
