package com.kylecorry.trail_sense.tools.navigation.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Gravity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.dialog
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.andromeda.fragments.observeFlow
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.andromeda.sense.clinometer.Clinometer
import com.kylecorry.andromeda.sense.orientation.DeviceOrientation
import com.kylecorry.luna.concurrency.CoroutineQueueRunner
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.sol.science.geography.projections.AzimuthalEquidistantProjection
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.ImproveAccuracyAlerter
import com.kylecorry.trail_sense.settings.ui.compassCalibrationView
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.hooks.HookTriggers
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.MapLayersBottomSheet
import com.kylecorry.trail_sense.shared.map_layers.ui.MapAttributionView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.getAttribution
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.shared.quickactions.ComposeQuickActionButtonView
import com.kylecorry.trail_sense.shared.safeRoundToInt
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sharing.Share
import com.kylecorry.trail_sense.shared.views.DataPointContent
import com.kylecorry.trail_sense.shared.views.NorthReferenceBadge
import com.kylecorry.trail_sense.shared.views.SensorStatusBadgeView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAndromedaComposeFragment
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.tools.map.ui.MapView
import com.kylecorry.trail_sense.tools.navigation.NavigationToolRegistration
import com.kylecorry.trail_sense.tools.navigation.domain.CompassStyle
import com.kylecorry.trail_sense.tools.navigation.domain.CompassStyleChooser
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationService
import com.kylecorry.trail_sense.tools.navigation.infrastructure.NavigationScreenLock
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.quickactions.NavigationQuickActionBinder
import com.kylecorry.trail_sense.tools.navigation.ui.data.UpdateAstronomyLayerCommand
import com.kylecorry.trail_sense.tools.navigation.ui.errors.NavigatorUserErrors
import com.kylecorry.trail_sense.tools.navigation.ui.layers.NavigationCompassLayerManager
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.BeaconCompassLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.ICompassView
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.MarkerCompassLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.NavigationCompassLayer
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.GPSDiagnosticScanner
import com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics.MagnetometerDiagnosticScanner
import java.time.Duration
import java.time.Instant

class NavigatorFragment : TrailSenseAndromedaComposeFragment() {

    private var navigatorViews: NavigatorViews? = null
    private val views: NavigatorViews
        get() = navigatorViews ?: error("Navigator views are not ready")
    private val isViewReady: Boolean
        get() = navigatorViews != null && view != null

    private val orientation by lazy { sensorService.getOrientation() }
    private val compass by lazy { sensorService.getCompass(orientation) }
    private val gps by lazy { sensorService.getGPS(frequency = Duration.ofMillis(200)) }
    private val clinometer by lazy { Clinometer(orientation, isAugmentedReality = true) }
    private val altimeter by lazy { sensorService.getAltimeter(gps = gps) }
    private val speedometer by lazy { sensorService.getSpeedometer(gps = gps) }
    private val declinationProvider by lazy {
        DeclinationFactory().getDeclinationStrategy(
            userPrefs,
            gps
        )
    }
    private var declination = 0f

    private val userPrefs by lazy { UserPreferences(requireContext()) }

    private val beaconRepo by lazy { BeaconRepo.getInstance(requireContext()) }

    private val sensorService by lazy { SensorService(requireContext()) }

    private val navigationService = NavigationService()
    private val formatService by lazy { FormatService.getInstance(requireContext()) }

    private var beacons: Collection<Beacon> = listOf()
    private var nearbyBeacons: List<Beacon> = listOf()

    private var destination: Destination? = null
    private val navigator by lazy { Navigator.getInstance(requireContext()) }

    // Diagnostics
    private val errors by lazy { NavigatorUserErrors(this) }

    // Data commands
    private val updateAstronomyLayerCommand by lazy {
        UpdateAstronomyLayerCommand(
            astronomyCompassLayer,
            userPrefs,
            gps
        ) { declination }
    }

    private val loadBeaconsRunner = CoroutineQueueRunner()

    private val layers = NavigationCompassLayerManager()
    private var layerSheet: MapLayersBottomSheet? = null

    // Compass layers
    private val beaconCompassLayer = BeaconCompassLayer()
    private val astronomyCompassLayer = MarkerCompassLayer()
    private val navigationCompassLayer = NavigationCompassLayer()

    // Feature availability
    private val hasCompass by lazy { sensorService.hasCompass() }

    // Cached preferences
    private val baseDistanceUnits by lazy { userPrefs.baseDistanceUnits }
    private val isNearbyEnabled by lazy { userPrefs.navigation.showMultipleBeacons }
    private val isNearbyLinearOnly by lazy { userPrefs.navigation.showNearbyBeaconsOnlyOnLinearCompass }
    private val nearbyCount by lazy { userPrefs.navigation.numberOfVisibleBeacons }
    private val nearbyDistance
        get() = userPrefs.navigation.maxBeaconDistance
    private val styleChooser by lazy { CompassStyleChooser(userPrefs.navigation, hasCompass) }
    private val useTrueNorth by lazy { userPrefs.compass.useTrueNorth }
    private val screenLock by lazy { NavigationScreenLock(userPrefs.navigation.keepScreenUnlockedWhileOpen) }

    // State
    private var diagnosticResults: Map<Int, List<String>> = emptyMap()
    private var lastErrorCodes: List<String> = emptyList()
    private var lastAttributionLayerKey: Int? = null
    private var lastAttributionRadarVisible: Boolean? = null

    private val northReferenceHideTimer = CoroutineTimer {
        if (isViewReady) {
            views.northReferenceIndicator.showLabel = false
        }
    }

    private val compassLayerTimer = CoroutineTimer {
        updateCompassLayers()
    }

    private val updateTimer = CoroutineTimer {
        refreshNavigation()
    }

    private val triggers = HookTriggers()

    override fun beforeCreateContent() {
        navigatorViews = createNavigatorViews(requireContext())
    }

    @Composable
    override fun FragmentContent() {
        NavigatorHost(
            views = views,
            modifier = Modifier.fillMaxSize()
        )
    }

    override fun onDestroyView() {
        activity?.let { screenLock.releaseLock(it) }
        updateTimer.stop()
        compassLayerTimer.stop()
        northReferenceHideTimer.stop()
        navigatorViews = null
        super.onDestroyView()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val beaconId = arguments?.getLong("destination") ?: 0L

        // Load the destination and start navigation
        if (beaconId != 0L) {
            showCalibrationDialog()
            navigator.navigateTo(beaconId)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeFlow(navigator.destination2) {
            destination = it
            updateNearbyBeacons()
            handleShowWhenLocked()
        }

        // Observe diagnostics
        listOf(
            GPSDiagnosticScanner(gps),
            MagnetometerDiagnosticScanner()
        ).mapIndexed { index, scanner ->
            observeFlow(
                scanner.fullScan(requireContext()),
                BackgroundMinimumState.Resumed
            ) { results ->
                diagnosticResults = diagnosticResults + (index to results.map { it.id })
            }
        }

        views.linearCompass.setCompassLayers(
            listOf(
                astronomyCompassLayer,
                beaconCompassLayer,
                navigationCompassLayer
            )
        )
        views.radarCompass.setCompassLayers(
            listOfNotNull(
                astronomyCompassLayer,
                if (isNearbyLinearOnly) null else beaconCompassLayer,
                navigationCompassLayer
            )
        )


        NavigationQuickActionBinder(
            this,
            views.navigationTitle.leftButton,
            views.navigationTitle.rightButton,
            userPrefs.navigation
        ).bind()

        observeFlow(beaconRepo.getBeacons()) {
            beacons = it
            updateNearbyBeacons()
        }

        observe(compass) { }
        observe(clinometer) { }
        observe(altimeter) { }
        observe(gps) { }
        observe(speedometer) { }

        views.navigationTitle.onSubtitleLongClick = {
            Share.shareLocation(
                this,
                gps.location
            )
            true
        }

        views.navigationTitle.onSubtitleClick = {
            val sheet = LocationBottomSheet()
            sheet.gps = gps
            sheet.show(this)
        }

        views.altitudeView.setOnClickListener {
            val sheet = AltitudeBottomSheet()
            sheet.currentAltitude = Reading(altimeter.altitude, Instant.now())
            sheet.show(this)
        }

        views.linearCompass.setOnClickListener {
            toggleDestinationBearing()
        }

        views.beaconBtn.setOnClickListener {
            findNavController().openTool(Tools.BEACONS)
        }

        views.beaconBtn.setOnLongClickListener {
            if (gps.hasValidReading) {
                val bundle = Bundle().apply {
                    putParcelable("initial_location", GeoUri(
                        gps.location,
                        if (altimeter.hasValidReading) altimeter.altitude else gps.altitude
                    ))
                }
                findNavController().openTool(Tools.BEACONS, bundle)
            } else {
                findNavController().openTool(Tools.BEACONS)
            }
            true
        }

        views.accuracyView.setSensors(gps, compass)
        views.accuracyView.setOnClickListener { displayAccuracyTips() }

        views.radarCompassMap.setOnSingleTapListener {
            toggleDestinationBearing()
        }

        views.radarCompassMap.setOnLongPressListener {
            layerSheet?.dismiss()
            layerSheet = MapLayersBottomSheet(
                NavigationToolRegistration.MAP_ID
            )
            layers.pause(views.radarCompassMap)
            layerSheet?.setOnDismissListener {
                layers.resume(requireContext(), views.radarCompassMap, this)
            }
            layerSheet?.show(this)
        }

        views.linearCompass.setOnClickListener {
            toggleDestinationBearing()
        }

        views.radarCompassMap.isPanEnabled = false
        views.radarCompassMap.isFlingEnabled = false
        views.radarCompassMap.backgroundColorOverride =
            Resources.color(requireContext(), R.color.colorSecondary)
        views.radarCompassMap.minScale = 0.001f
        views.radarCompassMap.mapCenter = gps.location
        views.radarCompassMap.metersPerProjectedUnit = 1.0
        views.radarCompassMap.latitudeScaleFactor = { 1f }
        views.radarCompassMap.projection = AzimuthalEquidistantProjection(gps.location)
        views.radarCompassMap.resolutionPixels = userPrefs.navigation.radarCompassScale
        views.radarCompassMap.setOnScaleChangeListener(true) { resolutionPixels ->
            val radiusMeters = resolutionPixels * views.radarCompassMap.width / 2f
            views.radarCompass.setRadiusDistance(Distance.meters(radiusMeters))
            userPrefs.navigation.radarCompassScale = resolutionPixels
        }

        if (!hasCompass) {
            views.radarCompass.shouldDrawDial = userPrefs.navigation.showDialTicksWhenNoCompass
            views.radarCompass.shouldDrawAzimuthIndicator = false
            views.navigationTitle.isTitleVisible = false
            views.northReferenceIndicator.isVisible = false
        } else {
            views.radarCompass.shouldDrawAzimuthIndicator =
                userPrefs.navigation.showAzimuthIndicator
        }

        refreshNavigation()
    }

    private fun toggleDestinationBearing() {
        inBackground {
            if (destination is Destination.Beacon) {
                // TODO: Prompt to cancel navigation?
                // Don't set destination bearing while navigating to a beacon
                return@inBackground
            }

            if (destination == null && hasCompass) {
                // TODO: Wait for GPS location to be up to date (show a loading indicator)
                navigator.navigateToBearing(compass.rawBearing, gps.location)
            } else {
                onMain {
                    if (isViewReady) {
                        views.navigationSheet.requestCancelNavigation()
                    }
                }
            }
        }
    }

    private fun handleShowWhenLocked() {
        activity?.let { screenLock.updateLock(it) }
    }

    fun displayAccuracyTips() {
        context ?: return

        val alerter = ImproveAccuracyAlerter(requireContext())
        alerter.alert(listOf(gps, compass))
    }

    private fun updateAstronomyData() {
        inBackground {
            if (gps.location == Coordinate.zero) {
                return@inBackground
            }

            updateAstronomyLayerCommand.execute()
        }
    }

    override fun onResume() {
        super.onResume()

        if (!isViewReady) {
            return
        }

        views.radarCompassMap.useDensityPixelsForZoom =
            !userPrefs.navigation.highDetailMode
        layers.resume(requireContext(), views.radarCompassMap, this)
        views.radarCompass.bindMapView(views.radarCompassMap)

        // Show the north reference indicator
        views.northReferenceIndicator.showDetailsOnClick = true
        views.northReferenceIndicator.useTrueNorth = useTrueNorth
        views.northReferenceIndicator.showLabel = true
        northReferenceHideTimer.once(Duration.ofSeconds(5))
        lastErrorCodes = emptyList()
        updateTimer.interval(NAVIGATION_UPDATE_INTERVAL)
        compassLayerTimer.interval(100)
        refreshNavigation()
    }

    override fun onPause() {
        super.onPause()
        loadBeaconsRunner.cancel()
        errors.reset()
        updateTimer.stop()
        compassLayerTimer.stop()
        layerSheet?.setOnDismissListener(null)
        layerSheet?.dismiss()
        if (isViewReady) {
            layers.pause(views.radarCompassMap)
        }
        northReferenceHideTimer.stop()
    }

    private fun updateNearbyBeacons() {
        inBackground {
            onIO {
                loadBeaconsRunner.skipIfRunning {
                    val destinationBeacon = (destination as? Destination.Beacon)?.beacon
                    if (!isNearbyEnabled) {
                        nearbyBeacons = listOfNotNull(destinationBeacon)
                        return@skipIfRunning
                    }

                    nearbyBeacons = (navigationService.getNearbyBeacons(
                        gps.location,
                        beacons,
                        nearbyCount,
                        8f,
                        nearbyDistance
                    ) + listOfNotNull(destinationBeacon)).distinctBy { it.id }
                }
            }
        }
    }

    private fun getDestinationBearing(): Float? {
        return destination?.let { navigator.getBearing(gps.location, it).value }
    }

    private fun getSelectedBeacon(nearby: Collection<Beacon>): Beacon? {
        return (destination as? Destination.Beacon)?.beacon ?: getFacingBeacon(nearby)
    }

    private fun getFacingBeacon(nearby: Collection<Beacon>): Beacon? {
        return navigationService.getFacingBeacon(
            gps.location,
            compass.rawBearing,
            nearby,
            declination,
            useTrueNorth
        )
    }

    private fun refreshNavigation() {
        if (!isViewReady) {
            return
        }

        updateNavigationSheet()
        views.speed.title = formatService.formatSpeed(speedometer.speed.value)
        updateCompassBearing()

        if (triggers.frequency("compass_invalidation", Duration.ofSeconds(1))) {
            views.radarCompass.invalidate()
        }

        views.altitude.title = formatService.formatDistance(
            Distance.meters(altimeter.altitude).convertTo(baseDistanceUnits)
        )
        updateLocation()

        if (
            triggers.distance(
                "astronomy",
                gps.location,
                ASTRONOMY_UPDATE_DISTANCE,
                highAccuracy = false
            ) || triggers.frequency("astronomy", ASTRONOMY_UPDATE_FREQUENCY)
        ) {
            updateAstronomyData()
        }

        updateMapAttributionIfNeeded()
        updateCompassStyle()
        updateQuickActionAvailability()
        updateDiagnosticErrors()
    }

    private fun updateNavigationSheet() {
        val currentDestination =
            destination ?: getSelectedBeacon(nearbyBeacons)?.let { Destination.Beacon(it) }
        if (currentDestination != null) {
            views.navigationSheet.updateNavigationSensorValues(
                gps.location,
                altimeter.altitude,
                speedometer.speed.value,
                declination
            )
            views.navigationSheet.show(currentDestination, destination != null)
        } else {
            views.navigationSheet.hide()
        }
    }

    private fun updateMapAttributionIfNeeded() {
        val isRadarVisible = views.radarCompass.isVisible
        if (lastAttributionLayerKey == layers.key && lastAttributionRadarVisible == isRadarVisible) {
            return
        }

        lastAttributionLayerKey = layers.key
        lastAttributionRadarVisible = isRadarVisible

        inBackground {
            if (isRadarVisible) {
                val attribution = views.radarCompassMap.getAttribution(requireContext())
                onMain {
                    if (isViewReady) {
                        views.mapAttribution.text = attribution
                        views.mapAttribution.isVisible = attribution != null
                    }
                }
            } else {
                onMain {
                    if (isViewReady) {
                        views.mapAttribution.isVisible = false
                    }
                }
            }
        }
    }

    private fun updateCompassStyle() {
        val deviceOrientation = if (clinometer.incline > -30) {
            DeviceOrientation.Orientation.Portrait
        } else {
            DeviceOrientation.Orientation.Flat
        }
        val style = styleChooser.getStyle(deviceOrientation)

        views.linearCompass.isInvisible = style != CompassStyle.Linear
        views.radarCompass.isInvisible = style != CompassStyle.Radar
        views.radarCompassMap.isInvisible = style != CompassStyle.Radar
    }

    private fun updateQuickActionAvailability() {
        // TODO: Extract this logic to the flashlight (if camera is in use)
        if (userPrefs.navigation.rightButton == Tools.QUICK_ACTION_FLASHLIGHT) {
            views.navigationTitle.rightButton.isClickable =
                !views.linearCompass.isCameraActive
        }
        if (userPrefs.navigation.leftButton == Tools.QUICK_ACTION_FLASHLIGHT) {
            views.navigationTitle.leftButton.isClickable =
                !views.linearCompass.isCameraActive
        }
    }

    private fun updateDiagnosticErrors() {
        val codes = diagnosticResults.values.flatten().distinct()
        if (codes != lastErrorCodes) {
            lastErrorCodes = codes
            errors.update(codes)
        }
    }

    private fun updateCompassBearing() {
        val bearing = compass.rawBearing

        // Azimuth
        if (hasCompass) {
            val azimuthText =
                formatService.formatDegrees(bearing, replace360 = true)
                    .padStart(4, ' ')
            val directionText = formatService.formatDirection(CompassDirection.nearest(bearing))
                .padStart(2, ' ')
            val titleText = "$azimuthText   $directionText"
            views.navigationTitle.title = titleText
        }

        // Compass
        listOf<ICompassView>(
            views.radarCompass,
            views.linearCompass
        ).forEach {
            it.azimuth = bearing
            it.declination = declination
        }

        val actualBearing = if (useTrueNorth) {
            compass.bearing
        } else {
            compass.bearing.withDeclination(declination)
        }
        views.radarCompassMap.userAzimuth = actualBearing
        views.radarCompassMap.mapAzimuth = actualBearing.value
    }

    private fun updateLocation() {
        val location = gps.location

        declination = declinationProvider.getDeclination()
        compass.declination = declination

        views.navigationTitle.subtitle = formatService.formatLocation(location)

        views.radarCompassMap.userLocationAccuracy =
            gps.horizontalAccuracy?.let { Distance.meters(it) }

        // Compass center point
        listOf<ICompassView>(
            views.radarCompass,
            views.linearCompass
        ).forEach {
            it.compassCenter = location
        }

        views.radarCompassMap.mapCenter = location
        views.radarCompassMap.userLocation = location

        views.radarCompassMap.projection = AzimuthalEquidistantProjection(location)

        updateNearbyBeacons()
    }

    private fun updateCompassLayers() {
        inBackground {
            val destBearing = getDestinationBearing()
            val destinationBeacon = (destination as? Destination.Beacon)?.beacon
            val destColor = destinationBeacon?.color ?: AppColor.Blue.color

            val direction = destBearing?.let {
                MappableBearing(it, destColor)
            }

            // Update beacon layers
            beaconCompassLayer.setBeacons(nearbyBeacons)
            beaconCompassLayer.highlight(destinationBeacon)

            // Destination
            if (destinationBeacon != null) {
                navigationCompassLayer.setDestination(destinationBeacon)
            } else if (direction != null) {
                navigationCompassLayer.setDestination(direction)
            } else {
                navigationCompassLayer.setDestination(null as MappableBearing?)
            }
        }
    }

    private fun showCalibrationDialog() {
        if (userPrefs.navigation.showCalibrationOnNavigateDialog) {
            val calibrationView = compassCalibrationView(
                requireContext(),
                ViewGroup.LayoutParams.MATCH_PARENT,
                Resources.dp(requireContext(), 200f).toInt()
            )
            var doNotAskAgain = false
            val contentView = ConstraintLayout(requireContext())
            calibrationView.id = View.generateViewId()
            contentView.addView(
                calibrationView,
                ConstraintLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Resources.dp(requireContext(), 200f).toInt()
                ).apply {
                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                }
            )
            val checkboxView = ComposeView(requireContext()).apply {
                id = View.generateViewId()
                setContent {
                    TrailSenseTheme {
                        var checked by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    checked = it
                                    doNotAskAgain = it
                                }
                            )
                            Text(
                                text = getString(R.string.do_not_ask_again),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            contentView.addView(
                checkboxView,
                ConstraintLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    topToBottom = calibrationView.id
                    topMargin = Resources.dp(requireContext(), 8f).toInt()
                }
            )
            dialog(
                getString(R.string.calibrate_compass_dialog_title),
                getString(
                    R.string.calibrate_compass_on_navigate_dialog_content,
                    getString(android.R.string.ok)
                ),
                contentView = contentView,
                cancelText = null,
                cancelOnOutsideTouch = false,
                scrollable = true
            ) { cancelled ->
                if (!cancelled && doNotAskAgain) {
                    userPrefs.navigation.showCalibrationOnNavigateDialog = false
                }
            }
        }
    }

    companion object {
        const val CACHE_CAMERA_ZOOM = "sighting_compass_camera_zoom"
        private val ASTRONOMY_UPDATE_DISTANCE = Distance.kilometers(1f).meters()
        private val ASTRONOMY_UPDATE_FREQUENCY = Duration.ofMinutes(1)
        private const val NAVIGATION_UPDATE_INTERVAL = 33L
    }
}

private data class NavigatorViews(
    val root: View,
    val navigationTitle: NavigationTitleState,
    val altitude: NavigatorDataPointState,
    val altitudeView: View,
    val speed: NavigatorDataPointState,
    val radarCompassMap: MapView,
    val radarCompass: RadarCompassView,
    val linearCompass: LinearSightingCompassView,
    val mapAttribution: MapAttributionView,
    val northReferenceIndicator: NorthReferenceBadge,
    val beaconBtn: NavigatorIconButton,
    val accuracyView: SensorStatusBadgeView,
    val navigationSheet: NavigationSheetView
)

private class NavigationTitleState {
    val leftButton = ComposeQuickActionButtonView()
    val rightButton = ComposeQuickActionButtonView()
    var title by mutableStateOf("")
    var subtitle by mutableStateOf("")
    var isTitleVisible by mutableStateOf(true)
    var onSubtitleClick by mutableStateOf<(() -> Unit)?>(null)
    var onSubtitleLongClick by mutableStateOf<(() -> Boolean)?>(null)
}

private class NavigatorDataPointState(
    private val icon: Int
) {
    var title by mutableStateOf("")

    @Composable
    fun Content() {
        DataPointContent(
            title = title,
            description = "",
            icon = icon,
            tint = MaterialTheme.colorScheme.onSurface,
            showDescription = false,
            gravity = Gravity.START,
            onDescriptionClick = null
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NavigationTitle(
    state: NavigationTitleState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        state.leftButton.Content()
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.isTitleVisible) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                text = state.subtitle,
                modifier = Modifier.combinedClickable(
                    onClick = { state.onSubtitleClick?.invoke() },
                    onLongClick = { state.onSubtitleLongClick?.invoke() }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        state.rightButton.Content()
    }
}

@Composable
private fun NavigatorHost(
    views: NavigatorViews,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { views.root }
    )
}

private fun createNavigatorViews(context: Context): NavigatorViews {
    val root = ConstraintLayout(context).apply {
        id = R.id.navigator_layout
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    val title = NavigationTitleState()
    val titleView = ComposeView(context).apply {
        id = R.id.navigation_title
        setContent {
            TrailSenseTheme {
                NavigationTitle(title)
            }
        }
    }
    root.addView(
        titleView,
        ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        }
    )

    val altitude = NavigatorDataPointState(R.drawable.ic_altitude)
    val altitudeView = ComposeView(context).apply {
        id = R.id.altitude
        foregroundGravity = Gravity.CENTER
        setContent {
            TrailSenseTheme {
                altitude.Content()
            }
        }
    }
    root.addView(
        altitudeView,
        ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToStart = R.id.speed
            topToBottom = R.id.navigation_title
            marginStart = 16.dp(context)
            marginEnd = 16.dp(context)
            topMargin = 8.dp(context)
        }
    )

    val speed = NavigatorDataPointState(R.drawable.ic_speed)
    val speedView = ComposeView(context).apply {
        id = R.id.speed
        foregroundGravity = Gravity.CENTER
        setContent {
            TrailSenseTheme {
                speed.Content()
            }
        }
    }
    root.addView(
        speedView,
        ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            startToEnd = R.id.altitude
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = R.id.altitude
            bottomToBottom = R.id.altitude
            marginEnd = 16.dp(context)
        }
    )

    val radarCompassMap = MapView(context).apply {
        id = R.id.radar_compass_map
    }
    root.addView(
        radarCompassMap,
        ConstraintLayout.LayoutParams(0, 0).apply {
            startToStart = R.id.radar_compass
            endToEnd = R.id.radar_compass
            topToTop = R.id.radar_compass
            bottomToBottom = R.id.radar_compass
        }
    )

    val radarCompass = RadarCompassView(context, null).apply {
        id = R.id.radar_compass
        visibility = View.GONE
        isClickable = false
        isFocusable = false
    }
    root.addView(
        radarCompass,
        ConstraintLayout.LayoutParams(0, 0).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToBottom = R.id.altitude
            bottomToTop = R.id.spacer
            marginStart = 16.dp(context)
            marginEnd = 16.dp(context)
            topMargin = 8.dp(context)
            dimensionRatio = "W,1:1"
            verticalBias = 0f
        }
    )

    val linearCompass = LinearSightingCompassView(context, null).apply {
        id = R.id.linear_compass
    }
    root.addView(
        linearCompass,
        ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = R.id.radar_compass
            bottomToBottom = R.id.radar_compass
        }
    )

    val mapAttribution = MapAttributionView(context).apply {
        id = R.id.map_attribution
    }
    root.addView(
        mapAttribution,
        ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToBottom = R.id.radar_compass
            marginStart = 16.dp(context)
            marginEnd = 16.dp(context)
        }
    )

    val spacer = View(context).apply {
        id = R.id.spacer
    }
    root.addView(
        spacer,
        ConstraintLayout.LayoutParams(0, 0).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToTop = R.id.bottom_details
        }
    )

    val bottomDetails = ConstraintLayout(context).apply {
        id = R.id.bottom_details
    }
    root.addView(
        bottomDetails,
        ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToTop = R.id.navigation_sheet
        }
    )

    val northReferenceIndicator = NorthReferenceBadge(context).apply {
        id = R.id.north_reference_indicator
    }
    bottomDetails.addView(
        northReferenceIndicator,
        ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            bottomMargin = context.resources.getDimensionPixelSize(R.dimen.default_bottom_margin)
        }
    )

    val beaconBtn = NavigatorIconButton(context, R.drawable.ic_beacon).apply {
        id = R.id.beaconBtn
        isClickable = true
        isFocusable = true
    }
    bottomDetails.addView(
        beaconBtn,
        ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            marginEnd = 16.dp(context)
            bottomMargin = context.resources.getDimensionPixelSize(R.dimen.default_bottom_margin)
        }
    )

    val accuracyView = SensorStatusBadgeView(context).apply {
        id = R.id.accuracy_view
        alpha = 0.6f
    }
    bottomDetails.addView(
        accuracyView,
        ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            marginStart = 16.dp(context)
            bottomMargin = context.resources.getDimensionPixelSize(R.dimen.default_bottom_margin)
        }
    )

    val navigationSheet = NavigationSheetView(context).apply {
        id = R.id.navigation_sheet
    }
    root.addView(
        navigationSheet,
        ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
    )

    return NavigatorViews(
        root = root,
        navigationTitle = title,
        altitude = altitude,
        altitudeView = altitudeView,
        speed = speed,
        radarCompassMap = radarCompassMap,
        radarCompass = radarCompass,
        linearCompass = linearCompass,
        mapAttribution = mapAttribution,
        northReferenceIndicator = northReferenceIndicator,
        beaconBtn = beaconBtn,
        accuracyView = accuracyView,
        navigationSheet = navigationSheet
    )
}

private class NavigatorIconButton(
    context: Context,
    private val icon: Int
) : AbstractComposeView(context) {
    @Composable
    override fun Content() {
        TrailSenseTheme {
            androidx.compose.material3.Surface(
                modifier = Modifier.size(56.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

private fun Int.dp(context: Context): Int {
    return Resources.dp(context, toFloat()).toInt()
}

@Preview(showBackground = true)
@Composable
private fun NavigatorHostPreview() {
    TrailSenseTheme {
        NavigatorHost(
            views = createNavigatorViews(LocalContext.current)
        )
    }
}
