package com.kylecorry.trail_sense.tools.clinometer.ui

import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.navigationBarsPadding
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.MotionEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.observe
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.luna.time.Throttle
import com.kylecorry.trail_sense.shared.extensions.asLiveData
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.andromeda.sense.clinometer.Clinometer
import com.kylecorry.andromeda.sense.clinometer.IClinometer
import com.kylecorry.andromeda.sense.orientation.DeviceOrientation
import com.kylecorry.luna.cache.TrackedState
import com.kylecorry.sol.math.trigonometry.Trigonometry.cosDegrees
import com.kylecorry.sol.math.trigonometry.Trigonometry.normalizeAngle
import com.kylecorry.sol.science.geology.AvalancheRisk
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.kylecorry.trail_sense.shared.icons.trail.Avalanche
import com.kylecorry.trail_sense.shared.icons.trail.Camera
import com.kylecorry.trail_sense.shared.icons.trail.Lock
import com.kylecorry.trail_sense.shared.icons.trail.PhonePortrait
import com.kylecorry.trail_sense.shared.icons.trail.RotateFlatToPortrait
import com.kylecorry.trail_sense.shared.icons.trail.RotateLandscapeToPortrait
import com.kylecorry.trail_sense.shared.icons.trail.Ruler
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseConnectedChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInstrumentScaffold
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseReading
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseStat
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseStatGrid
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryMarkerColor
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.getMarkdown
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.CameraPreview
import com.kylecorry.trail_sense.shared.views.CameraState
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.ARPoint
import com.kylecorry.trail_sense.tools.augmented_reality.domain.position.SphericalARPoint
import com.kylecorry.trail_sense.tools.augmented_reality.ui.ARLine
import com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityView
import com.kylecorry.trail_sense.tools.augmented_reality.ui.ARMarker
import com.kylecorry.trail_sense.tools.augmented_reality.ui.CanvasCircle
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARLineLayer
import com.kylecorry.trail_sense.tools.augmented_reality.ui.layers.ARMarkerLayer
import java.time.Duration
import java.time.Instant
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun ClinometerToolScreen(
    host: TrailSenseDestinationHost,
    onController: (ClinometerLockController?) -> Unit,
    modifier: Modifier = Modifier
) {
    val controller = remember(host) { ClinometerController(host) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(controller, lifecycleOwner) {
        controller.onCreate()
        onController(controller)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> controller.onResume()
                Lifecycle.Event.ON_PAUSE -> controller.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.onDestroy()
            onController(null)
        }
    }

    controller.Content(modifier = modifier)
}

private class ClinometerController(
    private val host: TrailSenseDestinationHost
) : ClinometerLockController {

    private var clinometerViews: ClinometerViews? =
        createClinometerViews(host.requireContext())
    private val views: ClinometerViews
        get() = clinometerViews ?: error("Clinometer views are not ready")

    private val sensorService by lazy { SensorService(host.requireContext()) }
    private val orientation by lazy { sensorService.getOrientation() }
    private val cameraClinometer by lazy { Clinometer(orientation, isAugmentedReality = true) }
    private val sideClinometer by lazy { Clinometer(orientation, isAugmentedReality = false) }
    private val deviceOrientation by lazy { sensorService.getDeviceOrientationSensor() }
    private val prefs by lazy { UserPreferences(host.requireContext()) }
    private val formatter by lazy { FormatService.getInstance(host.requireContext()) }
    private val throttle = Throttle(20)
    private val invalidCameraOrientations = listOf(
        DeviceOrientation.Orientation.Landscape,
        DeviceOrientation.Orientation.LandscapeInverse,
    )

    private val invalidSideOrientations = listOf(
        DeviceOrientation.Orientation.Flat,
        DeviceOrientation.Orientation.FlatInverse,
    )

    // Lock
    private val minimumHoldDuration = Duration.ofMillis(200)
    private val minimumHoldAngle = 0.5f
    private var lockedAngle1: Float? = null
    private var lockedIncline1: Float? = null
    private var lockedAngle2: Float? = null
    private var lockedIncline2: Float? = null
    private var lockStartTime: Instant? = null
    private var hadLock = false
    private var isHolding = false
    private val lockState = TrackedState(false)

    private var distanceAway: Distance? = null
    private var knownHeight: Distance? = null

    private var useCamera = true

    // Augmented reality
    private val markerLayer = ARMarkerLayer()
    private val lineLayer = ARLineLayer()
    private var startMarker: ARPoint? = null
    private var endMarker: ARPoint? = null

    private val clinometer: IClinometer
        get() = if (useCamera) cameraClinometer else sideClinometer

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        ClinometerHost(
            views = views,
            modifier = modifier
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    fun onCreate() {
        host.toast(host.getString(R.string.set_inclination_instructions))

        views.clinometerTitle.isLeftButtonSelected = false
        views.clinometerTitle.isRightButtonSelected = false

        views.camera.setScaleType(PreviewView.ScaleType.FILL_CENTER)
        views.camera.setShowTorch(false)
        views.camera.passThroughTouchEvents = true

        views.clinometerTitle.onLeftButtonClick = {
            if (useCamera) {
                startSideClinometer()
            } else {
                startCameraClinometer(true)
            }
        }

        views.clinometerTitle.onRightButtonClick = {
            askForHeightOrDistance()
        }

        views.onTouch = { event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                onTouchDown()
            } else if (event.action == MotionEvent.ACTION_UP) {
                onTouchUp()
            }
            true
        }

        views.arView.setLayers(listOf(lineLayer, markerLayer))
        views.arView.showReticle = false
        views.arView.showPosition = false
        views.arView.passThroughTouchEvents = true

        views.arView.bind(views.camera, host.lifecycleOwner)

        sideClinometer.asLiveData().observe(host.lifecycleOwner) { updateUI() }
        cameraClinometer.asLiveData().observe(host.lifecycleOwner) { updateUI() }
        deviceOrientation.asLiveData().observe(host.lifecycleOwner) { updateUI() }
    }

    private fun startSideClinometer() {
        views.camera.stop()
        views.arView.stop()
        views.clinometerTitle.isCameraMode = false
        views.clinometerTitle.isLeftButtonSelected = false
        useCamera = false
    }

    private fun startCameraClinometer(showAlert: Boolean) {
        host.requestCamera { hasPermission ->
            if (hasPermission) {
                useCamera = true
                views.camera.start(
                    readFrames = false, shouldStabilizePreview = false
                )
                views.arView.start(false)
                views.clinometerTitle.isCameraMode = true
                views.clinometerTitle.isLeftButtonSelected = false
            } else {
                startSideClinometer()
                if (showAlert) {
                    host.alertNoCameraPermission()
                }
            }
        }
    }

    private fun getCurrentAngle(): Float {
        // TODO: This should just be clinometer.angle
        return normalizeAngle(-clinometer.angle + 180f)
    }

    private fun askForHeightOrDistance() {
        Pickers.item(
            host.requireContext(), host.getString(R.string.measure), listOf(
                host.getString(R.string.height), host.getString(R.string.distance)
            ), when {
                distanceAway != null -> 0
                knownHeight != null -> 1
                else -> -1
            }
        ) {
            if (it != null) {
                when (it) {
                    0 -> measureHeightPrompt()
                    1 -> measureDistancePrompt()
                }
            }
        }
    }

    private fun measureHeightPrompt() {
        val units = formatter.sortDistanceUnits(DistanceUtils.hikingDistanceUnits)
        CustomUiUtils.pickDistance(
            host.requireContext(),
            units,
            distanceAway,
            host.getString(R.string.clinometer_measure_height_title)
        ) { distance, _ ->
            if (distance != null) {
                distanceAway = distance
                knownHeight = null
                views.clinometerTitle.isRightButtonSelected = true
                CustomUiUtils.disclaimer(
                    host.requireContext(),
                    host.getString(R.string.instructions),
                    host.getMarkdown(
                        R.string.clinometer_measure_height_instructions,
                        formatter.formatDistance(distance, 2, false)
                    ),
                    "pref_clinometer_measure_height_read",
                    cancelText = null
                )
            }
        }
    }

    private fun measureDistancePrompt() {
        val units = formatter.sortDistanceUnits(DistanceUtils.humanDistanceUnits)

        CustomUiUtils.pickDistance(
            host.requireContext(),
            units,
            knownHeight,
            host.getString(R.string.clinometer_measure_distance_title),
            hint = host.getString(R.string.height),
            showFeetAndInches = true
        ) { distance, _ ->
            if (distance != null) {
                knownHeight = distance
                distanceAway = null
                views.clinometerTitle.isRightButtonSelected = true

                CustomUiUtils.disclaimer(
                    host.requireContext(),
                    host.getString(R.string.instructions),
                    host.getMarkdown(
                        R.string.clinometer_measure_distance_instructions,
                        formatter.formatDistance(distance, 2, false)
                    ),
                    "pref_clinometer_measure_distance_read",
                    cancelText = null
                )
            }
        }
    }

    fun onResume() {
        if (distanceAway == null && knownHeight == null) {
            distanceAway = prefs.clinometer.baselineDistance
            views.clinometerTitle.isRightButtonSelected = distanceAway != null
        }

        if (useCamera) {
            startCameraClinometer(false)
        } else {
            startSideClinometer()
        }
    }

    fun onPause() {
        if (useCamera) {
            views.camera.stop()
            views.arView.stop()
        }
    }

    fun onDestroy() {
        views.arView.unbind()
        clinometerViews = null
    }

    private fun updateUI() {

        if (throttle.isThrottled()) {
            return
        }

        // Update the lock state
        lockState.write(lockedAngle1 != null)

        if (lockState.hasChanges) {
            views.clinometerTitle.isLocked = lockState.read()
        }

        if (!isOrientationValid() && !lockState.peek()) {
            views.clinometerInstructions.isVisible = !useCamera
            views.cameraClinometerInstructions.isVisible = useCamera
            views.cameraViewHolder.isVisible = false
            views.clinometer.isInvisible = true
            return
        }

        views.clinometerInstructions.isVisible = false
        views.cameraClinometerInstructions.isVisible = false
        views.cameraViewHolder.isVisible = useCamera
        views.clinometer.isInvisible = useCamera

        val angle = lockedAngle2 ?: (if (!isHolding) lockedAngle1 else null) ?: getCurrentAngle()
        val incline =
            lockedIncline2 ?: (if (!isHolding) lockedIncline1 else null) ?: clinometer.incline

        views.clinometer.angle = angle
        views.cameraClinometer.inclination = incline

        views.clinometerTitle.title = formatter.formatDegrees(incline)
        val slope = getSlopePercent(incline)
        // Straight up or down has no finite grade; say so rather than printing "-%".
        views.clinometerTitle.subtitle = if (!slope.isFinite() || abs(incline) >= 89.95f) {
            host.getString(R.string.clinometer_vertical)
        } else {
            host.getString(R.string.slope_amount, formatter.formatPercentage(slope))
        }

        val risk = Geology.getAvalancheRisk(incline)
        views.avalancheRisk.title = getAvalancheRiskString(risk)
        views.avalancheRisk.tone = when (risk) {
            AvalancheRisk.Low -> TrailSenseTone.Good
            AvalancheRisk.Moderate -> TrailSenseTone.Caution
            AvalancheRisk.High -> TrailSenseTone.Error
        }

        updateMeasurement()
        updateARLine()
    }

    private fun updateMeasurement() {
        val distanceAway = distanceAway
        val knownHeight = knownHeight

        when {
            distanceAway != null -> {
                views.estimatedHeight.isSet = true
                views.estimatedHeight.description = host.getString(R.string.height)
                views.estimatedHeight.title = formatter.formatDistance(
                    getHeight(distanceAway).toRelativeDistance(), 1, false
                )
            }

            knownHeight != null -> {
                views.estimatedHeight.isSet = true
                views.estimatedHeight.description = host.getString(R.string.distance)
                views.estimatedHeight.title = formatter.formatDistance(
                    getDistance(knownHeight).toRelativeDistance(), 1, false
                )
            }

            else -> {
                views.estimatedHeight.isSet = false
                views.estimatedHeight.title = host.getString(R.string.distance_unset)
            }
        }
    }

    private fun updateARLine() {
        if (startMarker != null && (isHolding || endMarker != null)) {
            lineLayer.setLines(
                listOf(
                    ARLine(
                        listOfNotNull(
                            startMarker, endMarker ?: SphericalARPoint(
                                views.arView.azimuth,
                                views.arView.inclination,
                                isTrueNorth = prefs.compass.useTrueNorth,
                                distance = distanceAway?.meters()?.value ?: 10f
                            )
                        ), Color.WHITE, 1f
                    )
                )
            )
        } else {
            lineLayer.clearLines()
        }
    }

    private fun getSlopePercent(incline: Float): Float {
        return Geology.getSlopeGrade(incline)
    }

    private fun getAvalancheRiskString(risk: AvalancheRisk): String {
        return when (risk) {
            AvalancheRisk.Low -> host.getString(R.string.low)
            AvalancheRisk.Moderate -> host.getString(R.string.moderate)
            AvalancheRisk.High -> host.getString(R.string.high)
        }
    }

    private fun isOrientationValid(): Boolean {
        val invalidOrientations = if (useCamera) {
            invalidCameraOrientations
        } else {
            invalidSideOrientations
        }

        return !invalidOrientations.contains(deviceOrientation.orientation)
    }

    private fun getBottomIncline(): Float {
        val locked1 = lockedIncline1 ?: 0f
        val locked2 = lockedIncline2 ?: clinometer.incline
        return min(locked1, locked2)
    }

    private fun getTopIncline(): Float {
        val locked1 = lockedIncline1 ?: 0f
        val locked2 = lockedIncline2 ?: clinometer.incline
        return max(locked1, locked2)
    }

    private fun getHeight(distanceAway: Distance): Distance {
        return Geology.getHeightFromInclination(
            distanceAway, getBottomIncline(), getTopIncline()
        )
    }

    private fun getDistance(height: Distance): Distance {
        return Geology.getDistanceFromInclination(
            height, getBottomIncline(), getTopIncline()
        )
    }

    override fun onTouchDown() {
        if (!isOrientationValid() || isHolding) {
            return
        }

        isHolding = true
        hadLock = lockedAngle1 != null
        clearLock()
        lockedAngle1 = getCurrentAngle()
        lockedIncline1 = clinometer.incline
        lockStartTime = Instant.now()

        // Update the UI
        views.cameraClinometer.startInclination = lockedIncline1
        views.clinometer.startAngle = lockedAngle1
        startMarker = addMarker()
    }

    override fun onTouchUp() {
        if (!isOrientationValid() || !isHolding) {
            return
        }

        // Determine if the user did a sweep or single angle
        val deltaAngle = abs(clinometer.incline - (lockedIncline1 ?: 0f))
        val deltaTime = Duration.between(lockStartTime, Instant.now())
        if (deltaAngle < minimumHoldAngle || deltaTime < minimumHoldDuration) {

            // These aren't needed for a single angle mode
            views.cameraClinometer.startInclination = null
            views.clinometer.startAngle = null

            // If there was a lock, clear it instead of setting a new one
            if (hadLock) {
                clearLock()
            }

            isHolding = false
            return
        }

        lockedAngle2 = getCurrentAngle()
        lockedIncline2 = clinometer.incline

        // Update UI
        endMarker = addMarker()

        isHolding = false
    }

    private fun clearLock() {
        lockedAngle1 = null
        lockedIncline1 = null
        lockedAngle2 = null
        lockedIncline2 = null
        lockStartTime = null

        // Update UI
        startMarker = null
        endMarker = null
        lineLayer.clearLines()
        markerLayer.clearMarkers()
        views.cameraClinometer.startInclination = null
        views.clinometer.startAngle = null
    }

    private fun addMarker(): ARPoint {
        // Distance away is distance from device to the object at 0 inclination
        // Calculate the distance away using the hypotenuse of the triangle
        val adjacent = distanceAway?.meters()?.value ?: 10f
        val hypotenuse = adjacent / cosDegrees(clinometer.incline)
        val point = SphericalARPoint(
            views.arView.azimuth,
            views.arView.inclination,
            isTrueNorth = prefs.compass.useTrueNorth,
            distance = hypotenuse,
            angularDiameter = 1f
        )
        markerLayer.addMarker(
            ARMarker(
                point, CanvasCircle(Resources.getPrimaryMarkerColor(host.requireContext()))
            )
        )
        return point
    }


}

private data class ClinometerViews(
    val clinometerTitle: ClinometerTitleState,
    val clinometer: ClinometerView,
    val cameraViewHolder: ClinometerVisibilityState,
    val camera: CameraState,
    val arView: AugmentedRealityView,
    val cameraClinometer: CameraClinometerView,
    val avalancheRisk: ClinometerDataPointState,
    val estimatedHeight: ClinometerDataPointState,
    val clinometerInstructions: ClinometerVisibilityState,
    val cameraClinometerInstructions: ClinometerVisibilityState,
    var onTouch: ((MotionEvent) -> Boolean)? = null
)

private class ClinometerVisibilityState(initialVisible: Boolean = true) {
    var isVisible by mutableStateOf(initialVisible)
}

private class ClinometerDataPointState(
    initialDescription: String,
    val icon: ImageVector
) {
    var title by mutableStateOf("")
    var description by mutableStateOf(initialDescription)
    var tone by mutableStateOf(TrailSenseTone.Neutral)
    var isSet by mutableStateOf(false)
}

private class ClinometerTitleState {
    var isCameraMode by mutableStateOf(false)
    var isLeftButtonSelected by mutableStateOf(false)
    var isRightButtonSelected by mutableStateOf(false)
    var title by mutableStateOf("")
    var subtitle by mutableStateOf("")
    var isLocked by mutableStateOf(false)
    var onLeftButtonClick by mutableStateOf<(() -> Unit)?>(null)
    var onRightButtonClick by mutableStateOf<(() -> Unit)?>(null)
}

/**
 * The reading over the dial: the incline in large SemiBold tabular figures, the slope as a pill under it, and a lock
 * badge that pops in while a reading is held.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ClinometerReading(
    state: ClinometerTitleState,
    instruction: ImageVector?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
    Column(
        modifier = Modifier.padding(horizontal = TrailSenseSpacing.xl, vertical = TrailSenseSpacing.lg),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
    ) {
        Text(
            stringResource(R.string.clinometer_incline),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            TrailSenseReading(
                value = state.title.ifEmpty { stringResource(R.string.dash) },
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f, fill = false)
            )
            AnimatedVisibility(
                visible = state.isLocked,
                enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                    scaleIn(MaterialTheme.motionScheme.fastSpatialSpec()),
                exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                    scaleOut(MaterialTheme.motionScheme.fastSpatialSpec())
            ) {
                TrailSenseShapeBadge(
                    icon = TrailIcons.Lock,
                    shape = MaterialShapes.Cookie9Sided.toShape(),
                    size = 44.dp,
                    modifier = Modifier.padding(start = TrailSenseSpacing.md)
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text(
                    text = state.subtitle.ifEmpty { stringResource(R.string.dash) },
                    style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.sm)
                )
            }
            AnimatedVisibility(visible = state.isLocked) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ) {
                    Text(
                        text = stringResource(R.string.clinometer_locked),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.sm)
                    )
                }
            }
        }
        if (instruction != null) {
            ClinometerInstruction(icon = instruction)
        }
    }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ClinometerHost(
    views: ClinometerViews,
    modifier: Modifier = Modifier
) {
    val dialLabel = stringResource(R.string.clinometer_mode_phone)
    val cameraLabel = stringResource(R.string.camera)
    val scheme = MaterialTheme.colorScheme
    TrailSenseInstrumentScaffold(
        title = stringResource(R.string.clinometer_title),
        modifier = modifier
    ) {
        // Touch-and-hold anywhere below the bar locks the reading, as before.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = TrailSenseSize.maximumContentWidth)
                .align(Alignment.TopCenter)
                .pointerInteropFilter { event ->
                    views.onTouch?.invoke(event) ?: false
                }
                .navigationBarsPadding()
                .padding(
                    start = TrailSenseSpacing.lg,
                    end = TrailSenseSpacing.lg,
                    bottom = TrailSenseSpacing.lg
                ),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
        ) {
            ClinometerReading(
                views.clinometerTitle,
                instruction = when {
                    views.clinometerInstructions.isVisible -> TrailIcons.RotateFlatToPortrait
                    views.cameraClinometerInstructions.isVisible -> TrailIcons.RotateLandscapeToPortrait
                    else -> null
                }
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                views.clinometer.Content(Modifier.fillMaxSize())
                if (views.cameraViewHolder.isVisible) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(TrailSenseShape.hero))
                            .background(scheme.primaryContainer)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            CameraPreview(
                                state = views.camera,
                                modifier = Modifier.fillMaxSize()
                            )
                            views.arView.Content(Modifier.fillMaxSize())
                            // The sight: a short primary bar with a dot in the middle, like a level vial.
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .width(72.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(scheme.primary)
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(scheme.primary)
                            )
                        }
                        views.cameraClinometer.Content(
                            Modifier
                                .width(56.dp)
                                .fillMaxHeight()
                        )
                    }
                }
                if (views.clinometerInstructions.isVisible) {
                    views.clinometer.Preview(Modifier.fillMaxSize().alpha(0.35f))
                }
            }

            TrailSenseConnectedChoice(
                options = listOf(
                    TrailSenseChoice(false, dialLabel, TrailIcons.PhonePortrait),
                    TrailSenseChoice(true, cameraLabel, TrailIcons.Camera)
                ),
                selected = views.clinometerTitle.isCameraMode,
                onSelect = { views.clinometerTitle.onLeftButtonClick?.invoke() }
            )

            // Risk takes its colour from the band the needle is in; the measurement tile opens the prompt.
            val measurement = views.estimatedHeight
            TrailSenseStatGrid(
                stats = listOf(
                    TrailSenseStat(
                        label = views.avalancheRisk.description,
                        value = views.avalancheRisk.title.ifEmpty { "--" },
                        tone = views.avalancheRisk.tone,
                        icon = TrailIcons.Avalanche
                    ),
                    TrailSenseStat(
                        label = measurement.description,
                        value = if (measurement.isSet) measurement.title else stringResource(R.string.measure),
                        supportingText = null,
                        tone = if (measurement.isSet) TrailSenseTone.Primary else TrailSenseTone.Secondary,
                        icon = TrailIcons.Ruler,
                        onClick = { views.clinometerTitle.onRightButtonClick?.invoke() }
                    )
                ),
                columns = 2
            )
        }
    }
}

private fun createClinometerViews(context: Context): ClinometerViews {
    val title = ClinometerTitleState()

    val avalancheRisk = ClinometerDataPointState(
        initialDescription = context.getString(R.string.avalanche_risk),
        icon = TrailIcons.Avalanche
    )
    val estimatedHeight = ClinometerDataPointState(
        initialDescription = context.getString(R.string.height),
        icon = TrailIcons.Ruler
    )

    val clinometer = ClinometerView()
    val cameraViewHolder = ClinometerVisibilityState(initialVisible = false)
    val camera = CameraState(context)
    val arView = AugmentedRealityView(context)
    val cameraClinometer = CameraClinometerView()

    val clinometerInstructions = ClinometerVisibilityState()
    val cameraClinometerInstructions = ClinometerVisibilityState()

    return ClinometerViews(
        clinometerTitle = title,
        clinometer = clinometer,
        cameraViewHolder = cameraViewHolder,
        camera = camera,
        arView = arView,
        cameraClinometer = cameraClinometer,
        avalancheRisk = avalancheRisk,
        estimatedHeight = estimatedHeight,
        clinometerInstructions = clinometerInstructions,
        cameraClinometerInstructions = cameraClinometerInstructions
    )
}

/** Shown while the phone is held the wrong way: a solid pill over the dimmed dial, the rotation hint inside it. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ClinometerInstruction(icon: ImageVector) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(start = TrailSenseSpacing.sm, end = TrailSenseSpacing.xl, top = TrailSenseSpacing.sm, bottom = TrailSenseSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrailSenseShapeBadge(
                icon = icon,
                shape = MaterialShapes.Cookie9Sided.toShape(),
                size = 52.dp,
                containerColor = MaterialTheme.colorScheme.onPrimary,
                contentColor = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(TrailSenseSpacing.md))
            Text(
                text = stringResource(R.string.clinometer_rotate_device),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ClinometerHostPreview() {
    TrailSenseTheme {
        ClinometerHost(
            views = createClinometerViews(androidx.compose.ui.platform.LocalContext.current)
        )
    }
}
