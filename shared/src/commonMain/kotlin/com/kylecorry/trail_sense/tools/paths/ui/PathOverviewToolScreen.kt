@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.paths.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.sol.science.geography.Geography
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.colors.ColorInts
import com.kylecorry.trail_sense.shared.debugging.isDebug
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.dialogs.withCancelableLoading
import com.kylecorry.trail_sense.shared.extensions.lineString
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.icons.trail.FullscreenExit
import com.kylecorry.trail_sense.shared.icons.trail.MenuDots
import com.kylecorry.trail_sense.shared.icons.trail.Recenter
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.io.SharedGpxIOService
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.ConfigurableGeoJsonLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.setLayers
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.start
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.stop
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.navigation.RouteLocation
import com.kylecorry.trail_sense.shared.navigation.TrailSenseAppNavigation
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sharing.ActionItem
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.shared.toRelativeDistance
import com.kylecorry.trail_sense.shared.views.ColorPickerDialog
import com.kylecorry.trail_sense.shared.views.ColorScaleContent
import com.kylecorry.trail_sense.shared.views.ColorScaleState
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSheet
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.rememberTrailSenseBack
import com.kylecorry.trail_sense.tools.beacons.infrastructure.BeaconNavigator
import com.kylecorry.trail_sense.tools.beacons.infrastructure.IBeaconNavigator
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.IBeaconService
import com.kylecorry.trail_sense.tools.map.map_layers.ScaleBarLayer
import com.kylecorry.trail_sense.tools.map.ui.SharedMapView
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathFollow
import com.kylecorry.trail_sense.tools.navigation.infrastructure.INavigator
import com.kylecorry.trail_sense.tools.paths.PathsToolServices
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathDirection
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.PathPointColoringStyle
import com.kylecorry.trail_sense.tools.paths.domain.beacon.IPathPointBeaconConverter
import com.kylecorry.trail_sense.tools.paths.domain.beacon.TemporaryPathPointBeaconConverter
import com.kylecorry.trail_sense.tools.paths.infrastructure.commands.AddPathPointCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.ChangePathColorCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.ChangePathLineStyleCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.ChangePointStyleCommand
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
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PathOverviewToolScreen(
    route: Route.PathOverview,
    modifier: Modifier = Modifier,
    debugElevations: suspend (List<PathPoint>, List<PathPoint>) -> Unit = { _, _ -> }
) {
    val host = AppHost.current()
    val strings = remember { getAppService<AppStrings>() }
    val appNavigator = LocalNavigator.current
    val effects = LocalScreenEffects.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val prefs = remember { getAppService<UserPreferences>() }
    val backtrack = remember { getAppService<ToolCatalog>().getService(PathsToolServices.BACKTRACK) }
    val navigation = remember { getAppService<INavigator>() }
    val viewModel = viewModel {
        val sensorService = getAppService<SensorService>()
        val gps = sensorService.getGPS()
        PathOverviewViewModel(
            pathId = route.pathId,
            pathService = getAppService<IPathService>(),
            navigator = navigation,
            gps = gps,
            compass = sensorService.getCompass(),
            declinationProvider = DeclinationFactory().getDeclinationStrategy(prefs, gps),
            baseDistanceUnits = prefs.baseDistanceUnits,
            isDebug = isDebug(),
            debugElevations = debugElevations,
            isBacktrackOn = { backtrack?.isEnabled() == true },
            stopBacktrack = { backtrack?.disable() }
        )
    }
    val formatService = remember { FormatService.getInstance() }
    val pathService = viewModel.pathService
    val path = viewModel.path
    val waypoints = viewModel.waypoints
    val listState = rememberLazyListState()
    var showPathPointsSheet by remember { mutableStateOf(false) }
    var pathColorPickerTarget by remember { mutableStateOf<Path?>(null) }
    var simplifyRequest by remember { mutableStateOf<SimplifyPathRequest?>(null) }
    val simplifyCommand = remember(host, scope) {
        SimplifyPathCommand(host, scope, pathService) { simplifyRequest = it }
    }

    // The map and its layers are rendering objects, so they live with the composition.
    val layer = remember { ConfigurableGeoJsonLayer(prefs.paths.useFastPathRendering) }
    val locationLayer = remember { ConfigurableGeoJsonLayer(false, layerId = "path_location") }
    val locationColor = MaterialTheme.colorScheme.primary.toArgb()
    val map = remember(density) {
        SharedMapView(density.density, density.fontScale).apply {
            keepScreenOn = true
            setLayers(listOf(layer, locationLayer, ScaleBarLayer(prefs.baseDistanceUnits, formatService)))
        }
    }
    val legend = remember { ColorScaleState() }
    val pointFactory = remember(path?.style?.point) {
        getPointFactory(path?.style?.point ?: PathPointColoringStyle.None)
    }
    val bounds = remember(waypoints) { Geography.getBounds(waypoints.map { it.coordinate }) }
    val converter: IPathPointBeaconConverter = remember(strings) {
        TemporaryPathPointBeaconConverter(strings.get(Res.string.waypoint))
    }
    val beaconNavigator: IBeaconNavigator = remember(appNavigator, navigation) {
        BeaconNavigator(
            getAppService<IBeaconService>(),
            TrailSenseAppNavigation(appNavigator),
            navigation
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
    LaunchedEffect(map, locationColor) {
        snapshotFlow { Triple(viewModel.userLocation, viewModel.userLocationAccuracy, viewModel.userAzimuth) }
            .collect { (location, accuracy, azimuth) ->
                map.userLocation = location
                map.userLocationAccuracy = accuracy
                azimuth?.let { map.userAzimuth = it }
                val features = if (location != Coordinate.zero || accuracy != null) {
                    listOf(
                        GeoJsonFeature.point(location, color = locationColor, opacity = 25, moveWithUserLocation = true, scaleToLocationAccuracy = true),
                        GeoJsonFeature.point(location, color = locationColor, strokeColor = ColorInts.White, strokeWeight = 2f, size = 16f, moveWithUserLocation = true)
                    )
                } else emptyList()
                locationLayer.setData(GeoJsonFeatureCollection(features))
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
            MoveIPathCommand(host, pathService).execute(current)
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
        val coordinate = point.coordinate
        appNavigator.navigate(Route.PlaceBeacon(initialLocation = RouteLocation(
            coordinate.latitude,
            coordinate.longitude,
            point.elevation,
            mapOf("label" to (current.name ?: strings.get(Res.string.waypoint)))
        )))
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
                PathAction.Rename -> strings.get(Res.string.rename)
                PathAction.Keep -> if (current.temporary) strings.get(Res.string.keep_forever) else null
                PathAction.ToggleVisibility -> if (current.style.visible) {
                    strings.get(Res.string.hide)
                } else {
                    strings.get(Res.string.show)
                }

                PathAction.Export -> strings.get(Res.string.export)
                PathAction.Simplify -> strings.get(Res.string.simplify)
                PathAction.ReplaceElevations -> strings.get(Res.string.replace_elevations)
                PathAction.ViewPoints -> strings.get(Res.string.points)
                PathAction.Move -> strings.get(Res.string.move_to)
                else -> null
            }
            name?.let {
                ActionItem(it, TrailIcons.MenuDots) {
                    when (action) {
                        PathAction.Export -> ExportPathCommand(
                            host,
                            scope,
                            SharedGpxIOService(),
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
            effects.chooseAction(current.name ?: strings.get(Res.string.path), null, actionItems)?.action?.invoke()
        }
    }

    fun addPoint() {
        if (viewModel.path == null) return
        scope.launch {
            val job = launch {
                val added = AddPathPointCommand(viewModel.pathId).execute()
                effects.showMessage(strings.get(if (added) Res.string.point_added else Res.string.gps_unavailable))
            }

            Alerts.withCancelableLoading(
                host,
                strings.get(Res.string.loading),
                onCancel = { job.cancel() }) {
                job.join()
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
    } ?: PathOverviewModel.empty(MaterialTheme.colorScheme.primary.toArgb())

    val mapHeight = if (viewModel.isFullscreen) {
        val legendHeight = with(density) { 72.dp.roundToPx() }
        (listState.layoutInfo.viewportSize.height - legendHeight).coerceAtLeast(with(density) { 250.dp.roundToPx() })
    } else {
        with(density) { 250.dp.roundToPx() }
    }

    PathOverviewHost(
        model = model,
        listState = listState,
        onBack = rememberTrailSenseBack(),
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
            title = appString(Res.string.path_color),
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
    map: SharedMapView,
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
        map.setBackgroundColor(mapBackground)
    }
    Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(mapHeight)
                .clip(RoundedCornerShape(TrailSenseShape.groupOuter))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            map.Content(Modifier.fillMaxSize().testTag("path_image"))
            PathIconButton(
                icon = if (isFullscreen) TrailIcons.FullscreenExit else TrailIcons.Recenter,
                contentDescription = appString(Res.string.center),
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
