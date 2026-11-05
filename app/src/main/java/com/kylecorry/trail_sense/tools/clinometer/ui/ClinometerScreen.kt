package com.kylecorry.trail_sense.tools.clinometer.ui

import androidx.compose.ui.draw.alpha
import android.content.Context
import android.graphics.Color
import com.kylecorry.trail_sense.shared.camera.CameraScaleType
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color as ComposeColor
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
import androidx.compose.animation.animateColorAsState
import com.kylecorry.trail_sense.shared.views.compose.trailSenseToneColors
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
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.markdown.MarkdownService
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.CameraPreview
import com.kylecorry.trail_sense.shared.views.CameraState
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.AppPermission
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.shared.views.compose.ScreenEffects
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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun ClinometerToolScreen(
    onController: (ClinometerLockController?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val effects = LocalScreenEffects.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel = viewModel {
        val prefs = UserPreferences(context.applicationContext)
        ClinometerViewModel(baselineDistance = { prefs.clinometer.baselineDistance })
    }
    // The sensors, dialogs and drawings (dial, sight, AR markers, camera) stay with the composition.
    val controller = remember(context, effects, lifecycleOwner, viewModel) {
        ClinometerController(context, effects, lifecycleOwner, viewModel)
    }

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
    private val context: Context,
    private val effects: ScreenEffects,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: ClinometerViewModel
) : ClinometerLockController {

    private var clinometerViews: ClinometerViews? =
        createClinometerViews(context)
    private val views: ClinometerViews
        get() = clinometerViews ?: error("Clinometer views are not ready")

    private val sensorService by lazy { SensorService(context) }
    private val orientation by lazy { sensorService.getOrientation() }
    private val cameraClinometer by lazy { Clinometer(orientation, isAugmentedReality = true) }
    private val sideClinometer by lazy { Clinometer(orientation, isAugmentedReality = false) }
    private val deviceOrientation by lazy { sensorService.getDeviceOrientationSensor() }
    private val prefs by lazy { UserPreferences(context) }
    private val formatter by lazy { FormatService.getInstance(context) }
    private val throttle = Throttle(20)
    private val invalidCameraOrientations = listOf(
        DeviceOrientation.Orientation.Landscape,
        DeviceOrientation.Orientation.LandscapeInverse,
    )

    private val invalidSideOrientations = listOf(
        DeviceOrientation.Orientation.Flat,
        DeviceOrientation.Orientation.FlatInverse,
    )

    private val lockState = TrackedState(false)

    private var useCamera: Boolean
        get() = viewModel.useCamera
        set(value) {
            viewModel.useCamera = value
        }

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

    fun onCreate() {
        effects.showMessage(context.getString(R.string.set_inclination_instructions))

        views.clinometerTitle.isLeftButtonSelected = false
        views.clinometerTitle.isRightButtonSelected = false

        views.camera.setScaleType(CameraScaleType.Fill)
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

        views.onTouchDown = ::onTouchDown
        views.onTouchUp = ::onTouchUp

        views.arView.setLayers(listOf(lineLayer, markerLayer))
        views.arView.showReticle = false
        views.arView.showPosition = false
        views.arView.passThroughTouchEvents = true

        views.arView.bind(views.camera, lifecycleOwner)

        sideClinometer.asLiveData().observe(lifecycleOwner) { updateUI() }
        cameraClinometer.asLiveData().observe(lifecycleOwner) { updateUI() }
        deviceOrientation.asLiveData().observe(lifecycleOwner) { updateUI() }
    }

    private fun startSideClinometer() {
        views.camera.stop()
        views.arView.stop()
        views.clinometerTitle.isCameraMode = false
        views.clinometerTitle.isLeftButtonSelected = false
        useCamera = false
    }

    private fun startCameraClinometer(showAlert: Boolean) {
        lifecycleOwner.lifecycleScope.launch {
            val hasPermission = effects.requestPermission(AppPermission.Camera)
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
                    effects.explainPermissionDenied(AppPermission.Camera)
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
            context, context.getString(R.string.measure), listOf(
                context.getString(R.string.height), context.getString(R.string.distance)
            ), when {
                viewModel.distanceAway != null -> 0
                viewModel.knownHeight != null -> 1
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
            context,
            units,
            viewModel.distanceAway,
            context.getString(R.string.clinometer_measure_height_title)
        ) { distance, _ ->
            if (distance != null) {
                viewModel.measureHeight(distance)
                views.clinometerTitle.isRightButtonSelected = true
                CustomUiUtils.disclaimer(
                    context,
                    context.getString(R.string.instructions),
                    markdown(
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
            context,
            units,
            viewModel.knownHeight,
            context.getString(R.string.clinometer_measure_distance_title),
            hint = context.getString(R.string.height),
            showFeetAndInches = true
        ) { distance, _ ->
            if (distance != null) {
                viewModel.measureDistance(distance)
                views.clinometerTitle.isRightButtonSelected = true

                CustomUiUtils.disclaimer(
                    context,
                    context.getString(R.string.instructions),
                    markdown(
                        R.string.clinometer_measure_distance_instructions,
                        formatter.formatDistance(distance, 2, false)
                    ),
                    "pref_clinometer_measure_distance_read",
                    cancelText = null
                )
            }
        }
    }

    private fun markdown(resId: Int, vararg formatArgs: Any?): CharSequence {
        return DependencyRegistry.get<MarkdownService>().toMarkdown(context.getString(resId, *formatArgs))
    }

    fun onResume() {
        if (viewModel.distanceAway == null && viewModel.knownHeight == null) {
            viewModel.onResume()
            views.clinometerTitle.isRightButtonSelected = viewModel.distanceAway != null
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
        lockState.write(viewModel.isLocked)

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

        val angle = viewModel.displayedAngle(getCurrentAngle())
        val incline = viewModel.displayedIncline(clinometer.incline)

        views.clinometer.angle = angle
        views.cameraClinometer.inclination = incline

        views.clinometerTitle.title = formatter.formatDegrees(incline)
        val slope = getSlopePercent(incline)
        // Straight up or down has no finite grade; say so rather than printing "-%".
        views.clinometerTitle.subtitle = if (!slope.isFinite() || abs(incline) >= 89.95f) {
            context.getString(R.string.clinometer_vertical)
        } else {
            context.getString(R.string.slope_amount, formatter.formatPercentage(slope))
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
        val distanceAway = viewModel.distanceAway
        val knownHeight = viewModel.knownHeight

        when {
            distanceAway != null -> {
                views.estimatedHeight.isSet = true
                views.estimatedHeight.description = context.getString(R.string.height)
                views.estimatedHeight.title = formatter.formatDistance(
                    viewModel.getHeight(distanceAway, clinometer.incline).toRelativeDistance(), 1, false
                )
            }

            knownHeight != null -> {
                views.estimatedHeight.isSet = true
                views.estimatedHeight.description = context.getString(R.string.distance)
                views.estimatedHeight.title = formatter.formatDistance(
                    viewModel.getDistance(knownHeight, clinometer.incline).toRelativeDistance(), 1, false
                )
            }

            else -> {
                views.estimatedHeight.isSet = false
                views.estimatedHeight.title = context.getString(R.string.distance_unset)
            }
        }
    }

    private fun updateARLine() {
        if (startMarker != null && (viewModel.isHolding || endMarker != null)) {
            lineLayer.setLines(
                listOf(
                    ARLine(
                        listOfNotNull(
                            startMarker, endMarker ?: SphericalARPoint(
                                views.arView.azimuth,
                                views.arView.inclination,
                                isTrueNorth = prefs.compass.useTrueNorth,
                                distance = viewModel.distanceAway?.meters()?.value ?: 10f
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
            AvalancheRisk.Low -> context.getString(R.string.low)
            AvalancheRisk.Moderate -> context.getString(R.string.moderate)
            AvalancheRisk.High -> context.getString(R.string.high)
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

    override fun onTouchDown() {
        if (!isOrientationValid()) {
            return
        }
        if (!viewModel.startHold(getCurrentAngle(), clinometer.incline)) {
            return
        }
        clearLockDrawings()
        views.cameraClinometer.startInclination = viewModel.lockedIncline1
        views.clinometer.startAngle = viewModel.lockedAngle1
        startMarker = addMarker()
    }

    override fun onTouchUp() {
        if (!isOrientationValid()) {
            return
        }
        when (viewModel.endHold(getCurrentAngle(), clinometer.incline)) {
            ClinometerHoldResult.None -> Unit
            ClinometerHoldResult.Sweep -> endMarker = addMarker()
            ClinometerHoldResult.SingleAngle -> {
                // These aren't needed for a single angle
                views.cameraClinometer.startInclination = null
                views.clinometer.startAngle = null
            }

            ClinometerHoldResult.Cleared -> clearLockDrawings()
        }
    }

    private fun clearLockDrawings() {
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
        val adjacent = viewModel.distanceAway?.meters()?.value ?: 10f
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
                point, CanvasCircle(Resources.getPrimaryMarkerColor(context))
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
    var onTouchDown: (() -> Unit)? = null,
    var onTouchUp: (() -> Unit)? = null
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
 * The reading right under the dial: the incline as the one display value and the slope as the verdict. A quiet
 * lock line appears while a reading is held.
 */
@Composable
private fun ClinometerReading(
    state: ClinometerTitleState,
    showVerdict: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
    ) {
        Text(
            text = if (showVerdict) state.title.ifEmpty { stringResource(R.string.dash) } else stringResource(R.string.dash),
            style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Text(
            text = if (showVerdict && state.subtitle.isNotEmpty()) {
                state.subtitle
            } else {
                stringResource(R.string.clinometer_waiting)
            },
            style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        AnimatedVisibility(
            visible = state.isLocked,
            enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
            exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = TrailIcons.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(TrailSenseSpacing.xs))
                Text(
                    text = stringResource(R.string.clinometer_locked),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
    // One question: how steep is this slope?
    // How it is measured comes first (the phone's edge, or sighting through the camera), then the one live object,
    // the angle and its slope under it, and what that angle means as one short list.
    val scheme = MaterialTheme.colorScheme
    TrailSenseInstrumentScaffold(
        title = stringResource(R.string.clinometer_title),
        largeTitle = true,
        modifier = modifier
    ) {
        // Tap or hold anywhere below the bar to lock the reading, as before.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = TrailSenseSize.maximumContentWidth)
                .align(Alignment.TopCenter)
                .lockOnHold(
                    onDown = { views.onTouchDown?.invoke() },
                    onUp = { views.onTouchUp?.invoke() }
                )
                .padding(
                    start = TrailSenseSpacing.lg,
                    end = TrailSenseSpacing.lg,
                    bottom = TrailSenseSpacing.lg
                ),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
        ) {
            TrailSenseConnectedChoice(
                options = listOf(
                    TrailSenseChoice(false, dialLabel, TrailIcons.PhonePortrait),
                    TrailSenseChoice(true, cameraLabel, TrailIcons.Camera)
                ),
                selected = views.clinometerTitle.isCameraMode,
                onSelect = { views.clinometerTitle.onLeftButtonClick?.invoke() }
            )

            val instruction = when {
                views.clinometerInstructions.isVisible -> TrailIcons.RotateFlatToPortrait
                views.cameraClinometerInstructions.isVisible -> TrailIcons.RotateLandscapeToPortrait
                else -> null
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                views.clinometer.Content(Modifier.fillMaxSize())
                if (views.cameraViewHolder.isVisible) {
                    // The sight: the camera in one rounded window, a level line across it with a ring at the centre.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(TrailSenseShape.groupOuter))
                            .background(scheme.surfaceContainer)
                    ) {
                        CameraPreview(
                            state = views.camera,
                            showZoomSlider = false,
                            modifier = Modifier.fillMaxSize()
                        )
                        views.arView.Content(Modifier.fillMaxSize())
                        ClinometerSight(
                            inclination = views.cameraClinometer.inclination,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                if (views.clinometerInstructions.isVisible) {
                    views.clinometer.Preview(Modifier.fillMaxSize().alpha(0.4f))
                }
            }

            // Held the wrong way: one hint line takes the reading's place. Measuring: the angle and its slope.
            if (instruction != null) {
                ClinometerInstruction(
                    icon = instruction,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                ClinometerReading(views.clinometerTitle, showVerdict = true)
            }

            // What the angle means: the avalanche risk of this slope, and the height (or distance) it gives.
            val risk = views.avalancheRisk
            val measurement = views.estimatedHeight
            val showRisk = instruction == null && risk.title.isNotEmpty()
            Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
                if (showRisk) {
                    TrailSenseListItem(
                        title = risk.description,
                        icon = rememberVectorPainter(risk.icon),
                        position = TrailSenseGroupPosition(isFirst = true, isLast = false),
                        trailing = {
                            // The risk as a role chip, so High reads as a warning on dark and light alike.
                            val (chip, onChip) = when (risk.tone) {
                                TrailSenseTone.Error -> scheme.errorContainer to scheme.onErrorContainer
                                TrailSenseTone.Caution -> scheme.tertiaryContainer to scheme.onTertiaryContainer
                                else -> scheme.secondaryContainer to scheme.onSecondaryContainer
                            }
                            androidx.compose.material3.Surface(
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = chip,
                                contentColor = onChip
                            ) {
                                Text(
                                    text = risk.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(
                                        horizontal = TrailSenseSpacing.md,
                                        vertical = TrailSenseSpacing.xs
                                    )
                                )
                            }
                        }
                    )
                }
                TrailSenseListItem(
                    title = measurement.description,
                    icon = rememberVectorPainter(TrailIcons.Ruler),
                    position = TrailSenseGroupPosition(isFirst = !showRisk, isLast = true),
                    onClick = { views.clinometerTitle.onRightButtonClick?.invoke() },
                    trailing = {
                        Text(
                            text = if (measurement.isSet) measurement.title else stringResource(R.string.measure),
                            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                            fontWeight = FontWeight.SemiBold,
                            color = if (measurement.isSet) scheme.onSurface else scheme.primary
                        )
                    }
                )
            }
        }
    }
}

/**
 * The camera's sight: a primary level line across the picture, broken by a ring at the centre where you aim. In
 * the corner, the slope seen from the side: a small protractor with the horizon, the line of sight at the measured
 * angle and the wedge between them filled, so the angle is drawn, not only stated. The avalanche band (30 to 45
 * degrees) is marked in tertiary on its rim, and the sight swings to the angle on a spring.
 */
@Composable
private fun ClinometerSight(inclination: Float, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val color = scheme.primary
    val band = scheme.tertiary
    // A faint dark edge so the sight reads on a bright sky as well as on dark ground.
    val halo = ComposeColor.Black.copy(alpha = 0.25f)
    val scrim = ComposeColor.Black.copy(alpha = 0.45f)
    val chalk = ComposeColor.White
    val angle by androidx.compose.animation.core.animateFloatAsState(
        inclination.coerceIn(-90f, 90f),
        androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        ),
        label = "sightAngle"
    )
    androidx.compose.foundation.Canvas(modifier) {
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        val ring = 14.dp.toPx()
        val gap = ring + 8.dp.toPx()
        val inset = 24.dp.toPx()
        val stroke = 3.dp.toPx()
        listOf(halo to stroke + 2.dp.toPx(), color to stroke).forEach { (c, w) ->
            drawLine(
                c,
                androidx.compose.ui.geometry.Offset(inset, center.y),
                androidx.compose.ui.geometry.Offset(center.x - gap, center.y),
                w,
                androidx.compose.ui.graphics.StrokeCap.Round
            )
            drawLine(
                c,
                androidx.compose.ui.geometry.Offset(center.x + gap, center.y),
                androidx.compose.ui.geometry.Offset(size.width - inset, center.y),
                w,
                androidx.compose.ui.graphics.StrokeCap.Round
            )
            drawCircle(c, ring, center, style = androidx.compose.ui.graphics.drawscope.Stroke(w))
        }
        drawCircle(color, 3.dp.toPx(), center)

        // The side view: a half-disc protractor in the lower left, facing the slope.
        val radius = min(size.height * 0.22f, 64.dp.toPx())
        val pivot = androidx.compose.ui.geometry.Offset(16.dp.toPx(), size.height - 16.dp.toPx() - radius)
        val box = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f)
        val topLeft = pivot - androidx.compose.ui.geometry.Offset(radius, radius)
        drawArc(scrim, -90f, 180f, true, topLeft, box)
        // The avalanche band, above and below the horizon.
        listOf(-45f, 30f).forEach { start ->
            drawArc(
                band,
                start,
                15f,
                false,
                topLeft + androidx.compose.ui.geometry.Offset(2.dp.toPx(), 2.dp.toPx()),
                androidx.compose.ui.geometry.Size(box.width - 4.dp.toPx(), box.height - 4.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(4.dp.toPx())
            )
        }
        // The wedge from the horizon to the line of sight (screen angles run clockwise, so up is negative).
        drawArc(color.copy(alpha = 0.45f), min(0f, -angle), kotlin.math.abs(angle), true, topLeft, box)
        drawLine(
            chalk.copy(alpha = 0.7f),
            pivot,
            pivot + androidx.compose.ui.geometry.Offset(radius, 0f),
            2.dp.toPx(),
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))
        )
        val radians = Math.toRadians(-angle.toDouble())
        val tip = pivot + androidx.compose.ui.geometry.Offset(
            (radius * kotlin.math.cos(radians)).toFloat(),
            (radius * kotlin.math.sin(radians)).toFloat()
        )
        drawLine(color, pivot, tip, stroke, androidx.compose.ui.graphics.StrokeCap.Round)
        drawCircle(color, 4.dp.toPx(), pivot)
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

/**
 * A touch anywhere that no control takes: [onDown] when the finger lands, [onUp] when the last finger lifts (also
 * when something else took the rest of the gesture, so a hold never stays open).
 */
private fun Modifier.lockOnHold(onDown: () -> Unit, onUp: () -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown()
        onDown()
        do {
            val event = awaitPointerEvent()
        } while (event.changes.any { it.pressed })
        onUp()
    }
}

/** Shown while the phone is held the wrong way: one plain hint line (how to hold it) under the resting dial. */
@Composable
private fun ClinometerInstruction(icon: ImageVector, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(vertical = TrailSenseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(TrailSenseSpacing.sm))
        Text(
            text = stringResource(R.string.clinometer_rotate_device),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
