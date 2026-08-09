package com.kylecorry.trail_sense.tools.clinometer.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.Gravity
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.kylecorry.andromeda.pickers.Pickers
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
import com.kylecorry.trail_sense.shared.views.DataPointContent
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
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
        views.clinometerTitle.leftButtonIcon = R.drawable.ic_camera
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
                views.clinometerTitle.leftButtonIcon = R.drawable.ic_phone_portrait
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
        views.clinometerTitle.subtitle =
            host.getString(R.string.slope_amount, formatter.formatPercentage(getSlopePercent(incline)))

        views.avalancheRisk.title = getAvalancheRiskString(Geology.getAvalancheRisk(incline))

        updateMeasurement()
        updateARLine()
    }

    private fun updateMeasurement() {
        val distanceAway = distanceAway
        val knownHeight = knownHeight

        when {
            distanceAway != null -> {
                views.estimatedHeight.description = host.getString(R.string.height)
                views.estimatedHeight.title = formatter.formatDistance(
                    getHeight(distanceAway).toRelativeDistance(), 1, false
                )
            }

            knownHeight != null -> {
                views.estimatedHeight.description = host.getString(R.string.distance)
                views.estimatedHeight.title = formatter.formatDistance(
                    getDistance(knownHeight).toRelativeDistance(), 1, false
                )
            }

            else -> {
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
    private val icon: Int
) {
    var title by mutableStateOf("")
    var description by mutableStateOf(initialDescription)

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        Box(modifier = modifier) {
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

private class ClinometerTitleState {
    var leftButtonIcon by mutableIntStateOf(R.drawable.ic_camera)
    var rightButtonIcon by mutableIntStateOf(R.drawable.ruler)
    var isLeftButtonSelected by mutableStateOf(false)
    var isRightButtonSelected by mutableStateOf(false)
    var title by mutableStateOf("")
    var subtitle by mutableStateOf("")
    var isLocked by mutableStateOf(false)
    var onLeftButtonClick by mutableStateOf<(() -> Unit)?>(null)
    var onRightButtonClick by mutableStateOf<(() -> Unit)?>(null)
}

@Composable
private fun ClinometerTitle(
    state: ClinometerTitleState,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = TrailSenseSpacing.md,
                vertical = TrailSenseSpacing.lg
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClinometerTitleButton(
                icon = state.leftButtonIcon,
                isSelected = state.isLeftButtonSelected,
                onClick = { state.onLeftButtonClick?.invoke() }
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = TrailSenseSpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                    if (state.isLocked) {
                        Icon(
                            painter = painterResource(R.drawable.lock),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .padding(start = TrailSenseSpacing.sm)
                                .size(24.dp)
                        )
                    }
                }
                Text(
                    text = state.subtitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
            ClinometerTitleButton(
                icon = state.rightButtonIcon,
                isSelected = state.isRightButtonSelected,
                onClick = { state.onRightButtonClick?.invoke() }
            )
        }
    }
}

@Composable
private fun ClinometerTitleButton(
    icon: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val iconTint = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(
        shape = CircleShape,
        color = containerColor,
        modifier = Modifier.size(40.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ClinometerHost(
    views: ClinometerViews,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.pointerInteropFilter { event ->
            views.onTouch?.invoke(event) ?: false
        }
    ) {
        ClinometerTitle(
            views.clinometerTitle,
            modifier = Modifier.padding(
                start = TrailSenseSpacing.lg,
                end = TrailSenseSpacing.lg,
                top = TrailSenseSpacing.lg
            )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(TrailSenseSpacing.lg),
            contentAlignment = Alignment.Center
        ) {
            views.clinometer.Content(Modifier.fillMaxSize())
            if (views.cameraViewHolder.isVisible) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ComposeColor(Resources.color(LocalContext.current, R.color.colorSecondary)))
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
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .width(60.dp)
                                .height(1.dp)
                                .background(ComposeColor(Resources.color(LocalContext.current, R.color.orange_40)))
                        )
                    }
                    views.cameraClinometer.Content(
                        Modifier
                            .width(50.dp)
                            .fillMaxHeight()
                    )
                }
            }
            if (views.clinometerInstructions.isVisible) {
                ClinometerInstruction(icon = R.drawable.ic_rotate_flat_to_portrait)
            }
            if (views.cameraClinometerInstructions.isVisible) {
                ClinometerInstruction(icon = R.drawable.ic_rotate_landscape_to_portrait)
            }
        }
        TrailSenseCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = TrailSenseSpacing.lg,
                    end = TrailSenseSpacing.lg,
                    bottom = TrailSenseSpacing.xl
                )
        ) {
            Text(
                text = LocalContext.current.getString(R.string.measure),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(TrailSenseSpacing.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                views.avalancheRisk.Content(Modifier.weight(1f))
                views.estimatedHeight.Content(Modifier.weight(1f))
            }
        }
    }
}

private fun createClinometerViews(context: Context): ClinometerViews {
    val title = ClinometerTitleState()

    val avalancheRisk = ClinometerDataPointState(
        initialDescription = context.getString(R.string.avalanche_risk),
        icon = R.drawable.avalanche
    )
    val estimatedHeight = ClinometerDataPointState(
        initialDescription = context.getString(R.string.height),
        icon = R.drawable.ruler
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

@Composable
private fun ClinometerInstruction(icon: Int) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = context.getString(R.string.clinometer_rotate_device),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
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
