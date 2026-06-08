package com.kylecorry.trail_sense.tools.paths.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.gridlayout.widget.GridLayout
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.toast
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
import com.kylecorry.andromeda.list.LockableScrollView
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.views.chart.Chart
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.MathExtensions.roundPlaces
import com.kylecorry.sol.math.statistics.Statistics
import com.kylecorry.sol.science.geography.Geography
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.units.Distance
import com.google.android.material.button.MaterialButton
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
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
import com.kylecorry.trail_sense.shared.views.ColorScaleView
import com.kylecorry.trail_sense.shared.views.DataPointContent
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAndromedaComposeFragment
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
import kotlinx.coroutines.launch
import java.time.Duration


class PathOverviewFragment : TrailSenseAndromedaComposeFragment() {

    private var pathOverviewViews: PathOverviewViews? = null
    private val views: PathOverviewViews
        get() = pathOverviewViews ?: error("Path overview views are not ready")
    private val isViewReady: Boolean
        get() = pathOverviewViews != null && view != null

    private val prefs by lazy { UserPreferences(requireContext()) }
    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val throttle = Throttle(20)

    private val sensorService by lazy { SensorService(requireContext()) }
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
    private val pathService by lazy { PathService.getInstance(requireContext()) }

    private var pointSheet: PathPointsListFragment? = null

    private lateinit var chart: PathElevationChart
    private var path: Path? = null
    private var waypoints: List<PathPoint> = emptyList()
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

    private val converter: IPathPointBeaconConverter by lazy {
        TemporaryPathPointBeaconConverter(
            getString(R.string.waypoint)
        )
    }

    private val beaconNavigator: IBeaconNavigator by lazy {
        BeaconNavigator(
            BeaconService(requireContext()),
            NavControllerAppNavigation(findNavController())
        )
    }

    override fun beforeCreateContent() {
        pathOverviewViews = createPathOverviewViews(requireContext())
    }

    @Composable
    override fun FragmentContent() {
        PathOverviewHost(
            views = views,
            modifier = Modifier.fillMaxSize()
        )
    }

    override fun onDestroyView() {
        pathOverviewViews = null
        super.onDestroyView()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pathId = requireArguments().getLong("path_id")
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
    }

    override fun onResume() {
        super.onResume()
        // Populate the last known location
        views.pathImage.userLocation = gps.location
        views.pathImage.userLocationAccuracy =
            gps.horizontalAccuracy?.let { Distance.meters(it) }

        views.pathImage.start()
    }

    override fun onPause() {
        super.onPause()
        views.pathImage.stop()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chart = PathElevationChart(views.chart)

        views.pathImage.isInteractive = true

        views.pathImage.setOnTouchListener { v, event ->
            views.root.isScrollable = event.action == MotionEvent.ACTION_UP
            false
        }

        views.pathMapFullscreenToggle.setOnClickListener {
            isFullscreen = !isFullscreen
            views.pathMapHolder.layoutParams = if (isFullscreen) {
                val legendHeight = Resources.dp(requireContext(), 72f).toInt()
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    views.root.height - legendHeight
                )
            } else {
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    Resources.dp(requireContext(), 250f).toInt()
                ).also {
                    it.marginStart = Resources.dp(requireContext(), 16f).toInt()
                    it.marginEnd = Resources.dp(requireContext(), 16f).toInt()
                }
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

        views.addPointBtn.setOnClickListener {
            inBackground {
                var wasSuccessful = false
                val job = launch {
                    BacktrackCommand(requireContext(), pathId).execute()
                    wasSuccessful = true
                }

                Alerts.withCancelableLoading(
                    requireContext(),
                    getString(R.string.loading),
                    onCancel = { job.cancel() }) {
                    job.join()
                    if (wasSuccessful) {
                        toast(getString(R.string.point_added))
                    }
                }
            }
        }

        views.navigateBtn.setOnClickListener {
            navigateToNearestPathPoint()
        }

        views.pathTitle.rightButton.setOnClickListener {
            showPathMenu()
        }

        views.pathTitle.subtitle.setOnClickListener {
            movePath()
        }

        chart.setOnPointClickListener {
            viewWaypoint(it)
        }

        views.pathElevationMin.setOnClickListener {
            val point = waypoints.filter { it.elevation != null }.minByOrNull { it.elevation!! }
            point?.let { viewWaypoint(it) }
        }

        views.pathElevationMax.setOnClickListener {
            val point = waypoints.filter { it.elevation != null }.maxByOrNull { it.elevation!! }
            point?.let { viewWaypoint(it) }
        }

        observe(pathService.getLivePath(pathId)) {
            path = it
            views.root.post {
                updateParent()
                updateElevationPlot()
                updatePointStyleLegend()
                updatePathMap()
                onPathChanged()
            }
        }

        observe(pathService.getWaypointsLive(pathId)) {
            onWaypointsChanged(it)
        }

        observe(gps) {
            updateDeclination()
            views.pathImage.userLocation = gps.location
            views.pathImage.userLocationAccuracy =
                gps.horizontalAccuracy?.let { Distance.meters(it) }
            onPathChanged()
        }

        observe(compass) {
            views.pathImage.userAzimuth = compass.bearing
        }

        views.pathImage.setLayers(
            listOf(layer, myLocationLayer, scaleBarLayer)
        )

        views.pathLineStyle.setOnClickListener {
            val path = path ?: return@setOnClickListener
            val command = ChangePathLineStyleCommand(requireContext(), this)
            command.execute(path)
        }

        views.pathColor.setOnClickListener {
            val path = path ?: return@setOnClickListener
            val command = ChangePathColorCommand(requireContext(), this)
            command.execute(path)
        }

        views.pathPointStyle.setOnClickListener {
            val path = path ?: return@setOnClickListener
            val command = ChangePointStyleCommand(requireContext(), this)
            command.execute(path)
        }
    }

    private fun onWaypointsChanged(waypoints: List<PathPoint>) {
        inBackground {
            onDefault {
                this@PathOverviewFragment.waypoints =
                    hikingService.correctElevations(waypoints.sortedBy { it.id }).reversed()
            }
            onIO {
                DebugPathElevationsCommand(
                    requireContext(),
                    waypoints,
                    this@PathOverviewFragment.waypoints
                ).execute()
            }
            onMain {
                val selected = selectedPointId
                if (selected != null && waypoints.find { it.id == selected } == null) {
                    deselectPoint()
                }
                pointSheet?.setPoints(this@PathOverviewFragment.waypoints)
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
        inBackground {
            val parent = onIO { pathService.getGroup(path.parentId) }
            if (isViewReady) {
                views.pathTitle.subtitle.text = parent?.name ?: getString(R.string.no_group)
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
        val command = MoveIPathCommand(requireContext(), pathService)
        inBackground {
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
            views.pathTitle.rightButton, listOf(
                getString(R.string.rename),
                if (path.temporary) getString(R.string.keep_forever) else null,
                if (path.style.visible) getString(R.string.hide) else getString(
                    R.string.show
                ),
                getString(R.string.export),
                getString(R.string.simplify),
                getString(R.string.points)
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
        val command = SimplifyPathCommand(requireContext(), this, pathService)
        command.execute(path)
    }

    private fun exportPath(path: Path) {
        val command = ExportPathCommand(
            requireContext(),
            this,
            IOFactory().createGpxService(this),
            pathService
        )
        command.execute(path)
    }

    private fun togglePathVisibility(path: Path) {
        val command = TogglePathVisibilityCommand(requireContext(), this, pathService)
        command.execute(path)
    }

    private fun renamePath(path: Path) {
        val command = RenamePathCommand(requireContext(), this, pathService)
        command.execute(path)
    }

    private fun keepPath(path: Path) {
        val command = KeepPathCommand(requireContext(), this, pathService)
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

        views.pathLineStyle.text = listOf(
            getString(R.string.solid),
            getString(R.string.dotted),
            getString(R.string.arrow),
            getString(R.string.dashed),
            getString(R.string.square),
            getString(R.string.diamond),
            getString(R.string.cross)
        )[path.style.line.ordinal]

        val distance =
            path.metadata.distance.convertTo(prefs.baseDistanceUnits).toRelativeDistance()

        val start = path.metadata.duration?.start
        val end = path.metadata.duration?.end


        views.pathTitle.title.text = PathNameFactory(requireContext()).getName(path)

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

        Colors.setImageColor(views.pathColor, path.style.color)

        updateMapLayer()

        updateDebugStats()
    }

    private fun updateMapLayer() {
        val path = path ?: return
        val waypointFeatures = waypoints.asBeacons(
            requireContext(),
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
        inBackground {
            onIO {
                declination = declinationProvider.getDeclination()
                compass.declination = declination
            }
        }
    }

    private fun deselectPoint() {
        selectedPointId = null
        views.pathSelectedPoint.isVisible = false
        chart.removeHighlight()
        updateMapLayer()
    }

    private fun updatePointStyleLegend() {

        val path = path ?: return

        val factory = getPointFactory()

        views.pathPointStyle.text = listOf(
            getString(R.string.none),
            getString(R.string.cell_signal),
            getString(R.string.elevation),
            getString(R.string.time),
            getString(R.string.path_slope)
        )[path.style.point.ordinal]

        views.pathLegend.colorScale = factory.createColorScale(waypoints)
        views.pathLegend.labels = factory.createLabelMap(waypoints)
        views.pathLegend.isVisible = path.style.point != PathPointColoringStyle.None
    }

    // Waypoints
    private fun viewPoints() {
        views.root.scrollTo(0, 0)
        pointSheet = PathPointsListFragment()
        pointSheet?.show(this)
        pointSheet?.onCreateBeaconListener = { createBeacon(it) }
        pointSheet?.onDeletePointListener = { deleteWaypoint(it) }
        pointSheet?.onNavigateToPointListener = { navigateToWaypoint(it) }
        pointSheet?.onViewPointListener = { viewWaypoint(it) }
        pointSheet?.setPoints(waypoints)
    }

    private fun viewWaypoint(point: PathPoint) {
        selectedPointId = if (selectedPointId == point.id) {
            null
        } else {
            point.id
        }

        views.pathSelectedPoint.removeAllViews()

        if (selectedPointId != null) {
            views.pathSelectedPoint.isVisible = true
            chart.highlight(point)
            views.pathSelectedPoint.addView(
                ComposeView(requireContext()).apply {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                    setContent {
                        TrailSenseTheme {
                            PathPointRow(
                                point = point,
                                formatService = formatService,
                                baseDistanceUnits = prefs.baseDistanceUnits,
                                onCreateBeacon = { createBeacon(it) },
                                onDelete = { deleteWaypoint(it) },
                                onNavigate = { navigateToWaypoint(it) },
                                onView = {}
                            )
                        }
                    }
                },
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        } else {
            deselectPoint()
        }

        onPathChanged()
    }

    private fun navigateToWaypoint(point: PathPoint) {
        val path = path ?: return
        val command = NavigateToPointCommand(
            this,
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

        toast(getString(R.string.navigating_to_nearest_path_point))

        inBackground {
            command.execute(path, points)
        }
    }

    private fun deleteWaypoint(point: PathPoint) {
        val path = path ?: return
        val command = DeletePointCommand(requireContext(), this)
        command.execute(path, point)
    }

    private fun createBeacon(point: PathPoint) {
        val path = path ?: return
        val command = CreateBeaconFromPointCommand(requireContext())
        command.execute(path, point)
    }

    private fun getPointFactory(): IPointDisplayFactory {
        return getPointFactory(requireContext(), path?.style?.point ?: PathPointColoringStyle.None)
    }

    private fun updateDebugStats() {
        if (!isDebug()) {
            return
        }

        views.pathsTiming.isVisible = true

        inBackground {
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
    val root: LockableScrollView,
    val pathTitle: Toolbar,
    val pathColor: ImageView,
    val pathLineStyle: TextView,
    val pathPointStyle: TextView,
    val pathMapHolder: ConstraintLayout,
    val pathImage: MapView,
    val pathMapFullscreenToggle: ImageButton,
    val pathSelectedPoint: FrameLayout,
    val pathLegend: ColorScaleView,
    val pathDuration: PathDataPointState,
    val pathDistance: PathDataPointState,
    val pathWaypoints: PathDataPointState,
    val pathDifficulty: PathDataPointState,
    val pathElevationGain: PathDataPointState,
    val pathElevationLoss: PathDataPointState,
    val pathElevationMin: PathDataPointState,
    val pathElevationMax: PathDataPointState,
    val chart: Chart,
    val navigateBtn: MaterialButton,
    val addPointBtn: MaterialButton,
    val pathsTiming: TextView
)

private class PathDataPointState(
    initialDescription: String,
    private val icon: Int
) {
    lateinit var view: View
    var title by mutableStateOf("")
    var description by mutableStateOf(initialDescription)

    var isVisible: Boolean
        get() = view.isVisible
        set(value) {
            view.isVisible = value
        }

    fun setOnClickListener(listener: View.OnClickListener?) {
        view.setOnClickListener(listener)
    }

    @Composable
    fun Content() {
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

@Composable
private fun PathOverviewHost(
    views: PathOverviewViews,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { views.root }
    )
}

private fun createPathOverviewViews(context: Context): PathOverviewViews {
    val root = LockableScrollView(context, null).apply {
        id = R.id.path_scroll
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    val content = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }
    root.addView(
        content,
        ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )

    val title = Toolbar(context, null).apply {
        id = R.id.path_title
        rightButton.setImageResource(R.drawable.ic_menu_dots)
        rightButton.isVisible = true
        leftButton.isVisible = false
    }
    content.addView(
        title,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )

    val styleRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    content.addView(
        styleRow,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = 16.dp(context)
            marginEnd = 16.dp(context)
            bottomMargin = 16.dp(context)
        }
    )

    val pathColor = ImageView(context).apply {
        id = R.id.path_color
        setImageResource(R.drawable.bubble)
        imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.blue))
    }
    styleRow.addView(
        pathColor,
        LinearLayout.LayoutParams(24.dp(context), 24.dp(context)).apply {
            marginEnd = 8.dp(context)
        }
    )

    val pathLineStyle = createDropdownText(context, R.id.path_line_style)
    styleRow.addView(
        pathLineStyle,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            marginEnd = 8.dp(context)
        }
    )

    val pathPointStyle = createDropdownText(context, R.id.path_point_style)
    styleRow.addView(
        pathPointStyle,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )

    val mapHolder = ConstraintLayout(context).apply {
        id = R.id.path_map_holder
    }
    content.addView(
        mapHolder,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            250.dp(context)
        ).apply {
            marginStart = 16.dp(context)
            marginEnd = 16.dp(context)
        }
    )

    val pathImage = MapView(context, null).apply {
        id = R.id.path_image
        setBackgroundResource(R.drawable.rounded_rectangle)
        backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorSecondary))
        keepScreenOn = true
    }
    mapHolder.addView(
        pathImage,
        ConstraintLayout.LayoutParams(0, 0).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
    )

    val fullscreenToggle = ImageButton(context).apply {
        id = R.id.path_map_fullscreen_toggle
        setImageResource(R.drawable.ic_recenter)
        setBackgroundColor(Color.TRANSPARENT)
        imageTintList = ColorStateList.valueOf(Color.WHITE)
    }
    mapHolder.addView(
        fullscreenToggle,
        ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            marginStart = 8.dp(context)
            bottomMargin = 8.dp(context)
        }
    )

    val selectedPoint = FrameLayout(context).apply {
        id = R.id.path_selected_point
        visibility = View.GONE
    }
    content.addView(
        selectedPoint,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 16.dp(context)
        }
    )

    val legend = ColorScaleView(context, null).apply {
        id = R.id.path_legend
    }
    content.addView(
        legend,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            40.dp(context)
        ).apply {
            marginStart = 16.dp(context)
            marginEnd = 16.dp(context)
            topMargin = 16.dp(context)
        }
    )

    val grid = GridLayout(context).apply {
        id = R.id.path_grid
        columnCount = 2
        useDefaultMargins = false
    }
    content.addView(
        grid,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = 16.dp(context)
            marginEnd = 16.dp(context)
            topMargin = 16.dp(context)
        }
    )

    val duration = addDataPoint(grid, context, R.id.path_duration, R.drawable.ic_tool_clock, R.string.duration)
    val distance = addDataPoint(grid, context, R.id.path_distance, R.drawable.ruler, R.string.distance)
    val points = addDataPoint(grid, context, R.id.path_waypoints, R.drawable.ic_location, R.string.points)
    val difficulty = addDataPoint(grid, context, R.id.path_difficulty, R.drawable.steps, R.string.difficulty)
    val gain = addDataPoint(grid, context, R.id.path_elevation_gain, R.drawable.ic_elevation_gain, R.string.ascent)
    val loss = addDataPoint(grid, context, R.id.path_elevation_loss, R.drawable.ic_elevation_loss, R.string.descent)
    val min = addDataPoint(grid, context, R.id.path_elevation_min, R.drawable.ic_elevation_loss, R.string.lowest_point_elevation)
    val max = addDataPoint(grid, context, R.id.path_elevation_max, R.drawable.ic_elevation_gain, R.string.highest_point_elevation)

    val elevationTitle = TextView(context).apply {
        text = context.getString(R.string.elevation)
        gravity = Gravity.CENTER
    }
    content.addView(
        elevationTitle,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = 16.dp(context)
        }
    )

    val chart = Chart(context).apply {
        id = R.id.chart
    }
    content.addView(
        chart,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            context.resources.getDimensionPixelSize(R.dimen.chart_height)
        ).apply {
            marginStart = 16.dp(context)
            marginEnd = 16.dp(context)
        }
    )

    val buttonRow = LinearLayout(context).apply {
        gravity = Gravity.CENTER
        orientation = LinearLayout.HORIZONTAL
    }
    content.addView(
        buttonRow,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 16.dp(context)
            bottomMargin = context.resources.getDimensionPixelSize(R.dimen.default_bottom_margin)
        }
    )

    val navigate = MaterialButton(context).apply {
        id = R.id.navigate_btn
        text = context.getString(R.string.navigate)
        icon = ContextCompat.getDrawable(context, R.drawable.ic_beacon)
    }
    buttonRow.addView(
        navigate,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            marginEnd = 16.dp(context)
        }
    )

    val addPoint = MaterialButton(context).apply {
        id = R.id.add_point_btn
        text = context.getString(R.string.point)
        icon = ContextCompat.getDrawable(context, R.drawable.ic_add)
    }
    buttonRow.addView(
        addPoint,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )

    val timing = TextView(context).apply {
        id = R.id.paths_timing
        visibility = View.GONE
    }
    content.addView(
        timing,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = 16.dp(context)
            marginEnd = 16.dp(context)
            bottomMargin = context.resources.getDimensionPixelSize(R.dimen.default_bottom_margin)
        }
    )

    return PathOverviewViews(
        root = root,
        pathTitle = title,
        pathColor = pathColor,
        pathLineStyle = pathLineStyle,
        pathPointStyle = pathPointStyle,
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
        navigateBtn = navigate,
        addPointBtn = addPoint,
        pathsTiming = timing
    )
}

private fun createDropdownText(context: Context, id: Int): TextView {
    return TextView(context).apply {
        this.id = id
        compoundDrawablePadding = 16.dp(context)
        setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_drop_down, 0)
        compoundDrawableTintList = ColorStateList.valueOf(Resources.androidTextColorSecondary(context))
    }
}

private fun addDataPoint(
    grid: GridLayout,
    context: Context,
    id: Int,
    icon: Int,
    description: Int
): PathDataPointState {
    val dataPoint = PathDataPointState(
        initialDescription = context.getString(description),
        icon = icon
    )
    val view = ComposeView(context).apply {
        this.id = id
        dataPoint.view = this
        setContent {
            TrailSenseTheme {
                dataPoint.Content()
            }
        }
    }
    grid.addView(view, createGridDataPointLayoutParams(context))
    return dataPoint
}

private fun createGridDataPointLayoutParams(context: Context): GridLayout.LayoutParams {
    val margin = 8.dp(context)
    return GridLayout.LayoutParams().apply {
        width = 0
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        setMargins(margin, margin, margin, margin)
    }
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
