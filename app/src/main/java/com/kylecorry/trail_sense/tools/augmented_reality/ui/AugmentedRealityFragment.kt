package com.kylecorry.trail_sense.tools.augmented_reality.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.navigation.NavController
import com.kylecorry.andromeda.alerts.dialog
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.luna.time.TimerActionBehavior
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.core.ui.Colors.withAlpha
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observeFlow
import com.kylecorry.andromeda.fragments.show
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
import com.kylecorry.trail_sense.shared.permissions.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.shared.readableName
import com.kylecorry.trail_sense.shared.views.CameraPreview
import com.kylecorry.trail_sense.shared.views.CameraState
import com.kylecorry.trail_sense.shared.views.DateTimeSliderSheetState
import com.kylecorry.trail_sense.shared.views.SensorStatusBadgeView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAndromedaComposeFragment
import com.kylecorry.trail_sense.shared.withId
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomySubsystem
import com.kylecorry.trail_sense.tools.astronomy.ui.format.PlanetMapper
import com.kylecorry.trail_sense.tools.augmented_reality.domain.calibration.ARCalibratorFactory
import com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance.ARBeaconGuidanceTarget
import com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance.ARGuidanceDisplayState
import com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance.ARGuidanceLayer
import com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance.ARGuidanceTarget
import com.kylecorry.trail_sense.tools.augmented_reality.ui.guidance.BeaconGuidanceTarget
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARAstronomyLayer
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARBeaconLayer
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARGridLayer
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARLayer
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARPathLayer
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARSatelliteLayer
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.navigation.ui.IMappablePath
import com.kylecorry.trail_sense.tools.paths.map_layers.AugmentedRealityPathLayerManager
import java.time.Instant
import java.time.ZonedDateTime
import kotlin.math.hypot

class AugmentedRealityFragment : TrailSenseAndromedaComposeFragment() {

    private var augmentedRealityViews: AugmentedRealityViews? = null
    private val views: AugmentedRealityViews
        get() = augmentedRealityViews ?: error("Augmented reality views are not ready")
    private val isViewReady: Boolean
        get() = augmentedRealityViews != null && view != null

    private var mode = ARMode.Normal
    private var timeOverride: Instant? = null

    private val userPrefs by lazy { UserPreferences(requireContext()) }
    private val beaconRepo by lazy {
        BeaconRepo.getInstance(requireContext())
    }

    private val formatter by lazy { FormatService.getInstance(requireContext()) }
    private val planetMapper by lazy { PlanetMapper(requireContext()) }
    private var activeGuidanceTarget: ARGuidanceTarget? = null
    private val guidanceRefreshRunner = CoroutineQueueRunner()

    private val beaconLayer by lazy {
        ARBeaconLayer(
            getString(R.string.beacons),
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
            getString(R.string.astronomy),
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

    private val navigator by lazy { Navigator.getInstance(requireContext()) }

    private val gridLayer by lazy {
        ARGridLayer(
            30,
            northColor = Resources.getCardinalDirectionColor(requireContext()),
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

    override fun beforeCreateContent() {
        augmentedRealityViews = createAugmentedRealityViews(requireContext())
    }

    @Composable
    override fun FragmentContent() {
        AugmentedRealityHost(
            views = views,
            modifier = Modifier.fillMaxSize()
        )
    }

    override fun onDestroyView() {
        augmentedRealityViews = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Beacon layer setup (TODO: Move this to a layer manager)
        observeFlow(beaconRepo.getBeacons()) {
            beaconLayer.setBeacons(it)
        }
        observeFlow(navigator.destination) {
            beaconLayer.destination = it
            syncBeaconGuidance(it)
        }

        views.guidancePanel.isVisible = false
        views.arGuideCancel.setOnClickListener {
            replaceActiveGuidanceTarget(null)
            setMode(ARMode.Normal)
        }

        views.accuracyView.setSensors(views.arView.gps, views.arView.geomagneticOrientationSensor)
        views.accuracyView.setOnClickListener { displayAccuracyTips() }

        views.camera.setScaleType(PreviewView.ScaleType.FILL_CENTER)
        views.camera.setShowTorch(false)

        views.arView.infiniteFocusWhenPointedUp = true
        views.arView.bind(views.camera)

        val arguments = requireArguments()
        val modeId = arguments.getLong("mode", ARMode.Normal.id)
        val desiredMode = ARMode.entries.withId(modeId) ?: ARMode.Normal

        isCameraEnabled = arguments.getBoolean("camera_enabled", true)

        views.cameraToggle.setOnClickListener {
            if (isCameraEnabled) {
                stopCamera()
            } else {
                startCamera()
            }
        }

        views.calibrateBtn.setOnClickListener {
            startCalibration()
        }

        views.layersBtn.setOnClickListener {
            showLayersSheet()
        }

        views.searchBtn.setOnClickListener {
            showGuidanceLayerPicker()
        }

        views.timeBtn.setOnClickListener {
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

        setMode(desiredMode, requireArguments().getBundle("extras"))

        views.calibrateBtn.setOnLongClickListener {
            dialog(getString(R.string.reset_calibration_question)) { cancelled ->
                if (!cancelled) {
                    views.arView.resetCalibration()
                }
            }
            true
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

        ARCalibrateDisclaimer(requireContext()).alert()

        if (!Sensors.hasGyroscope(requireContext())) {
            ARNoGyroAlert(requireContext()).alert()
        }
    }

    override fun onResume() {
        super.onResume()

        pathLayerManager = AugmentedRealityPathLayerManager(
            requireContext(),
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
        requestCamera {
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
                alertNoCameraPermission()
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

    override fun onPause() {
        super.onPause()
        views.camera.stop()
        views.arView.stop()
        setTimeOverride(null)
        views.timeSheet.hide()
        pathLayerManager?.stop()
        layerManagementUpdater.stop()
        guidanceLocationUpdater.stop()
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
        val isNight = AstronomySubsystem.getInstance(requireContext()).isNight()
        val shouldIncreaseExposureAtNight = userPrefs.augmentedReality.increaseExposureAtNight
        views.arView.setExposureCompensation(if (isNight && shouldIncreaseExposureAtNight) 0.5f else 0f)
    }

    // TODO: Extract focus formatters
    private fun onSunFocused(time: ZonedDateTime): Boolean {
        views.arView.focusText =
            getString(R.string.sun) + "\n" + formatter.formatRelativeDateTime(
                time,
                includeSeconds = false
            )
        return true
    }

    private fun onMoonFocused(time: ZonedDateTime, phase: MoonPhase): Boolean {
        views.arView.focusText =
            getString(R.string.moon) + "\n" + formatter.formatRelativeDateTime(
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
            constellations.joinToString("\n\n\n") { "${getString(R.string.constellation)}\n${it.name}" }
        views.arView.focusText =
            "${getString(R.string.star)}\n${star.name}\n\n\n$constellationString".trim()
        return true
    }

    private fun onPlanetFocused(planet: Planet): Boolean {
        views.arView.focusText = planetMapper.getName(planet)
        return true
    }

    private fun onMeteorShowerFocused(shower: MeteorShower): Boolean {
        views.arView.focusText = getString(R.string.meteor_shower) + "\n" + shower.readableName()
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
            views.focusActionButton.setTextDistinct(getString(R.string.navigate))
            views.focusActionButton.setOnClickListener {
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
            requireContext(),
            getString(R.string.locate),
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

            inBackground {
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
        inBackground {
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
        val previousBeaconId = (activeGuidanceTarget as? ARBeaconGuidanceTarget)?.beacon?.id
        val newBeacon = (target as? ARBeaconGuidanceTarget)?.beacon

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

        val activeBeaconId = (activeGuidanceTarget as? ARBeaconGuidanceTarget)?.beacon?.id

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

        inBackground {
            guidanceRefreshRunner.enqueue {
                tryOrLog {
                    val state = target.refresh(views.arView)
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
        val sheet = ARLayersBottomSheet()
        sheet.setOnDismissListener {
            updateLayerVisibility()
        }
        sheet.show(this)
    }

    private fun startCalibration() {
        views.calibrationPanel.isVisible = true
        val isSunUp = astronomyService.isSunUp(views.arView.location)
        dialog(
            getString(R.string.calibrate),
            getString(
                R.string.ar_calibration_instructions,
                if (isSunUp) getString(R.string.sun) else getString(R.string.moon)
            ),
        ) { cancelled ->
            if (cancelled) {
                stopCalibration()
            }
        }
    }

    private fun calibrate() {
        inBackground {
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
        context ?: return

        val alerter = ImproveAccuracyAlerter(
            requireContext(),
            getString(R.string.ar_calibration_instructions_hint)
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

    companion object {
        fun open(
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
    }

}

private data class AugmentedRealityViews(
    val root: View,
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
    val guidancePanel: ConstraintLayout,
    val arGuideIcon: ArGuideIconView,
    val arGuideName: ArTextView,
    val arGuideCancel: ArIconButton,
    val timeSheet: DateTimeSliderSheetState
)

@Composable
private fun AugmentedRealityHost(
    views: AugmentedRealityViews,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { views.root }
    )
}

private fun createAugmentedRealityViews(context: Context): AugmentedRealityViews {
    val root = ConstraintLayout(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    val camera = CameraState(context)
    val cameraPreview = ComposeView(context).apply {
        id = R.id.camera
        setContent {
            TrailSenseTheme {
                CameraPreview(
                    state = camera,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
    root.addView(
        cameraPreview,
        ConstraintLayout.LayoutParams(0, 0).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToTop = R.id.guidance_panel
        }
    )

    val arView = AugmentedRealityView(context, null).apply {
        id = R.id.ar_view
    }
    root.addView(
        arView,
        ConstraintLayout.LayoutParams(0, 0).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToTop = R.id.guidance_panel
        }
    )

    val cameraToggle = createArIconButton(context, R.id.camera_toggle, R.drawable.ic_camera)
    root.addView(
        cameraToggle,
        ConstraintLayout.LayoutParams(32.dp(context), 32.dp(context)).apply {
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            setMargins(8.dp(context), 8.dp(context), 8.dp(context), 8.dp(context))
        }
    )

    val calibrate = createArIconButton(context, R.id.calibrate_btn, R.drawable.ic_compass_icon)
    root.addView(
        calibrate,
        ConstraintLayout.LayoutParams(32.dp(context), 32.dp(context)).apply {
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            setMargins(8.dp(context), 8.dp(context), 8.dp(context), 8.dp(context))
        }
    )

    val layers = createArIconButton(context, R.id.layers_btn, R.drawable.ic_layers)
    root.addView(
        layers,
        ConstraintLayout.LayoutParams(32.dp(context), 32.dp(context)).apply {
            startToStart = R.id.calibrate_btn
            endToEnd = R.id.calibrate_btn
            topToBottom = R.id.calibrate_btn
            topMargin = 8.dp(context)
        }
    )

    val search = createArIconButton(context, R.id.search_btn, R.drawable.ic_search).apply {
        visibility = View.GONE
    }
    root.addView(
        search,
        ConstraintLayout.LayoutParams(32.dp(context), 32.dp(context)).apply {
            startToStart = R.id.camera_toggle
            endToEnd = R.id.camera_toggle
            topToBottom = R.id.camera_toggle
            topMargin = 8.dp(context)
        }
    )

    val time = createArIconButton(context, R.id.time_btn, R.drawable.ic_tool_clock)
    root.addView(
        time,
        ConstraintLayout.LayoutParams(32.dp(context), 32.dp(context)).apply {
            startToStart = R.id.camera_toggle
            endToEnd = R.id.camera_toggle
            topToBottom = R.id.search_btn
            topMargin = 8.dp(context)
        }
    )

    val accuracy = SensorStatusBadgeView(context, null).apply {
        id = R.id.accuracy_view
        alpha = 0.6f
    }
    root.addView(
        accuracy,
        ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToTop = R.id.calibration_panel
            marginStart = 16.dp(context)
            bottomMargin = 48.dp(context)
        }
    )

    val focusAction = ArTextButton(context).apply {
        id = R.id.focus_action_button
        visibility = View.GONE
    }
    root.addView(
        focusAction,
        ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToTop = R.id.calibration_panel
            bottomMargin = context.resources.getDimensionPixelSize(R.dimen.default_bottom_margin)
        }
    )

    val calibrationPanel = ArCalibrationPanelView(context).apply {
        id = R.id.calibration_panel
        visibility = View.GONE
    }
    root.addView(
        calibrationPanel,
        ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToTop = R.id.guidance_panel
            bottomMargin = context.resources.getDimensionPixelSize(R.dimen.default_bottom_margin)
        }
    )

    val guidancePanel = ConstraintLayout(context).apply {
        id = R.id.guidance_panel
        setPadding(16.dp(context), 16.dp(context), 16.dp(context), 16.dp(context))
        setBackgroundColor(resolveThemeColor(context, com.google.android.material.R.attr.colorSurfaceContainer))
        visibility = View.GONE
    }
    root.addView(
        guidancePanel,
        ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToTop = R.id.time_sheet
        }
    )

    val guideLabel = ArTextView(context).apply {
        id = R.id.ar_guide_label
        text = context.getString(R.string.target)
    }
    guidancePanel.addView(
        guideLabel,
        ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            startToStart = R.id.ar_guide_name
            endToEnd = R.id.ar_guide_name
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        }
    )

    val guideIcon = ArGuideIconView(context).apply {
        id = R.id.ar_guide_icon
    }
    guidancePanel.addView(
        guideIcon,
        ConstraintLayout.LayoutParams(24.dp(context), 24.dp(context)).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = R.id.ar_guide_label
            bottomToBottom = R.id.ar_guide_name
        }
    )

    val guideCancel = createTransparentIconButton(
        context,
        R.id.ar_guide_cancel,
        R.drawable.ic_cancel,
        Resources.androidTextColorSecondary(context)
    )
    guidancePanel.addView(
        guideCancel,
        ConstraintLayout.LayoutParams(24.dp(context), 24.dp(context)).apply {
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = R.id.ar_guide_label
            bottomToBottom = R.id.ar_guide_name
        }
    )

    val guideName = ArTextView(
        context,
        style = ArTextStyle.Title
    ).apply {
        id = R.id.ar_guide_name
    }
    guidancePanel.addView(
        guideName,
        ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            startToEnd = R.id.ar_guide_icon
            endToStart = R.id.ar_guide_cancel
            topToBottom = R.id.ar_guide_label
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            marginStart = 16.dp(context)
            marginEnd = 16.dp(context)
            topMargin = 4.dp(context)
        }
    )

    val timeSheet = DateTimeSliderSheetState(context)
    val timeSheetView = ComposeView(context).apply {
        id = R.id.time_sheet
        visibility = View.GONE
        timeSheet.view = this
        setContent {
            TrailSenseTheme {
                timeSheet.Content()
            }
        }
    }
    root.addView(
        timeSheetView,
        ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
    )

    return AugmentedRealityViews(
        root = root,
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
        guidancePanel = guidancePanel,
        arGuideIcon = guideIcon,
        arGuideName = guideName,
        arGuideCancel = guideCancel,
        timeSheet = timeSheet
    )
}

private fun createArIconButton(context: Context, id: Int, icon: Int): ArIconButton {
    return createTransparentIconButton(context, id, icon, Color.WHITE)
}

private fun createTransparentIconButton(
    context: Context,
    id: Int,
    icon: Int,
    tint: Int
): ArIconButton {
    return ArIconButton(context, icon, tint).apply {
        this.id = id
    }
}

private class ArIconButton(
    context: Context,
    icon: Int,
    private val tint: Int
) : AbstractComposeView(context) {
    private var iconRes by mutableIntStateOf(icon)

    fun setImageResource(icon: Int) {
        iconRes = icon
    }

    @Composable
    override fun Content() {
        TrailSenseTheme {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = ComposeColor(tint),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private class ArTextButton(
    context: Context,
    text: String = ""
) : AbstractComposeView(context) {
    private var label by mutableStateOf(text)

    fun setTextDistinct(text: String) {
        if (label != text) {
            label = text
        }
    }

    @Composable
    override fun Content() {
        TrailSenseTheme {
            Button(onClick = { performClick() }) {
                Text(text = label)
            }
        }
    }
}

private class ArCalibrationPanelView(context: Context) : AbstractComposeView(context) {
    var onConfirmClick by mutableStateOf<(() -> Unit)?>(null)
    var onCancelClick by mutableStateOf<(() -> Unit)?>(null)

    @Composable
    override fun Content() {
        TrailSenseTheme {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { onConfirmClick?.invoke() }) {
                    Text(text = context.getString(R.string.calibrate))
                }
                Box(modifier = Modifier.width(8.dp))
                IconButton(onClick = { onCancelClick?.invoke() }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_cancel),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

private class ArGuideIconView(context: Context) : AbstractComposeView(context) {
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
    override fun Content() {
        TrailSenseTheme {
            Surface(
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
}

private class ArTextView(
    context: Context,
    private val style: ArTextStyle = ArTextStyle.Label
) : AbstractComposeView(context) {
    var text by mutableStateOf("")

    @Composable
    override fun Content() {
        TrailSenseTheme {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = when (style) {
                    ArTextStyle.Label -> MaterialTheme.typography.bodySmall
                    ArTextStyle.Title -> MaterialTheme.typography.bodyLarge
                }
            )
        }
    }
}

private enum class ArTextStyle {
    Label,
    Title
}

private fun resolveThemeColor(context: Context, attr: Int): Int {
    val value = android.util.TypedValue()
    context.theme.resolveAttribute(attr, value, true)
    return if (value.resourceId != 0) {
        ContextCompat.getColor(context, value.resourceId)
    } else {
        value.data
    }
}

private fun Int.dp(context: Context): Int {
    return Resources.dp(context, toFloat()).toInt()
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
