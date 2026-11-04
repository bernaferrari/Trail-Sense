@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.paths.ui

import androidx.compose.material3.MaterialShapes
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.toShape
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpotlight
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseStat
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseStatGrid
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesBadge
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesKeyValue
import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import android.content.Context
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.luna.time.Throttle
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.trail_sense.shared.extensions.asLiveData
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.MathExtensions.roundPlaces
import com.kylecorry.sol.math.statistics.Statistics
import com.kylecorry.sol.science.geography.Geography
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geology
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
import com.kylecorry.trail_sense.shared.navigation.NavControllerAppNavigation
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.toRelativeDistance
import com.kylecorry.trail_sense.shared.views.ColorScaleContent
import com.kylecorry.trail_sense.shared.views.ColorScaleState
import com.kylecorry.trail_sense.shared.views.ColorPickerDialog
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.sharing.ActionItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import android.os.Bundle
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
import com.kylecorry.trail_sense.tools.paths.domain.factories.IPointDisplayFactory
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.observe
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseHeroCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSheet

@Composable
fun PathOverviewToolScreen(
    host: TrailSenseDestinationHost,
    modifier: Modifier = Modifier
) {
    val controller = remember(host) { PathOverviewController(host) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(controller, lifecycleOwner) {
        controller.onCreate()
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

    controller.Content(modifier = modifier.fillMaxSize())
}

private class PathOverviewController(private val host: TrailSenseDestinationHost) {

    private val views = createPathOverviewViews(host.requireContext())
    private val isViewReady: Boolean
        get() = isCreated

    private val prefs by lazy { UserPreferences(host.requireContext()) }
    private val formatService by lazy { FormatService.getInstance(host.requireContext()) }
    private val throttle = Throttle(20)

    private val sensorService by lazy { SensorService(host.requireContext()) }
    private val gps by lazy { sensorService.getGPS() }
    private val compass by lazy { sensorService.getCompass() }
    private val declinationProvider by lazy {
        DeclinationFactory().getDeclinationStrategy(
            prefs,
            gps
        )
    }
    private var declination = 0f
    private val hikingService = HikingService
    private val pathService by lazy { PathService.getInstance(host.requireContext()) }

    private val chart: PathElevationChart
        get() = views.chart
    private var path: Path? = null
    private var waypoints: List<PathPoint> = emptyList()
    private var pathPointsSheetPoints by mutableStateOf<List<PathPoint>>(emptyList())
    private var showPathPointsSheet by mutableStateOf(false)
    private var pathId: Long = 0L
    private var selectedPointId: Long? = null
    private var calculatedDuration = Duration.ZERO
    private var elevationGain = Distance.meters(0f)
    private var elevationLoss = Distance.meters(0f)
    private var elevationRange: Range<Distance>? = null
    private var slopes: List<Triple<PathPoint, PathPoint, Float>> = emptyList()
    private var difficulty = HikingDifficulty.Easy

    private val scaleBarLayer = ScaleBarLayer()

    private var lastBounds = CoordinateBounds.empty

    // TODO: Eventually move all of this to the path layer (including waypoint rendering)
    private val layer = ConfigurableGeoJsonLayer()
    private val myLocationLayer =
        GeoJsonLayer(MyLocationGeoJsonSource(), MyLocationGeoJsonSource.SOURCE_ID)
    private val paceFactor = 1.75f

    private var isFullscreen = false
    private var pathColorPickerTarget by mutableStateOf<Path?>(null)
    private var isCreated = false

    private val converter: IPathPointBeaconConverter by lazy {
        TemporaryPathPointBeaconConverter(
            host.getString(R.string.waypoint)
        )
    }

    private val navigator by lazy { Navigator.getInstance(host.requireContext()) }

    private val beaconNavigator: IBeaconNavigator by lazy {
        BeaconNavigator(
            BeaconService(host.requireContext()),
            NavControllerAppNavigation(host.findNavController()),
            navigator
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Content(modifier: Modifier = Modifier) {
        val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
        PathOverviewHost(
            views = views,
            onBack = { dispatcher?.onBackPressed() },
            modifier = modifier
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
                    points = pathPointsSheetPoints,
                    formatService = formatService,
                    baseDistanceUnits = prefs.baseDistanceUnits,
                    onCreateBeacon = {
                        createBeacon(it)
                        showPathPointsSheet = false
                    },
                    onDelete = {
                        deleteWaypoint(it)
                    },
                    onNavigate = {
                        navigateToWaypoint(it)
                        showPathPointsSheet = false
                    },
                    onNavigateDirect = {
                        navigateDirectlyToWaypoint(it)
                        showPathPointsSheet = false
                    },
                    onView = {
                        viewWaypoint(it)
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
                        ChangePathColorCommand(
                            host.lifecycleOwner,
                            pathService
                        ).execute(colorPickerPath, it)
                    }
                    pathColorPickerTarget = null
                },
                onDismiss = {
                    pathColorPickerTarget = null
                }
            )
        }
    }

    fun onCreate() {
        if (isCreated) {
            return
        }
        isCreated = true
        pathId = host.destinationArguments?.getLong("path_id") ?: error("Missing path id")
        layer.renderer.configureLineStringRenderer(shouldRenderWithDrawLines = prefs.paths.useFastPathRendering)
        layer.setOnClickListener {
            if (selectedPointId != null) {
                deselectPoint()
                return@setOnClickListener true
            }
            val point = waypoints.firstOrNull { point -> point.id == it.id }
            if (point != null) {
                viewWaypoint(point)
                true
            } else {
                false
            }
        }
        setupView()
    }

    fun onResume() {
        if (!isViewReady) {
            return
        }
        // Populate the last known location
        views.pathImage.userLocation = gps.location
        views.pathImage.userLocationAccuracy =
            gps.horizontalAccuracy?.let { Distance.meters(it) }

        views.pathImage.start()
    }

    fun onPause() {
        if (!isViewReady) {
            return
        }
        views.pathImage.stop()
    }

    fun onDestroy() {
        onPause()
        isCreated = false
    }

    private fun setupView() {
        views.pathImage.isInteractive = true

        views.pathMapFullscreenToggle.onClick = {
            isFullscreen = !isFullscreen
            views.pathMapHolder.height = if (isFullscreen) {
                val legendHeight = Resources.dp(host.requireContext(), 72f).toInt()
                views.root.height - legendHeight
            } else {
                Resources.dp(host.requireContext(), 250f).toInt()
            }
            views.pathMapFullscreenToggle.setIcon(if (isFullscreen) TrailIcons.FullscreenExit else TrailIcons.Recenter)
            val timer = CoroutineTimer {
                if (isViewReady) {
                    views.root.scrollToTop()
                    views.pathImage.fitIntoView(lastBounds)
                }
            }
            timer.once(Duration.ofMillis(30))
        }

        views.pathActions.onAddPointClick = {
            host.inBackground {
                var wasSuccessful = false
                val job = launch {
                    AddPathPointCommand(host.requireContext(), pathId).execute()
                    wasSuccessful = true
                }

                Alerts.withCancelableLoading(
                    host.requireContext(),
                    host.getString(R.string.loading),
                    onCancel = { job.cancel() }) {
                    job.join()
                    if (wasSuccessful) {
                        host.toast(host.getString(R.string.point_added))
                    }
                }
            }
        }

        views.pathActions.onFollowToStartClick = {
            followPath(PathFollow.Direction(PathDirection.ToStart))
        }
        views.pathActions.onFollowToEndClick = {
            followPath(PathFollow.Direction(PathDirection.ToEnd))
        }

        views.pathTitle.onMenuClick = {
            showPathMenu()
        }

        chart.setOnPointClickListener {
            viewWaypoint(it)
        }

        views.pathElevationMin.onClick = {
            val point = waypoints.filter { it.elevation != null }.minByOrNull { it.elevation!! }
            point?.let { viewWaypoint(it) }
        }

        views.pathElevationMax.onClick = {
            val point = waypoints.filter { it.elevation != null }.maxByOrNull { it.elevation!! }
            point?.let { viewWaypoint(it) }
        }

        pathService.getLivePath(pathId).observe(host.lifecycleOwner) {
            path = it
            views.root.post {
                updateParent()
                updateElevationPlot()
                updatePointStyleLegend()
                updatePathMap()
                onPathChanged()
            }
        }

        pathService.getWaypointsLive(pathId).observe(host.lifecycleOwner) {
            onWaypointsChanged(it)
        }

        gps.asLiveData().observe(host.lifecycleOwner) {
            updateDeclination()
            views.pathImage.userLocation = gps.location
            views.pathImage.userLocationAccuracy =
                gps.horizontalAccuracy?.let { Distance.meters(it) }
            onPathChanged()
        }

        compass.asLiveData().observe(host.lifecycleOwner) {
            views.pathImage.userAzimuth = compass.bearing
        }

        views.pathImage.setLayers(
            listOf(layer, myLocationLayer, scaleBarLayer)
        )

        views.pathStyleControls.onLineStyleClick = lineStyleClick@{
            val path = path ?: return@lineStyleClick
            val command = ChangePathLineStyleCommand(host.requireContext(), host.lifecycleOwner)
            command.execute(path)
        }

        views.pathStyleControls.onColorClick = colorClick@{
            val path = path ?: return@colorClick
            pathColorPickerTarget = path
        }

        views.pathStyleControls.onPointStyleClick = pointStyleClick@{
            val path = path ?: return@pointStyleClick
            val command = ChangePointStyleCommand(host.requireContext(), host.lifecycleOwner)
            command.execute(path)
        }
    }

    private fun onWaypointsChanged(waypoints: List<PathPoint>) {
        host.inBackground {
            onDefault {
                this@PathOverviewController.waypoints =
                    hikingService.correctElevations(waypoints.sortedBy { it.id }).reversed()
            }
            onIO {
                DebugPathElevationsCommand(
                    host.requireContext(),
                    waypoints,
                    this@PathOverviewController.waypoints
                ).execute()
            }
            onMain {
                val selected = selectedPointId
                if (selected != null && waypoints.find { it.id == selected } == null) {
                    deselectPoint()
                }
                pathPointsSheetPoints = this@PathOverviewController.waypoints
                updateElevationOverview()
                updateHikingStats()
                updatePathMap()
                updatePointStyleLegend()
                onPathChanged()
            }
        }
    }

    private fun updateParent() {
        val path = path ?: return
        host.inBackground {
            val parent = onIO { pathService.getGroup(path.parentId) }
            if (isViewReady) {
                views.pathTitle.subtitle = parent?.name ?: host.getString(R.string.no_group)
            }
        }
    }

    private fun updateElevationPlot() {
        chart.plot(
            waypoints.reversed(),
            path?.style?.color ?: prefs.paths.defaultPathColor.color
        )
    }

    private suspend fun updateHikingStats() = onDefault {
        val reversed = waypoints.reversed()
        difficulty = hikingService.getHikingDifficulty(reversed)
        calculatedDuration =
            hikingService.getHikingDuration(reversed, paceFactor)
    }

    private fun movePath() {
        val path = path ?: return
        val command = MoveIPathCommand(host.requireContext(), pathService)
        host.inBackground {
            command.execute(path)
        }
    }

    private suspend fun updateElevationOverview() {
        onDefault {
            val path = waypoints.reversed()

            val gainLoss = hikingService.getElevationLossGain(path)

            val units = prefs.baseDistanceUnits
            elevationGain = gainLoss.second.convertTo(units)
            elevationLoss = gainLoss.first.convertTo(units)
            elevationRange =
                path.mapNotNull { it.elevation?.let { Distance.meters(it).convertTo(units) } }
                    .range()
            slopes = hikingService.getSlopes(path)
            slopes.forEach {
                it.first.slope = it.third
            }
            val first = slopes.lastOrNull()
            first?.first?.slope = first.third
        }
        onMain {
            updateElevationPlot()
        }
    }

    private fun showPathMenu() {
        val path = path ?: return
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
                PathAction.Rename -> host.getString(R.string.rename)
                PathAction.Keep -> if (path.temporary) host.getString(R.string.keep_forever) else null
                PathAction.ToggleVisibility -> if (path.style.visible) {
                    host.getString(R.string.hide)
                } else {
                    host.getString(R.string.show)
                }
                PathAction.Export -> host.getString(R.string.export)
                PathAction.Simplify -> host.getString(R.string.simplify)
                PathAction.ViewPoints -> host.getString(R.string.points)
                PathAction.Move -> host.getString(R.string.move_to)
                else -> null
            }
            name?.let {
                ActionItem(it, TrailIcons.MenuDots) {
                    when (action) {
                        PathAction.Export -> exportPath(path)
                        PathAction.Rename -> renamePath(path)
                        PathAction.Keep -> keepPath(path)
                        PathAction.ToggleVisibility -> togglePathVisibility(path)
                        PathAction.Simplify -> simplifyPath(path)
                        PathAction.ViewPoints -> viewPoints()
                        PathAction.Move -> movePath()
                        else -> {
                            // Do nothing
                        }
                    }
                }
            }
        }

        host.showActionSheet(path.name ?: host.getString(R.string.path), null, actionItems) {
            when (it) {
                null -> Unit
                else -> it.action()
            }
        }
    }

    private fun simplifyPath(path: Path) {
        val command = SimplifyPathCommand(host.requireContext(), host.lifecycleOwner, pathService)
        command.execute(path)
    }

    private fun exportPath(path: Path) {
        val command = ExportPathCommand(
            host.requireContext(),
            host.lifecycleOwner,
            IOFactory().createGpxService(
                host.requireContext(),
                host.intentResultRetriever
            ),
            pathService
        )
        command.execute(path)
    }

    private fun togglePathVisibility(path: Path) {
        val command = TogglePathVisibilityCommand(host.requireContext(), host.lifecycleOwner, pathService)
        command.execute(path)
    }

    private fun renamePath(path: Path) {
        val command = RenamePathCommand(host.requireContext(), host.lifecycleOwner, pathService)
        command.execute(path)
    }

    private fun keepPath(path: Path) {
        val command = KeepPathCommand(host.requireContext(), host.lifecycleOwner, pathService)
        command.execute(path)
    }

    private fun updatePathMap() {
        if (!isViewReady) {
            return
        }
        lastBounds = Geography.getBounds(waypoints.map { it.coordinate })
        views.pathImage.fitIntoView(lastBounds)
        updateMapLayer()
    }

    private fun onPathChanged() {
        val path = path ?: return

        if (!isViewReady || throttle.isThrottled()) {
            return
        }

        views.pathStyleControls.lineStyle = listOf(
            host.getString(R.string.solid),
            host.getString(R.string.dotted),
            host.getString(R.string.arrow),
            host.getString(R.string.dashed),
            host.getString(R.string.square),
            host.getString(R.string.diamond),
            host.getString(R.string.cross)
        )[path.style.line.ordinal]

        val distance =
            path.metadata.distance.convertTo(prefs.baseDistanceUnits).toRelativeDistance()

        val start = path.metadata.duration?.start
        val end = path.metadata.duration?.end


        views.pathTitle.title = PathNameFactory(host.requireContext()).getName(path)

        val duration = if (start != null && end != null && Duration.between(
                start,
                end
            ) > Duration.ofMinutes(1)
        ) {
            Duration.between(start, end)
        } else {
            calculatedDuration
        }

        views.pathDuration.title = formatService.formatDuration(duration, false)
        views.pathWaypoints.title = path.metadata.waypoints.toString()

        // Elevations
        views.pathElevationGain.title = formatService.formatDistance(
            elevationGain,
            Units.getDecimalPlaces(elevationGain.units),
            false
        )
        views.pathElevationLoss.title = formatService.formatDistance(
            elevationLoss,
            Units.getDecimalPlaces(elevationLoss.units),
            false
        )

        val elevationRange = elevationRange
        views.pathElevationMin.isVisible = elevationRange != null
        views.pathElevationMax.isVisible = elevationRange != null

        elevationRange?.let {
            views.pathElevationMin.title = formatService.formatDistance(
                it.start,
                Units.getDecimalPlaces(it.start.units),
                false
            )

            views.pathElevationMax.title = formatService.formatDistance(
                it.end,
                Units.getDecimalPlaces(it.end.units),
                false
            )
        }

        views.pathDifficulty.title = formatService.formatHikingDifficulty(difficulty)

        views.pathDistance.title =
            formatService.formatDistance(
                distance,
                Units.getDecimalPlaces(distance.units),
                false
            )

        views.pathStyleControls.pathColor = path.style.color

        updateMapLayer()

        updateDebugStats()
    }

    private fun updateMapLayer() {
        val path = path ?: return
        val waypointFeatures = waypoints.asBeacons(
            host.requireContext(),
            path.style.point,
            selectedPointId
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
            path.id,
            path.name,
            lineStyle = path.style.line,
            color = path.style.color
        )

        layer.setData(GeoJsonFeatureCollection(waypointFeatures + pathFeature))
    }

    private fun updateDeclination() {
        host.inBackground {
            onIO {
                declination = declinationProvider.getDeclination()
                compass.declination = declination
            }
        }
    }

    private fun deselectPoint() {
        selectedPointId = null
        views.pathSelectedPoint.isVisible = false
        views.pathSelectedPoint.point = null
        chart.removeHighlight()
        updateMapLayer()
    }

    private fun updatePointStyleLegend() {

        val path = path ?: return

        val factory = getPointFactory()

        views.pathStyleControls.pointStyle = listOf(
            host.getString(R.string.none),
            host.getString(R.string.cell_signal),
            host.getString(R.string.elevation),
            host.getString(R.string.time),
            host.getString(R.string.path_slope)
        )[path.style.point.ordinal]

        views.pathLegend.colorScale = factory.createColorScale(waypoints)
        views.pathLegend.labels = factory.createLabelMap(waypoints)
        views.pathLegend.isVisible = path.style.point != PathPointColoringStyle.None
    }

    // Waypoints
    private fun viewPoints() {
        views.root.scrollToTop()
        pathPointsSheetPoints = waypoints
        showPathPointsSheet = true
    }

    private fun viewWaypoint(point: PathPoint) {
        selectedPointId = if (selectedPointId == point.id) {
            null
        } else {
            point.id
        }

        if (selectedPointId != null) {
            views.pathSelectedPoint.isVisible = true
            views.pathSelectedPoint.point = point
            views.pathSelectedPoint.formatService = formatService
            views.pathSelectedPoint.baseDistanceUnits = prefs.baseDistanceUnits
            views.pathSelectedPoint.onCreateBeacon = { createBeacon(it) }
            views.pathSelectedPoint.onDelete = { deleteWaypoint(it) }
            views.pathSelectedPoint.onNavigate = { navigateToWaypoint(it) }
            views.pathSelectedPoint.onNavigateDirect = { navigateDirectlyToWaypoint(it) }
            chart.highlight(point)
        } else {
            deselectPoint()
        }

        onPathChanged()
    }

    private fun navigateToWaypoint(point: PathPoint) {
        followPath(PathFollow.ToPoint(point.id))
    }

    private fun followPath(follow: PathFollow) {
        val selectedPath = path ?: return
        host.lifecycleOwner.lifecycleScope.launch {
            navigator.navigateAlongPath(selectedPath, follow)
            onMain {
                host.findNavController().navigate(
                    R.id.action_navigation,
                    Bundle().apply {
                        putBoolean(BeaconNavigator.ARG_NAVIGATION_STARTED, true)
                    }
                )
            }
        }
    }

    private fun navigateDirectlyToWaypoint(point: PathPoint) {
        val selectedPath = path ?: return
        val command = NavigateToPointCommand(
            host.lifecycleOwner,
            converter,
            beaconNavigator
        )
        tryOrNothing {
            command.execute(selectedPath, point)
        }
    }

    private fun deleteWaypoint(point: PathPoint) {
        val path = path ?: return
        val command = DeletePointCommand(host.requireContext(), host.lifecycleOwner)
        command.execute(path, point)
    }

    private fun createBeacon(point: PathPoint) {
        val path = path ?: return
        val command = CreateBeaconFromPointCommand(host.requireContext())
        command.execute(path, point)
    }

    private fun getPointFactory(): IPointDisplayFactory {
        return getPointFactory(host.requireContext(), path?.style?.point ?: PathPointColoringStyle.None)
    }

    private fun updateDebugStats() {
        if (!isDebug()) {
            return
        }

        views.pathsTiming.isVisible = true

        host.inBackground {
            onDefault {
                val readings = waypoints
                    .filter { it.time != null }
                    .sortedBy { it.time }
                    .zipWithNext { a, b -> Duration.between(a.time, b.time).seconds / 60f }

                if (readings.isEmpty()) {
                    return@onDefault
                }

                val mean = Statistics.mean(readings).roundPlaces(2)
                val stdev = Statistics.stdev(readings, mean = mean).roundPlaces(2)
                val max = readings.maxOrNull()?.roundPlaces(2) ?: 0f
                val median = Statistics.median(readings).roundPlaces(2)
                val quantile75 = Statistics.quantile(readings, 0.75f).roundPlaces(2)
                val quantile90 = Statistics.quantile(readings, 0.9f).roundPlaces(2)
                onMain {
                    @Suppress("SetTextI18n")
                    views.pathsTiming.text = "Mean: $mean\n" +
                            "Stdev: $stdev\n" +
                            "Max: $max\n" +
                            "Median: $median\n" +
                            "75th: $quantile75\n" +
                            "90th: $quantile90"
                }
            }
        }
    }
}

private data class PathOverviewViews(
    val root: PathScrollState,
    val pathTitle: PathTitleState,
    val pathStyleControls: PathStyleControlsView,
    val pathMapHolder: PathMapHolderState,
    val pathImage: MapView,
    val pathMapFullscreenToggle: PathIconButton,
    val pathSelectedPoint: PathSelectedPointView,
    val pathLegend: ColorScaleState,
    val pathDuration: PathDataPointState,
    val pathDistance: PathDataPointState,
    val pathWaypoints: PathDataPointState,
    val pathDifficulty: PathDataPointState,
    val pathElevationGain: PathDataPointState,
    val pathElevationLoss: PathDataPointState,
    val pathElevationMin: PathDataPointState,
    val pathElevationMax: PathDataPointState,
    val chart: PathElevationChart,
    val pathActions: PathActionsView,
    val pathsTiming: PathTimingView
)

/** The page's scroll position, so the map can be brought to the top when it goes full screen. */
private class PathScrollState {
    val listState = LazyListState()

    /** The height of the scrolling area, in pixels. */
    val height: Int
        get() = listState.layoutInfo.viewportSize.height

    fun scrollToTop() {
        CoroutineScope(Dispatchers.Main).launch {
            listState.animateScrollToItem(0)
        }
    }

    fun post(action: () -> Unit): Boolean {
        action()
        return true
    }
}

private class PathMapHolderState(initialHeight: Int) {
    var height by mutableIntStateOf(initialHeight)
}

private class PathTitleState {
    var title by mutableStateOf("")
    var subtitle by mutableStateOf("")
    var onMenuClick by mutableStateOf<(() -> Unit)?>(null)
}

private class PathDataPointState(
    initialDescription: String,
    val icon: ImageVector
) {
    var title by mutableStateOf("")
    var description by mutableStateOf(initialDescription)
    var onClick by mutableStateOf<(() -> Unit)?>(null)
    var isVisible by mutableStateOf(true)
}

/**
 * The path detail page: the map preview stays on top (it can grow to fill the screen), then the one fact you came for
 * (how far, how long) in a hero card, then the elevation profile, how to follow the path, and how it is drawn.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PathOverviewHost(
    views: PathOverviewViews,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val chartHeight = with(density) {
        context.resources.getDimensionPixelSize(R.dimen.chart_height).toDp()
    }
    val selectedPoint = views.pathSelectedPoint

    TrailSensePage(
        title = views.pathTitle.title,
        subtitle = views.pathTitle.subtitle.ifBlank { null },
        onBack = onBack,
        backLabel = stringResource(R.string.back),
        state = views.root.listState,
        modifier = modifier,
        actions = {
            IconButton(
                onClick = { views.pathTitle.onMenuClick?.invoke() },
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
                onClick = views.pathActions.onAddPointClick,
                icon = { Icon(rememberVectorPainter(TrailIcons.Add), contentDescription = null) },
                text = { Text(stringResource(R.string.point)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_point_btn")
            )
        }
    ) {
        item(key = "map") {
            PathMapPreview(views)
        }

        if (selectedPoint.isVisible && selectedPoint.point != null) {
            item(key = "selected_point") {
                Box(Modifier.animateItem()) {
                    selectedPoint.Content(Modifier.fillMaxWidth())
                }
            }
        }

        item(key = "hero") {
            PathSummaryHero(views)
        }

        trailSenseSection(key = "elevation", title = context.getString(R.string.elevation)) {
            row { _ ->
                TrailSenseStatGrid(
                    stats = listOfNotNull(
                        views.pathElevationGain.takeIf { it.isVisible }?.let {
                            TrailSenseStat(
                                label = it.description,
                                value = it.title.ifBlank { "–" },
                                tone = TrailSenseTone.Tertiary,
                                icon = it.icon
                            )
                        },
                        views.pathElevationLoss.takeIf { it.isVisible }?.let {
                            TrailSenseStat(
                                label = it.description,
                                value = it.title.ifBlank { "–" },
                                tone = TrailSenseTone.Primary,
                                icon = it.icon
                            )
                        }
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
                    views.chart.Content(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(chartHeight)
                    )
                }
            }
            if (views.pathElevationMin.isVisible) {
                row { position -> PathStatRow(views.pathElevationMin, position) }
            }
            if (views.pathElevationMax.isVisible) {
                row { position -> PathStatRow(views.pathElevationMax, position) }
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
                        onClick = views.pathActions.onFollowToStartClick,
                        modifier = Modifier.weight(1f)
                    )
                    PathNavigateButton(
                        label = context.getString(R.string.path_direction_to_end),
                        rotation = 0f,
                        first = false,
                        onClick = views.pathActions.onFollowToEndClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        trailSenseSection(key = "appearance", title = context.getString(R.string.appearance)) {
            val style = views.pathStyleControls
            row { position ->
                TrailSenseListItem(
                    title = context.getString(R.string.color),
                    position = position,
                    onClick = style.onColorClick,
                    trailing = {
                        Surface(
                            modifier = Modifier
                                .padding(end = TrailSenseSpacing.sm)
                                .size(32.dp),
                            shape = MaterialShapes.Cookie9Sided.toShape(),
                            color = ComposeColor(style.pathColor)
                        ) {}
                    },
                    modifier = Modifier.testTag("path_color")
                )
            }
            row { position ->
                TrailSenseListItem(
                    title = context.getString(R.string.line_style),
                    subtitle = style.lineStyle,
                    position = position,
                    onClick = style.onLineStyleClick,
                    trailing = { PathChevron() }
                )
            }
            row { position ->
                TrailSenseListItem(
                    title = context.getString(R.string.point_style),
                    subtitle = style.pointStyle,
                    position = position,
                    onClick = style.onPointStyleClick,
                    trailing = { PathChevron() }
                )
            }
        }

        if (views.pathsTiming.isVisible) {
            item(key = "timing") {
                TrailSenseGroupCard(position = TrailSenseGroupPosition.Single) {
                    views.pathsTiming.Content(Modifier.fillMaxWidth())
                }
            }
        }

        // Room for the floating button over the last row.
        item(key = "fab_spacer") { Spacer(Modifier.height(56.dp)) }
    }
}

/** The map preview, with its color legend tucked under it when points are colored. */
@Composable
private fun PathMapPreview(views: PathOverviewViews) {
    val density = LocalDensity.current
    val mapHeight by animateDpAsState(
        targetValue = with(density) { views.pathMapHolder.height.toDp() },
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "pathMapHeight"
    )
    val mapBackground = MaterialTheme.colorScheme.surfaceContainerHigh.toArgb()
    SideEffect {
        views.pathImage.backgroundTintList = ColorStateList.valueOf(mapBackground)
    }
    Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(mapHeight)
                .clip(RoundedCornerShape(TrailSenseShape.groupOuter))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            views.pathImage.Content(Modifier.fillMaxSize())
            views.pathMapFullscreenToggle.Content(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(TrailSenseSpacing.md)
            )
        }
        AnimatedVisibility(
            visible = views.pathLegend.isVisible,
            enter = expandVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                    fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
            exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                    fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec())
        ) {
            ColorScaleContent(
                state = views.pathLegend,
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
private fun PathSummaryHero(views: PathOverviewViews) {
    val distance = views.pathDistance
    PlacesKeyValue(
        value = distance.title.ifBlank { stringResource(R.string.dash) },
        verdict = distance.description.ifBlank { null },
        modifier = Modifier.animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec()),
        below = {
            val stats = listOf(views.pathDuration, views.pathWaypoints, views.pathDifficulty)
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
    state: PathDataPointState,
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

/** A labelled value inside a group card, with its icon in the family's small well. */
@Composable
private fun PathStat(state: PathDataPointState, modifier: Modifier = Modifier) {
    if (!state.isVisible) return
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(TrailSenseShape.iconContainer),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(TrailSenseSize.compactIconContainer)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = rememberVectorPainter(state.icon),
                    contentDescription = null,
                    modifier = Modifier.size(TrailSenseSize.compactIcon)
                )
            }
        }
        Spacer(Modifier.width(TrailSenseSpacing.md))
        Column {
            Text(
                text = state.title.ifBlank { stringResource(R.string.dash) },
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = state.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** A value as a row: the label on the left, the value trailing; tapping it selects that point. */
@Composable
private fun PathStatRow(state: PathDataPointState, position: TrailSenseGroupPosition) {
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

private fun createPathOverviewViews(context: Context): PathOverviewViews {
    val root = PathScrollState()

    val title = PathTitleState()

    val styleControls = PathStyleControlsView(context)
    val mapHolder = PathMapHolderState(Resources.dp(context, 250f).toInt())

    val pathImage = MapView(context).apply {
        id = R.id.path_image
        backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorSecondary))
        keepScreenOn = true
    }

    val fullscreenToggle = PathIconButton(TrailIcons.Recenter, R.string.center)

    val selectedPoint = PathSelectedPointView().apply {
        isVisible = false
    }

    val legend = ColorScaleState()
    val duration = createDataPoint(context, TrailIcons.ToolClock, R.string.duration)
    val distance = createDataPoint(context, TrailIcons.Ruler, R.string.distance)
    val points = createDataPoint(context, TrailIcons.Location, R.string.points)
    val difficulty = createDataPoint(context, TrailIcons.Steps, R.string.difficulty)
    val gain = createDataPoint(context, TrailIcons.ElevationGain, R.string.ascent)
    val loss = createDataPoint(context, TrailIcons.ElevationLoss, R.string.descent)
    val min = createDataPoint(context, TrailIcons.ElevationLoss, R.string.lowest_point_elevation)
    val max = createDataPoint(context, TrailIcons.ElevationGain, R.string.highest_point_elevation)

    val chart = PathElevationChart(context)

    val pathActions = PathActionsView()

    val timing = PathTimingView().apply {
        isVisible = false
    }

    return PathOverviewViews(
        root = root,
        pathTitle = title,
        pathStyleControls = styleControls,
        pathMapHolder = mapHolder,
        pathImage = pathImage,
        pathMapFullscreenToggle = fullscreenToggle,
        pathSelectedPoint = selectedPoint,
        pathLegend = legend,
        pathDuration = duration,
        pathDistance = distance,
        pathWaypoints = points,
        pathDifficulty = difficulty,
        pathElevationGain = gain,
        pathElevationLoss = loss,
        pathElevationMin = min,
        pathElevationMax = max,
        chart = chart,
        pathActions = pathActions,
        pathsTiming = timing
    )
}

private class PathStyleControlsView(context: Context) {
    var pathColor by mutableIntStateOf(ContextCompat.getColor(context, R.color.blue))
    var lineStyle by mutableStateOf("")
    var pointStyle by mutableStateOf("")
    var onColorClick by mutableStateOf({})
    var onLineStyleClick by mutableStateOf({})
    var onPointStyleClick by mutableStateOf({})
}

private class PathSelectedPointView {
    var point by mutableStateOf<PathPoint?>(null)
    var formatService by mutableStateOf<FormatService?>(null)
    var baseDistanceUnits by mutableStateOf<DistanceUnits?>(null)
    var onCreateBeacon by mutableStateOf<(PathPoint) -> Unit>({})
    var onDelete by mutableStateOf<(PathPoint) -> Unit>({})
    var onNavigate by mutableStateOf<(PathPoint) -> Unit>({})
    var onNavigateDirect by mutableStateOf<(PathPoint) -> Unit>({})
    var isVisible by mutableStateOf(true)

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        if (!isVisible) {
            return
        }
        val point = point ?: return
        val formatService = formatService ?: return
        val baseDistanceUnits = baseDistanceUnits ?: return

        PathPointRow(
            point = point,
            formatService = formatService,
            baseDistanceUnits = baseDistanceUnits,
            onCreateBeacon = onCreateBeacon,
            onDelete = onDelete,
            onNavigate = onNavigate,
            onNavigateDirect = onNavigateDirect,
            onView = {},
            position = TrailSenseGroupPosition.Single,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            modifier = modifier
        )
    }
}

private class PathTimingView {
    var text by mutableStateOf("")
    var isVisible by mutableStateOf(true)

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        if (!isVisible) {
            return
        }
        Text(
            text = text,
            modifier = modifier,
            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private class PathActionsView {
    var onFollowToStartClick by mutableStateOf({})
    var onFollowToEndClick by mutableStateOf({})
    var onAddPointClick by mutableStateOf({})
}

private class PathIconButton(
    icon: ImageVector,
    private val contentDescription: Int
) {
    private var iconVector by mutableStateOf(icon)
    var onClick by mutableStateOf<(() -> Unit)?>(null)

    fun setIcon(icon: ImageVector) {
        iconVector = icon
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        FilledTonalIconButton(
            onClick = { onClick?.invoke() },
            modifier = modifier.size(TrailSenseSize.minimumTouchTarget),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(
                painter = rememberVectorPainter(iconVector),
                contentDescription = stringResource(contentDescription),
                modifier = Modifier.size(TrailSenseSize.leadingIcon)
            )
        }
    }
}

private fun createDataPoint(
    context: Context,
    icon: ImageVector,
    description: Int
): PathDataPointState {
    return PathDataPointState(
        initialDescription = context.getString(description),
        icon = icon
    )
}

@Preview(showBackground = true)
@Composable
private fun PathOverviewHostPreview() {
    TrailSenseTheme {
        PathOverviewHost(
            views = createPathOverviewViews(LocalContext.current),
            onBack = {}
        )
    }
}
