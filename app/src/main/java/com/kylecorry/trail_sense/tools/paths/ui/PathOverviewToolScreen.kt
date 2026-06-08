package com.kylecorry.trail_sense.tools.paths.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.Gravity
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.luna.time.Throttle
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.andromeda.pickers.Pickers
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
import com.kylecorry.trail_sense.shared.views.DataPointContent
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
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
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService
import com.kylecorry.trail_sense.tools.paths.domain.point_finder.NearestPathLineNavigator
import com.kylecorry.trail_sense.tools.paths.domain.point_finder.NearestPathPointNavigator
import com.kylecorry.trail_sense.tools.paths.infrastructure.commands.BacktrackCommand
import com.kylecorry.trail_sense.tools.paths.infrastructure.persistence.PathService
import com.kylecorry.trail_sense.tools.paths.ui.commands.ChangePathColorCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.ChangePathLineStyleCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.ChangePointStyleCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.CreateBeaconFromPointCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.DeletePointCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.ExportPathCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.KeepPathCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.MoveIPathCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.NavigateToPathCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.NavigateToPointCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.RenamePathCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.SimplifyPathCommand
import com.kylecorry.trail_sense.tools.paths.ui.commands.TogglePathVisibilityCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration

@Composable
fun PathOverviewToolScreen(
    fragment: Fragment,
    modifier: Modifier = Modifier
) {
    val controller = remember(fragment) { PathOverviewController(fragment) }
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

private class PathOverviewController(private val fragment: Fragment) {

    private val views = createPathOverviewViews(fragment.requireContext())
    private val isViewReady: Boolean
        get() = isCreated

    private val prefs by lazy { UserPreferences(fragment.requireContext()) }
    private val formatService by lazy { FormatService.getInstance(fragment.requireContext()) }
    private val throttle = Throttle(20)

    private val sensorService by lazy { SensorService(fragment.requireContext()) }
    private val gps by lazy { sensorService.getGPS() }
    private val compass by lazy { sensorService.getCompass() }
    private val declinationProvider by lazy {
        DeclinationFactory().getDeclinationStrategy(
            prefs,
            gps
        )
    }
    private var declination = 0f
    private val hikingService = HikingService()
    private val pathService by lazy { PathService.getInstance(fragment.requireContext()) }

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
            fragment.getString(R.string.waypoint)
        )
    }

    private val beaconNavigator: IBeaconNavigator by lazy {
        BeaconNavigator(
            BeaconService(fragment.requireContext()),
            NavControllerAppNavigation(fragment.findNavController())
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Content(modifier: Modifier = Modifier) {
        PathOverviewHost(
            views = views,
            modifier = modifier
        )

        if (showPathPointsSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showPathPointsSheet = false
                }
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
                            fragment,
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
        pathId = fragment.requireArguments().getLong("path_id")
        layer.renderer.configureLineStringRenderer(shouldRenderWithDrawLines = prefs.navigation.useFastPathRendering)
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

        views.pathImage.setOnTouchListener { _, event ->
            views.root.isScrollable = event.action == MotionEvent.ACTION_UP
            false
        }

        views.pathMapFullscreenToggle.onClick = {
            isFullscreen = !isFullscreen
            views.pathMapHolder.height = if (isFullscreen) {
                val legendHeight = Resources.dp(fragment.requireContext(), 72f).toInt()
                views.root.height - legendHeight
            } else {
                Resources.dp(fragment.requireContext(), 250f).toInt()
            }
            views.pathMapFullscreenToggle.setImageResource(if (isFullscreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_recenter)
            val timer = CoroutineTimer {
                if (isViewReady) {
                    views.root.scrollTo(0, views.pathMapHolder.top)
                    views.pathImage.fitIntoView(lastBounds)
                }
            }
            timer.once(Duration.ofMillis(30))
        }

        views.pathActions.onAddPointClick = {
            fragment.inBackground {
                var wasSuccessful = false
                val job = launch {
                    BacktrackCommand(fragment.requireContext(), pathId).execute()
                    wasSuccessful = true
                }

                Alerts.withCancelableLoading(
                    fragment.requireContext(),
                    fragment.getString(R.string.loading),
                    onCancel = { job.cancel() }) {
                    job.join()
                    if (wasSuccessful) {
                        fragment.toast(fragment.getString(R.string.point_added))
                    }
                }
            }
        }

        views.pathActions.onNavigateClick = {
            navigateToNearestPathPoint()
        }

        views.pathTitle.onMenuClick = {
            showPathMenu()
        }

        views.pathTitle.onSubtitleClick = {
            movePath()
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

        fragment.observe(pathService.getLivePath(pathId)) {
            path = it
            views.root.post {
                updateParent()
                updateElevationPlot()
                updatePointStyleLegend()
                updatePathMap()
                onPathChanged()
            }
        }

        fragment.observe(pathService.getWaypointsLive(pathId)) {
            onWaypointsChanged(it)
        }

        fragment.observe(gps) {
            updateDeclination()
            views.pathImage.userLocation = gps.location
            views.pathImage.userLocationAccuracy =
                gps.horizontalAccuracy?.let { Distance.meters(it) }
            onPathChanged()
        }

        fragment.observe(compass) {
            views.pathImage.userAzimuth = compass.bearing
        }

        views.pathImage.setLayers(
            listOf(layer, myLocationLayer, scaleBarLayer)
        )

        views.pathStyleControls.onLineStyleClick = lineStyleClick@{
            val path = path ?: return@lineStyleClick
            val command = ChangePathLineStyleCommand(fragment.requireContext(), fragment)
            command.execute(path)
        }

        views.pathStyleControls.onColorClick = colorClick@{
            val path = path ?: return@colorClick
            pathColorPickerTarget = path
        }

        views.pathStyleControls.onPointStyleClick = pointStyleClick@{
            val path = path ?: return@pointStyleClick
            val command = ChangePointStyleCommand(fragment.requireContext(), fragment)
            command.execute(path)
        }
    }

    private fun onWaypointsChanged(waypoints: List<PathPoint>) {
        fragment.inBackground {
            onDefault {
                this@PathOverviewController.waypoints =
                    hikingService.correctElevations(waypoints.sortedBy { it.id }).reversed()
            }
            onIO {
                DebugPathElevationsCommand(
                    fragment.requireContext(),
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
        fragment.inBackground {
            val parent = onIO { pathService.getGroup(path.parentId) }
            if (isViewReady) {
                views.pathTitle.subtitle = parent?.name ?: fragment.getString(R.string.no_group)
            }
        }
    }

    private fun updateElevationPlot() {
        chart.plot(
            waypoints.reversed(),
            path?.style?.color ?: prefs.navigation.defaultPathColor.color
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
        val command = MoveIPathCommand(fragment.requireContext(), pathService)
        fragment.inBackground {
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
            PathAction.ViewPoints
        )

        Pickers.menu(
            fragment.requireView(), listOf(
                fragment.getString(R.string.rename),
                if (path.temporary) fragment.getString(R.string.keep_forever) else null,
                if (path.style.visible) fragment.getString(R.string.hide) else fragment.getString(
                    R.string.show
                ),
                fragment.getString(R.string.export),
                fragment.getString(R.string.simplify),
                fragment.getString(R.string.points)
            )
        ) {
            when (actions[it]) {
                PathAction.Export -> exportPath(path)
                PathAction.Rename -> renamePath(path)
                PathAction.Keep -> keepPath(path)
                PathAction.ToggleVisibility -> togglePathVisibility(path)
                PathAction.Simplify -> simplifyPath(path)
                PathAction.ViewPoints -> viewPoints()
                else -> {
                    // Do nothing
                }
            }
            true
        }
    }

    private fun simplifyPath(path: Path) {
        val command = SimplifyPathCommand(fragment.requireContext(), fragment, pathService)
        command.execute(path)
    }

    private fun exportPath(path: Path) {
        val command = ExportPathCommand(
            fragment.requireContext(),
            fragment,
            IOFactory().createGpxService(
                fragment.requireContext(),
                fragment.requireActivity() as IntentResultRetriever
            ),
            pathService
        )
        command.execute(path)
    }

    private fun togglePathVisibility(path: Path) {
        val command = TogglePathVisibilityCommand(fragment.requireContext(), fragment, pathService)
        command.execute(path)
    }

    private fun renamePath(path: Path) {
        val command = RenamePathCommand(fragment.requireContext(), fragment, pathService)
        command.execute(path)
    }

    private fun keepPath(path: Path) {
        val command = KeepPathCommand(fragment.requireContext(), fragment, pathService)
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
            fragment.getString(R.string.solid),
            fragment.getString(R.string.dotted),
            fragment.getString(R.string.arrow),
            fragment.getString(R.string.dashed),
            fragment.getString(R.string.square),
            fragment.getString(R.string.diamond),
            fragment.getString(R.string.cross)
        )[path.style.line.ordinal]

        val distance =
            path.metadata.distance.convertTo(prefs.baseDistanceUnits).toRelativeDistance()

        val start = path.metadata.duration?.start
        val end = path.metadata.duration?.end


        views.pathTitle.title = PathNameFactory(fragment.requireContext()).getName(path)

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
            fragment.requireContext(),
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
        fragment.inBackground {
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
            fragment.getString(R.string.none),
            fragment.getString(R.string.cell_signal),
            fragment.getString(R.string.elevation),
            fragment.getString(R.string.time),
            fragment.getString(R.string.path_slope)
        )[path.style.point.ordinal]

        views.pathLegend.colorScale = factory.createColorScale(waypoints)
        views.pathLegend.labels = factory.createLabelMap(waypoints)
        views.pathLegend.isVisible = path.style.point != PathPointColoringStyle.None
    }

    // Waypoints
    private fun viewPoints() {
        views.root.scrollTo(0, 0)
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
            chart.highlight(point)
        } else {
            deselectPoint()
        }

        onPathChanged()
    }

    private fun navigateToWaypoint(point: PathPoint) {
        val path = path ?: return
        val command = NavigateToPointCommand(
            fragment,
            converter,
            beaconNavigator
        )
        tryOrNothing {
            command.execute(path, point)
        }
    }

    private fun navigateToNearestPathPoint() {
        val path = path ?: return
        val points = waypoints
        val command = NavigateToPathCommand(
            if (prefs.navigation.onlyNavigateToPoints) NearestPathPointNavigator() else NearestPathLineNavigator(),
            gps,
            converter,
            beaconNavigator
        )

        fragment.toast(fragment.getString(R.string.navigating_to_nearest_path_point))

        fragment.inBackground {
            command.execute(path, points)
        }
    }

    private fun deleteWaypoint(point: PathPoint) {
        val path = path ?: return
        val command = DeletePointCommand(fragment.requireContext(), fragment)
        command.execute(path, point)
    }

    private fun createBeacon(point: PathPoint) {
        val path = path ?: return
        val command = CreateBeaconFromPointCommand(fragment.requireContext())
        command.execute(path, point)
    }

    private fun getPointFactory(): IPointDisplayFactory {
        return getPointFactory(fragment.requireContext(), path?.style?.point ?: PathPointColoringStyle.None)
    }

    private fun updateDebugStats() {
        if (!isDebug()) {
            return
        }

        views.pathsTiming.isVisible = true

        fragment.inBackground {
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

private class PathScrollState {
    val scrollState = ScrollState(0)
    var isScrollable by mutableStateOf(true)
    var height by mutableIntStateOf(0)

    fun scrollTo(x: Int, y: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            scrollState.scrollTo(y)
        }
    }

    fun post(action: () -> Unit): Boolean {
        action()
        return true
    }
}

private class PathMapHolderState(initialHeight: Int) {
    var height by mutableIntStateOf(initialHeight)
    var top by mutableIntStateOf(0)
}

private class PathTitleState {
    var title by mutableStateOf("")
    var subtitle by mutableStateOf("")
    var onSubtitleClick by mutableStateOf<(() -> Unit)?>(null)
    var onMenuClick by mutableStateOf<(() -> Unit)?>(null)
}

private class PathDataPointState(
    initialDescription: String,
    private val icon: Int
) {
    var title by mutableStateOf("")
    var description by mutableStateOf(initialDescription)
    var onClick by mutableStateOf<(() -> Unit)?>(null)
    private var dataPointVisible by mutableStateOf(true)

    var isVisible: Boolean
        get() = dataPointVisible
        set(value) {
            dataPointVisible = value
        }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        if (!dataPointVisible) {
            return
        }
        Box(
            modifier = modifier.clickable(
                enabled = onClick != null,
                onClick = { onClick?.invoke() }
            )
        ) {
            DataPointContent(
                title = title,
                description = description,
                icon = icon,
                tint = MaterialTheme.colorScheme.onSurface,
                showDescription = true,
                gravity = Gravity.START,
                onDescriptionClick = null
            )
        }
    }
}

@Composable
private fun PathTitle(
    state: PathTitleState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Balance the trailing menu button so the title remains centered.
        Box(modifier = Modifier.size(48.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = state.subtitle,
                modifier = Modifier.clickable { state.onSubtitleClick?.invoke() },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        IconButton(onClick = { state.onMenuClick?.invoke() }) {
            Icon(
                painter = painterResource(R.drawable.ic_menu_dots),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun PathOverviewHost(
    views: PathOverviewViews,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    Column(
        modifier = modifier
            .onSizeChanged { views.root.height = it.height }
            .verticalScroll(
                state = views.root.scrollState,
                enabled = views.root.isScrollable
            )
    ) {
        PathTitle(views.pathTitle, Modifier.fillMaxWidth())
        views.pathStyleControls.Content(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(with(density) { views.pathMapHolder.height.toDp() })
                .onGloballyPositioned {
                    views.pathMapHolder.top = it.positionInParent().y.toInt()
                }
        ) {
            views.pathImage.Content(Modifier.fillMaxSize())
            views.pathMapFullscreenToggle.Content(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, bottom = 8.dp)
            )
        }
        views.pathSelectedPoint.Content(
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )
        if (views.pathLegend.isVisible) {
            ColorScaleContent(
                state = views.pathLegend,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                views.pathDuration.Content(Modifier.weight(1f))
                views.pathDistance.Content(Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                views.pathWaypoints.Content(Modifier.weight(1f))
                views.pathDifficulty.Content(Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                views.pathElevationGain.Content(Modifier.weight(1f))
                views.pathElevationLoss.Content(Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                views.pathElevationMin.Content(Modifier.weight(1f))
                views.pathElevationMax.Content(Modifier.weight(1f))
            }
        }
        Text(
            text = stringResource(R.string.elevation),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        views.chart.Content(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) {
                    context.resources.getDimensionPixelSize(R.dimen.chart_height).toDp()
                })
                .padding(horizontal = 16.dp)
        )
        views.pathActions.Content(
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )
        views.pathsTiming.Content(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        )
    }
}

private fun createPathOverviewViews(context: Context): PathOverviewViews {
    val root = PathScrollState()

    val title = PathTitleState()

    val styleControls = PathStyleControlsView(context)
    val mapHolder = PathMapHolderState(Resources.dp(context, 250f).toInt())

    val pathImage = MapView(context, null).apply {
        id = R.id.path_image
        setBackgroundResource(R.drawable.rounded_rectangle)
        backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorSecondary))
        keepScreenOn = true
    }

    val fullscreenToggle = PathIconButton(R.drawable.ic_recenter)

    val selectedPoint = PathSelectedPointView().apply {
        isVisible = false
    }

    val legend = ColorScaleState()
    val duration = createDataPoint(context, R.drawable.ic_tool_clock, R.string.duration)
    val distance = createDataPoint(context, R.drawable.ruler, R.string.distance)
    val points = createDataPoint(context, R.drawable.ic_location, R.string.points)
    val difficulty = createDataPoint(context, R.drawable.steps, R.string.difficulty)
    val gain = createDataPoint(context, R.drawable.ic_elevation_gain, R.string.ascent)
    val loss = createDataPoint(context, R.drawable.ic_elevation_loss, R.string.descent)
    val min = createDataPoint(context, R.drawable.ic_elevation_loss, R.string.lowest_point_elevation)
    val max = createDataPoint(context, R.drawable.ic_elevation_gain, R.string.highest_point_elevation)

    val chart = PathElevationChart(context)

    val pathActions = PathActionsView(context)

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

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(ComposeColor(pathColor), CircleShape)
                    .clickable(onClick = onColorClick)
            )
            PathStyleDropdownText(
                text = lineStyle,
                onClick = onLineStyleClick,
                modifier = Modifier.padding(start = 8.dp)
            )
            PathStyleDropdownText(
                text = pointStyle,
                onClick = onPointStyleClick,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun PathStyleDropdownText(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Icon(
            painter = painterResource(R.drawable.ic_drop_down),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 16.dp)
                .size(18.dp)
        )
    }
}

private class PathSelectedPointView {
    var point by mutableStateOf<PathPoint?>(null)
    var formatService by mutableStateOf<FormatService?>(null)
    var baseDistanceUnits by mutableStateOf<DistanceUnits?>(null)
    var onCreateBeacon by mutableStateOf<(PathPoint) -> Unit>({})
    var onDelete by mutableStateOf<(PathPoint) -> Unit>({})
    var onNavigate by mutableStateOf<(PathPoint) -> Unit>({})
    private var pointVisible by mutableStateOf(true)

    var isVisible: Boolean
        get() = pointVisible
        set(value) {
            pointVisible = value
        }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        if (!pointVisible) {
            return
        }
        val point = point ?: return
        val formatService = formatService ?: return
        val baseDistanceUnits = baseDistanceUnits ?: return

        Box(modifier = modifier) {
            PathPointRow(
                point = point,
                formatService = formatService,
                baseDistanceUnits = baseDistanceUnits,
                onCreateBeacon = onCreateBeacon,
                onDelete = onDelete,
                onNavigate = onNavigate,
                onView = {}
            )
        }
    }
}

private class PathTimingView {
    var text by mutableStateOf("")
    private var timingVisible by mutableStateOf(true)

    var isVisible: Boolean
        get() = timingVisible
        set(value) {
            timingVisible = value
        }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        if (!timingVisible) {
            return
        }
        Text(
            text = text,
            modifier = modifier,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private class PathActionsView(private val context: Context) {
    var onNavigateClick by mutableStateOf({})
    var onAddPointClick by mutableStateOf({})

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.Center
        ) {
            PathActionButton(
                text = context.getString(R.string.navigate),
                icon = R.drawable.ic_beacon,
                onClick = onNavigateClick
            )
            PathActionButton(
                text = context.getString(R.string.point),
                icon = R.drawable.ic_add,
                onClick = onAddPointClick,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Composable
private fun PathActionButton(
    text: String,
    icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier
                .padding(end = 8.dp)
                .size(18.dp)
        )
        Text(text = text)
    }
}

private class PathIconButton(
    icon: Int
) {
    private var iconRes by mutableIntStateOf(icon)
    var onClick by mutableStateOf<(() -> Unit)?>(null)

    fun setImageResource(icon: Int) {
        iconRes = icon
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        Box(
            modifier = modifier
                .size(48.dp)
                .clickable { onClick?.invoke() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = ComposeColor.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private fun createDataPoint(
    context: Context,
    icon: Int,
    description: Int
): PathDataPointState {
    return PathDataPointState(
        initialDescription = context.getString(description),
        icon = icon
    )
}

private fun Int.dp(context: Context): Int {
    return Resources.dp(context, toFloat()).toInt()
}

@Preview(showBackground = true)
@Composable
private fun PathOverviewHostPreview() {
    TrailSenseTheme {
        PathOverviewHost(
            views = createPathOverviewViews(LocalContext.current)
        )
    }
}
