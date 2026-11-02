package com.kylecorry.trail_sense.tools.augmented_reality.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.camera.view.PreviewView
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.luna.time.TimerActionBehavior
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.andromeda.sense.location.Satellite
import com.kylecorry.luna.concurrency.CoroutineQueueRunner
import com.kylecorry.sol.science.astronomy.locators.Planet
import com.kylecorry.sol.science.astronomy.meteors.MeteorShower
import com.kylecorry.sol.science.astronomy.moon.MoonPhase
import com.kylecorry.sol.science.astronomy.stars.Star
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.ImproveAccuracyAlerter
import com.kylecorry.trail_sense.shared.CustomUiUtils.getCardinalDirectionColor
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.readableName
import com.kylecorry.trail_sense.shared.views.CameraPreview
import com.kylecorry.trail_sense.shared.views.CameraState
import com.kylecorry.trail_sense.shared.views.DateTimeSliderSheetState
import com.kylecorry.trail_sense.shared.views.SensorStatusBadgeView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.withId
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomySubsystem
import com.kylecorry.trail_sense.tools.astronomy.ui.format.PlanetMapper
import com.kylecorry.trail_sense.tools.augmented_reality.domain.calibration.ARCalibratorFactory
import com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance.ARGuidanceRefreshRequest
import com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance.BeaconGuidanceTarget
import com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance.ARGuidanceDisplayState
import com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance.ARGuidanceLayer
import com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance.ARGuidanceTarget
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARAstronomyLayer
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARBeaconLayer
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARGridLayer
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARLayer
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARPathLayer
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARSatelliteLayer
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.ui.IMappablePath
import com.kylecorry.trail_sense.tools.paths.map_layers.AugmentedRealityPathLayerManager
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZonedDateTime
import kotlin.math.hypot

fun openAugmentedReality(
    navController: NavController,
    mode: ARMode = ARMode.Normal,
    enableCamera: Boolean = true,
    extras: Bundle? = null
) {
    navController.navigate(
        R.id.augmentedRealityFragment, Bundle().apply {
            putLong("mode", mode.id)
            putBoolean("camera_enabled", enableCamera)
            putBundle("extras", extras)
        }
    )
}

@Composable
fun AugmentedRealityToolScreen(
    host: TrailSenseDestinationHost,
    modifier: Modifier = Modifier
) {
    val controller = remember(host) { AugmentedRealityController(host) }
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

private class AugmentedRealityController(private val host: TrailSenseDestinationHost) {

    private val views = createAugmentedRealityViews(host.requireContext())
    private val isViewReady: Boolean
        get() = isCreated

    private var mode = ARMode.Normal
    private var timeOverride: Instant? = null

    private val userPrefs by lazy { UserPreferences(host.requireContext()) }
    private val beaconRepo by lazy {
        BeaconRepo.getInstance(host.requireContext())
    }

    private val formatter by lazy { FormatService.getInstance(host.requireContext()) }
    private val planetMapper by lazy { PlanetMapper(host.requireContext()) }
    private var activeGuidanceTarget: ARGuidanceTarget? = null
    private val guidanceRefreshRunner = CoroutineQueueRunner()

    private val beaconLayer by lazy {
        ARBeaconLayer(
            host.getString(R.string.beacons),
            Distance.meters(userPrefs.augmentedReality.beaconViewDistance),
            onFocus = this::onBeaconFocused
        ) {
            if (navigator.getDestinationId() != it.id) {
                navigator.navigateTo(it)
            } else {
                navigator.cancelBeaconNavigation()
            }
            true
        }
    }

    private val astronomyLayer by lazy {
        ARAstronomyLayer(
            host.getString(R.string.astronomy),
            drawBelowHorizon = userPrefs.augmentedReality.showAstronomyBelowHorizon,
            drawStars = userPrefs.augmentedReality.showStars,
            drawConstellations = userPrefs.augmentedReality.showConstellations,
            drawLowBrightnessObjects = userPrefs.augmentedReality.showLowBrightnessObjects,
            onSunFocus = this::onSunFocused,
            onMoonFocus = this::onMoonFocused,
            onStarFocus = this::onStarFocused,
            onPlanetFocus = this::onPlanetFocused,
            onMeteorShowerFocus = this::onMeteorShowerFocused
        )
    }

    private val satelliteLayer by lazy {
        ARSatelliteLayer(
            views.arView.gps,
            this::onSatelliteFocused
        )
    }

    private val navigator by lazy { Navigator.getInstance(host.requireContext()) }

    private val gridLayer by lazy {
        ARGridLayer(
            30,
            northColor = Resources.getCardinalDirectionColor(host.requireContext()),
            horizonColor = Color.WHITE,
            labelColor = Color.WHITE,
            color = Color.WHITE.withAlpha(100),
            useTrueNorth = userPrefs.compass.useTrueNorth
        )
    }

    private val pathsLayer by lazy {
        ARPathLayer(
            userPrefs.augmentedReality.pathViewDistance,
            adjustForPathElevation = userPrefs.augmentedReality.adjustForPathElevation,
            this::onPathFocused
        )
    }
    private var pathLayerManager: AugmentedRealityPathLayerManager? = null

    private var isCameraEnabled = true

    private var lastLocation = Coordinate.zero
    private val layerManagementUpdater = CoroutineTimer(actionBehavior = TimerActionBehavior.Skip) {
        if (!isViewReady) return@CoroutineTimer
        if (!userPrefs.augmentedReality.showPathLayer) return@CoroutineTimer
        if (views.arView.location == lastLocation) return@CoroutineTimer
        lastLocation = views.arView.location
        // This is only handling the path layer for now
        val viewDistance = Distance.meters(userPrefs.augmentedReality.pathViewDistance * 2f)
        pathLayerManager?.onBoundsChanged(
            CoordinateBounds.from(
                Geofence(
                    views.arView.location,
                    viewDistance
                )
            )
        )
    }

    private val guidanceLocationUpdater =
        CoroutineTimer(actionBehavior = TimerActionBehavior.Skip) {
            if (!isViewReady) return@CoroutineTimer
            if (activeGuidanceTarget == null) return@CoroutineTimer
            refreshGuidance()
        }

    // Calibration
    private val calibrationFactory = ARCalibratorFactory()
    private val astronomyService = AstronomyService()

    private var visibleLayersOverride: List<ARLayer>? = null
    private var visibleLayers: List<ARLayer> = emptyList()
    private var isCreated = false
    private var showLayersSheet by mutableStateOf(false)
    private var showBeaconLayer by mutableStateOf(false)
    private var showPathLayer by mutableStateOf(false)
    private var showAstronomyLayer by mutableStateOf(false)
    private var showGridLayer by mutableStateOf(false)

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Content(modifier: Modifier = Modifier) {
        AugmentedRealityHost(
            views = views,
            modifier = modifier
        )

        if (showLayersSheet) {
            ModalBottomSheet(
                onDismissRequest = ::dismissLayersSheet
            ) {
                ARLayersContent(
                    showBeaconLayer = showBeaconLayer,
                    onShowBeaconLayerChange = {
                        showBeaconLayer = it
                        userPrefs.augmentedReality.showBeaconLayer = it
                    },
                    showPathLayer = showPathLayer,
                    onShowPathLayerChange = {
                        showPathLayer = it
                        userPrefs.augmentedReality.showPathLayer = it
                    },
                    showAstronomyLayer = showAstronomyLayer,
                    onShowAstronomyLayerChange = {
                        showAstronomyLayer = it
                        userPrefs.augmentedReality.showAstronomyLayer = it
                    },
                    showGridLayer = showGridLayer,
                    onShowGridLayerChange = {
                        showGridLayer = it
                        userPrefs.augmentedReality.showGridLayer = it
                    },
                    onClose = ::dismissLayersSheet,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    fun onCreate() {
        if (isCreated) {
            return
        }
        isCreated = true
        // Beacon layer setup (TODO: Move this to a layer manager)
        host.lifecycleScope.launch {
            beaconRepo.getBeacons().collect {
                beaconLayer.setBeacons(it)
            }
        }
        host.lifecycleScope.launch {
            navigator.destination.collect { destination ->
                val beaconDestination = destination as? Destination.Beacon
                beaconLayer.destination = beaconDestination
                syncBeaconGuidance(beaconDestination?.beacon)
            }
        }

        views.guidancePanel.isVisible = false
        views.arGuideCancel.onClick = {
            replaceActiveGuidanceTarget(null)
            setMode(ARMode.Normal)
        }

        views.accuracyView.setSensors(views.arView.gps, views.arView.geomagneticOrientationSensor)
        views.accuracyView.setOnClickListener { displayAccuracyTips() }

        views.camera.setScaleType(PreviewView.ScaleType.FILL_CENTER)
        views.camera.setShowTorch(false)

        views.arView.infiniteFocusWhenPointedUp = true
        views.arView.bind(views.camera, host.lifecycleOwner)

        val arguments = host.destinationArguments ?: Bundle()
        val modeId = arguments.getLong("mode", ARMode.Normal.id)
        val desiredMode = ARMode.entries.withId(modeId) ?: ARMode.Normal

        isCameraEnabled = arguments.getBoolean("camera_enabled", true)

        views.cameraToggle.onClick = {
            if (isCameraEnabled) {
                stopCamera()
            } else {
                startCamera()
            }
        }

        views.calibrateBtn.onClick = {
            startCalibration()
        }

        views.layersBtn.onClick = {
            showLayersSheet()
        }

        views.searchBtn.onClick = {
            showGuidanceLayerPicker()
        }

        views.timeBtn.onClick = {
            if (views.timeSheet.isVisible) {
                setTimeOverride(null)
                views.timeSheet.hide()
            } else {
                views.timeSheet.setTime(timeOverride)
                views.timeSheet.show()
            }
            updateTimeButtonState()
        }

        views.timeSheet.onTimeChanged = {
            setTimeOverride(it)
            updateTimeButtonState()
            refreshGuidance()
        }

        setMode(desiredMode, arguments.getBundle("extras"))

        views.calibrateBtn.onLongClick = {
            Alerts.dialog(
                host.requireContext(),
                host.getString(R.string.reset_calibration_question)
            ) { cancelled ->
                if (!cancelled) {
                    views.arView.resetCalibration()
                }
            }
        }

        views.calibrationPanel.onConfirmClick = {
            calibrate()
        }

        views.calibrationPanel.onCancelClick = {
            stopCalibration()
        }

        views.arView.setOnFocusLostListener {
            views.focusActionButton.isVisible = false
        }

        ARCalibrateDisclaimer(host.requireContext()).alert()

        if (!Sensors.hasGyroscope(host.requireContext())) {
            ARNoGyroAlert(host.requireContext()).alert()
        }
    }

    fun onResume() {
        if (!isCreated) {
            return
        }

        pathLayerManager = AugmentedRealityPathLayerManager(
            host.requireContext(),
            pathsLayer,
            shouldCorrectElevations = userPrefs.augmentedReality.adjustForPathElevation
        )
        pathLayerManager?.start()
        layerManagementUpdater.interval(100)

        views.arView.start()
        if (isCameraEnabled) {
            startCamera()
        } else {
            stopCamera()
        }

        updateLayerVisibility()
        updateExposureCompensation()
        updateTimeButtonState()
        updateGuidanceButtonState()
        guidanceLocationUpdater.interval(1000)
        refreshGuidance()
    }

    // TODO: Move this to the AR view
    private fun startCamera() {
        isCameraEnabled = true
        views.cameraToggle.setImageResource(R.drawable.ic_camera)
        views.arView.backgroundFillColor = Color.TRANSPARENT
        host.requestCamera {
            if (it) {
                views.camera.isVisible = true
                views.camera.start(
                    readFrames = false, shouldStabilizePreview = false
                )
            } else {
                views.camera.isInvisible = true
                isCameraEnabled = false
                views.cameraToggle.setImageResource(R.drawable.ic_camera_off)
                views.arView.backgroundFillColor = Color.BLACK
                host.alertNoCameraPermission()
            }
        }
    }

    private fun stopCamera() {
        views.cameraToggle.setImageResource(R.drawable.ic_camera_off)
        isCameraEnabled = false
        views.arView.backgroundFillColor = Color.BLACK
        views.camera.stop()
        views.camera.isInvisible = true
    }

    fun onPause() {
        if (!isCreated) {
            return
        }
        views.camera.stop()
        views.arView.stop()
        setTimeOverride(null)
        views.timeSheet.hide()
        pathLayerManager?.stop()
        layerManagementUpdater.stop()
        guidanceLocationUpdater.stop()
    }

    fun onDestroy() {
        onPause()
        views.camera.stop()
        views.arView.stop()
        layerManagementUpdater.stop()
        guidanceLocationUpdater.stop()
        isCreated = false
    }

    private fun applyVisibleLayers() {
        if (!isViewReady) {
            return
        }
        views.arView.setLayers(visibleLayersOverride ?: visibleLayers)
    }

    private fun updateExposureCompensation() {
        if (!isViewReady) {
            return
        }
        val isNight = AstronomySubsystem.getInstance(host.requireContext()).isNight()
        val shouldIncreaseExposureAtNight = userPrefs.augmentedReality.increaseExposureAtNight
        views.arView.setExposureCompensation(if (isNight && shouldIncreaseExposureAtNight) 0.5f else 0f)
    }

    // TODO: Extract focus formatters
    private fun onSunFocused(time: ZonedDateTime): Boolean {
        views.arView.focusText =
            host.getString(R.string.sun) + "\n" + formatter.formatRelativeDateTime(
                time,
                includeSeconds = false
            )
        return true
    }

    private fun onMoonFocused(time: ZonedDateTime, phase: MoonPhase): Boolean {
        views.arView.focusText =
            host.getString(R.string.moon) + "\n" + formatter.formatRelativeDateTime(
                time,
                includeSeconds = false
            ) + "\n${formatter.formatMoonPhase(phase.phase)} (${
                formatter.formatPercentage(
                    phase.illumination
                )
            })"
        return true
    }

    private fun onStarFocused(star: Star): Boolean {
        val constellations = astronomyService.getConstellationsForStar(star)
        val constellationString =
            constellations.joinToString("\n\n\n") { "${host.getString(R.string.constellation)}\n${it.name}" }
        views.arView.focusText =
            "${host.getString(R.string.star)}\n${star.name}\n\n\n$constellationString".trim()
        return true
    }

    private fun onPlanetFocused(planet: Planet): Boolean {
        views.arView.focusText = planetMapper.getName(planet)
        return true
    }

    private fun onMeteorShowerFocused(shower: MeteorShower): Boolean {
        views.arView.focusText = host.getString(R.string.meteor_shower) + "\n" + shower.readableName()
        return true
    }

    private fun onSatelliteFocused(satellite: Satellite): Boolean {
        views.arView.focusText = "${satellite.constellation} ${satellite.id}"
        return true
    }

    private fun onPathFocused(path: IMappablePath): Boolean {
        views.arView.focusText = path.name
        return true
    }

    private fun onBeaconFocused(beacon: Beacon): Boolean {
        val distance = hypot(
            views.arView.location.distanceTo(beacon.coordinate),
            (beacon.elevation ?: views.arView.altitude) - views.arView.altitude
        )
        val userDistance = Distance.meters(distance).convertTo(userPrefs.baseDistanceUnits)
            .toRelativeDistance()
        val formattedDistance = formatter.formatDistance(
            userDistance,
            Units.getDecimalPlaces(userDistance.units),
            strict = false
        )
        views.arView.focusText = beacon.name + "\n" + formattedDistance

        // If the beacon isn't the destination, show the navigate button
        if (navigator.getDestinationId() != beacon.id) {
            views.focusActionButton.setTextDistinct(host.getString(R.string.navigate))
            views.focusActionButton.onClick = {
                replaceActiveGuidanceTarget(BeaconGuidanceTarget(beacon))
            }
            views.focusActionButton.isVisible = true
        } else {
            views.focusActionButton.isVisible = false
        }

        return true
    }

    private fun setMode(mode: ARMode, extras: Bundle? = null) {
        val previousMode = this.mode
        this.mode = mode
        when (mode) {
            ARMode.Normal -> {
                visibleLayersOverride = null
                if (previousMode == ARMode.Astronomy) {
                    setTimeOverride(null)
                    views.timeSheet.hide()
                    updateTimeButtonState()
                }
            }

            ARMode.Astronomy -> {
                replaceActiveGuidanceTarget(null)
                visibleLayersOverride = listOf(gridLayer, astronomyLayer)
                val overrideTime = extras?.getString("time")?.let {
                    ZonedDateTime.parse(it)
                }
                overrideTime?.let {
                    setTimeOverride(it.toInstant())
                    views.timeSheet.setTime(timeOverride)
                    views.timeSheet.show()
                    updateTimeButtonState()
                }
                showAstronomyTargetPicker()
            }
        }

        applyVisibleLayers()
        updateGuidanceButtonState()
    }

    private fun showGuidanceLayerPicker() {
        val layers = currentVisibleLayers().filterIsInstance<ARGuidanceLayer>()
        if (layers.isEmpty()) {
            updateGuidanceButtonState()
            return
        }

        Pickers.item(
            host.requireContext(),
            host.getString(R.string.locate),
            layers.map { it.guidanceName }
        ) { selected ->
            if (selected == null) {
                handleGuidancePickerCancelled()
                return@item
            }

            val layer = layers.getOrNull(selected) ?: run {
                handleGuidancePickerCancelled()
                return@item
            }

            host.inBackground {
                val target = layer.pickGuidanceTarget(views.arView)
                if (target == null) {
                    handleGuidancePickerCancelled()
                    return@inBackground
                }

                replaceActiveGuidanceTarget(target)
            }
        }
    }

    private fun showAstronomyTargetPicker() {
        host.inBackground {
            val target = astronomyLayer.pickGuidanceTarget(views.arView)
            if (target == null) {
                handleGuidancePickerCancelled()
                return@inBackground
            }
            replaceActiveGuidanceTarget(target)
        }
    }

    private fun handleGuidancePickerCancelled() {
        if (mode == ARMode.Astronomy && activeGuidanceTarget == null) {
            setMode(ARMode.Normal)
        }
    }

    private fun replaceActiveGuidanceTarget(target: ARGuidanceTarget?) {
        val previousBeaconId = (activeGuidanceTarget as? BeaconGuidanceTarget)?.beacon?.id
        val newBeacon = (target as? BeaconGuidanceTarget)?.beacon

        activeGuidanceTarget = null
        views.arView.clearGuide()
        updateGuidancePanel(null)

        if (previousBeaconId != null && previousBeaconId != newBeacon?.id) {
            navigator.cancelBeaconNavigation()
        }

        activeGuidanceTarget = target

        if (newBeacon != null) {
            navigator.navigateTo(newBeacon)
        }

        updateGuidanceButtonState()
        refreshGuidance()
    }

    private fun syncBeaconGuidance(destination: Beacon?) {
        if (mode != ARMode.Normal) {
            return
        }

        val activeBeaconId = (activeGuidanceTarget as? BeaconGuidanceTarget)?.beacon?.id

        if (destination == null) {
            return
        }

        if (activeBeaconId == destination.id) {
            return
        }

        replaceActiveGuidanceTarget(BeaconGuidanceTarget(destination))
    }

    private fun refreshGuidance() {
        val target = activeGuidanceTarget

        if (target == null) {
            views.arView.clearGuide()
            updateGuidancePanel(null)
            return
        }

        host.inBackground {
            guidanceRefreshRunner.enqueue {
                tryOrLog {
                    val state = target.refresh(
                        ARGuidanceRefreshRequest(
                            host.requireContext(),
                            views.arView.location,
                            ZonedDateTime.now()
                        )
                    )
                    onMain {
                        if (!isViewReady || activeGuidanceTarget !== target) {
                            return@onMain
                        }

                        updateGuidancePanel(state.display)
                        views.arView.guideTo(state.point) {
                            // Guidance targets remain active until cancelled or replaced
                        }
                    }
                }
            }
        }
    }

    private fun updateGuidancePanel(state: ARGuidanceDisplayState?) {
        if (state == null) {
            views.guidancePanel.isVisible = false
            return
        }

        views.guidancePanel.isVisible = true
        views.arGuideName.text = state.name
        views.arGuideIcon.setState(
            icon = state.icon,
            iconRotation = state.iconRotation,
            iconTint = state.iconTint ?: Color.WHITE,
            backgroundTint = state.iconBackgroundTint ?: Color.TRANSPARENT
        )
    }

    private fun currentVisibleLayers(): List<ARLayer> {
        return visibleLayersOverride ?: visibleLayers
    }

    private fun updateGuidanceButtonState() {
        val hasGuidanceLayers = currentVisibleLayers().any { it is ARGuidanceLayer }
        views.searchBtn.isVisible = activeGuidanceTarget != null || hasGuidanceLayers
    }

    private fun showLayersSheet() {
        val prefs = userPrefs.augmentedReality
        showBeaconLayer = prefs.showBeaconLayer
        showPathLayer = prefs.showPathLayer
        showAstronomyLayer = prefs.showAstronomyLayer
        showGridLayer = prefs.showGridLayer
        showLayersSheet = true
    }

    private fun dismissLayersSheet() {
        showLayersSheet = false
        updateLayerVisibility()
    }

    private fun startCalibration() {
        views.calibrationPanel.isVisible = true
        val isSunUp = astronomyService.isSunUp(views.arView.location)
        Alerts.dialog(
            host.requireContext(),
            host.getString(R.string.calibrate),
            host.getString(
                R.string.ar_calibration_instructions,
                if (isSunUp) host.getString(R.string.sun) else host.getString(R.string.moon)
            ),
        ) { cancelled ->
            if (cancelled) {
                stopCalibration()
            }
        }
    }

    private fun calibrate() {
        host.inBackground {
            val useGyro = userPrefs.augmentedReality.useGyroOnlyAfterCalibration
            val calibrator = if (astronomyService.isSunUp(views.arView.location)) {
                calibrationFactory.getSunCalibrator(views.arView.location)
            } else {
                calibrationFactory.getMoonCalibrator(views.arView.location)
            }
            views.arView.calibrate(calibrator, useGyro)
            stopCalibration()
        }
    }

    private fun stopCalibration() {
        views.calibrationPanel.isVisible = false
    }

    private fun displayAccuracyTips() {
        val alerter = ImproveAccuracyAlerter(
            host.requireContext(),
            host.getString(R.string.ar_calibration_instructions_hint)
        )
        alerter.alert(listOf(views.arView.gps, views.arView.geomagneticOrientationSensor))
    }

    private fun updateLayerVisibility() {
        visibleLayers = listOfNotNull(
            if (userPrefs.augmentedReality.showGridLayer) gridLayer else null,
            if (userPrefs.augmentedReality.showSatelliteLayer) satelliteLayer else null,
            if (userPrefs.augmentedReality.showAstronomyLayer) astronomyLayer else null,
            if (userPrefs.augmentedReality.showPathLayer) pathsLayer else null,
            if (userPrefs.augmentedReality.showBeaconLayer) beaconLayer else null
        )
        applyVisibleLayers()
        updateGuidanceButtonState()
    }

    private fun setTimeOverride(time: Instant?) {
        timeOverride = time
        astronomyLayer.timeOverride = time?.toZonedDateTime()
    }

    private fun updateTimeButtonState() {
        val isActive = timeOverride != null || views.timeSheet.isVisible
        views.timeBtn.alpha = if (isActive) 1f else 0.7f
    }

}

private data class AugmentedRealityViews(
    val camera: CameraState,
    val arView: AugmentedRealityView,
    val cameraToggle: ArIconButton,
    val calibrateBtn: ArIconButton,
    val layersBtn: ArIconButton,
    val timeBtn: ArIconButton,
    val searchBtn: ArIconButton,
    val accuracyView: SensorStatusBadgeView,
    val focusActionButton: ArTextButton,
    val calibrationPanel: ArCalibrationPanelView,
    val guidancePanel: ArVisibilityState,
    val arGuideIcon: ArGuideIconView,
    val arGuideName: ArTextView,
    val arGuideCancel: ArIconButton,
    val timeSheet: DateTimeSliderSheetState
)

private class ArVisibilityState(initialVisible: Boolean = true) {
    var isVisible by mutableStateOf(initialVisible)
}

@Composable
private fun AugmentedRealityHost(
    views: AugmentedRealityViews,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        CameraPreview(
            state = views.camera,
            modifier = Modifier.fillMaxSize()
        )
        views.arView.Content(Modifier.fillMaxSize())

        // Top-start floating controls (calibrate, layers)
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(TrailSenseSpacing.screen),
            shape = MaterialTheme.shapes.large,
            color = ComposeColor(Color.BLACK).copy(alpha = 0.58f)
        ) {
            Column(
                modifier = Modifier.padding(TrailSenseSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
            ) {
                views.calibrateBtn.Content()
                views.layersBtn.Content()
            }
        }

        // Top-end floating controls (camera toggle, locate, time)
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(TrailSenseSpacing.screen),
            shape = MaterialTheme.shapes.large,
            color = ComposeColor(Color.BLACK).copy(alpha = 0.58f)
        ) {
            Column(
                modifier = Modifier.padding(TrailSenseSpacing.xs),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
            ) {
                views.cameraToggle.Content()
                views.searchBtn.Content()
                views.timeBtn.Content()
            }
        }

        views.accuracyView.Content(
            Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = TrailSenseSpacing.screen, bottom = 136.dp)
        )
        views.focusActionButton.Content(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 96.dp)
        )
        views.calibrationPanel.Content(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 96.dp)
        )
        if (views.guidancePanel.isVisible) {
            TrailSenseCard(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        start = TrailSenseSpacing.screen,
                        end = TrailSenseSpacing.screen,
                        bottom = if (views.timeSheet.isVisible) 96.dp else TrailSenseSpacing.screen
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    views.arGuideIcon.Content()
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = TrailSenseSpacing.lg)
                    ) {
                        Text(
                            text = stringResource(R.string.target),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Box(modifier = Modifier.padding(top = TrailSenseSpacing.xs)) {
                            views.arGuideName.Content()
                        }
                    }
                    views.arGuideCancel.Content()
                }
            }
        }
        views.timeSheet.Content(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )
    }
}

private fun createAugmentedRealityViews(context: Context): AugmentedRealityViews {
    val camera = CameraState(context)

    val arView = AugmentedRealityView(context)

    val cameraToggle = createArIconButton(R.drawable.ic_camera, R.string.camera)
    val calibrate = createArIconButton(R.drawable.ic_compass_icon, R.string.calibrate)
    val layers = createArIconButton(R.drawable.ic_layers, R.string.map_layers)
    val search = createArIconButton(R.drawable.ic_search, R.string.search).apply {
        isVisible = false
    }
    val time = createArIconButton(R.drawable.ic_tool_clock, R.string.time)
    val accuracy = SensorStatusBadgeView(context).apply {
        alpha = 0.6f
    }
    val focusAction = ArTextButton().apply {
        isVisible = false
    }
    val calibrationPanel = ArCalibrationPanelView().apply {
        isVisible = false
    }

    val guideIcon = ArGuideIconView()
    val guideCancel = createTransparentIconButton(
        R.drawable.ic_cancel,
        Resources.androidTextColorSecondary(context),
        android.R.string.cancel
    )
    val guideName = ArTextView(
        style = ArTextStyle.Title
    )
    val timeSheet = DateTimeSliderSheetState(context)

    return AugmentedRealityViews(
        camera = camera,
        arView = arView,
        cameraToggle = cameraToggle,
        calibrateBtn = calibrate,
        layersBtn = layers,
        timeBtn = time,
        searchBtn = search,
        accuracyView = accuracy,
        focusActionButton = focusAction,
        calibrationPanel = calibrationPanel,
        guidancePanel = ArVisibilityState(initialVisible = false),
        arGuideIcon = guideIcon,
        arGuideName = guideName,
        arGuideCancel = guideCancel,
        timeSheet = timeSheet
    )
}

private fun createArIconButton(icon: Int, @StringRes contentDescription: Int): ArIconButton {
    return createTransparentIconButton(icon, Color.WHITE, contentDescription)
}

private fun createTransparentIconButton(
    icon: Int,
    tint: Int,
    @StringRes contentDescription: Int
): ArIconButton {
    return ArIconButton(icon, tint, contentDescription)
}

private class ArIconButton(
    icon: Int,
    private val tint: Int,
    @StringRes private val contentDescription: Int
) {
    private var iconRes by mutableIntStateOf(icon)
    private var buttonVisible by mutableStateOf(true)
    private var alphaState by mutableStateOf(1f)
    var onClick by mutableStateOf<(() -> Unit)?>(null)
    var onLongClick by mutableStateOf<(() -> Unit)?>(null)

    var alpha: Float
        get() = alphaState
        set(value) {
            alphaState = value
        }

    var isVisible: Boolean
        get() = buttonVisible
        set(value) {
            buttonVisible = value
        }

    fun setImageResource(icon: Int) {
        iconRes = icon
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        if (!buttonVisible) {
            return
        }
        Box(
            modifier = modifier
                .size(48.dp)
                .alpha(alphaState)
                .combinedClickable(
                    onClickLabel = stringResource(contentDescription),
                    onClick = { onClick?.invoke() },
                    onLongClick = { onLongClick?.invoke() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = stringResource(contentDescription),
                tint = ComposeColor(tint),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private class ArTextButton(
    text: String = ""
) {
    private var label by mutableStateOf(text)
    private var buttonVisible by mutableStateOf(true)
    var onClick by mutableStateOf<(() -> Unit)?>(null)

    var isVisible: Boolean
        get() = buttonVisible
        set(value) {
            buttonVisible = value
        }

    fun setTextDistinct(text: String) {
        if (label != text) {
            label = text
        }
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        if (!buttonVisible) {
            return
        }
        Button(
            modifier = modifier,
            onClick = { onClick?.invoke() }
        ) {
            Text(text = label)
        }
    }
}

private class ArCalibrationPanelView {
    var onConfirmClick by mutableStateOf<(() -> Unit)?>(null)
    var onCancelClick by mutableStateOf<(() -> Unit)?>(null)
    private var panelVisible by mutableStateOf(true)

    var isVisible: Boolean
        get() = panelVisible
        set(value) {
            panelVisible = value
        }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        if (!panelVisible) {
            return
        }
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { onConfirmClick?.invoke() }) {
                Text(text = stringResource(R.string.calibrate))
            }
            Box(modifier = Modifier.width(8.dp))
            IconButton(onClick = { onCancelClick?.invoke() }) {
                Icon(
                    painter = painterResource(R.drawable.ic_cancel),
                    contentDescription = stringResource(android.R.string.cancel),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private class ArGuideIconView {
    private var icon by mutableIntStateOf(R.drawable.ic_location)
    private var iconRotation by mutableStateOf(0f)
    private var iconTint by mutableIntStateOf(Color.WHITE)
    private var backgroundTint by mutableIntStateOf(Color.TRANSPARENT)

    fun setState(
        icon: Int,
        iconRotation: Float,
        iconTint: Int,
        backgroundTint: Int
    ) {
        this.icon = icon
        this.iconRotation = iconRotation
        this.iconTint = iconTint
        this.backgroundTint = backgroundTint
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        Surface(
            modifier = modifier,
            color = ComposeColor(backgroundTint),
            contentColor = ComposeColor(iconTint),
            shape = CircleShape
        ) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(14.dp)
                        .rotate(iconRotation)
                )
            }
        }
    }
}

private class ArTextView(
    private val style: ArTextStyle = ArTextStyle.Label
) {
    var text by mutableStateOf("")

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        Text(
            modifier = modifier,
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = when (style) {
                ArTextStyle.Label -> MaterialTheme.typography.bodySmall
                ArTextStyle.Title -> MaterialTheme.typography.bodyLarge
            }
        )
    }
}

private enum class ArTextStyle {
    Label,
    Title
}

@Preview(showBackground = true)
@Composable
private fun AugmentedRealityHostPreview() {
    TrailSenseTheme {
        AugmentedRealityHost(
            views = createAugmentedRealityViews(LocalContext.current)
        )
    }
}
