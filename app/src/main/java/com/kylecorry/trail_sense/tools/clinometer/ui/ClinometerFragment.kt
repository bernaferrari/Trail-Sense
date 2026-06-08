package com.kylecorry.trail_sense.tools.clinometer.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.luna.time.Throttle
import com.kylecorry.andromeda.fragments.observe
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
import com.kylecorry.trail_sense.shared.permissions.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.CameraPreview
import com.kylecorry.trail_sense.shared.views.CameraState
import com.kylecorry.trail_sense.shared.views.DataPointContent
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment
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

class ClinometerFragment : TrailSenseComposeFragment() {

    private var clinometerViews: ClinometerViews? = null
    private val views: ClinometerViews
        get() = clinometerViews ?: error("Clinometer views are not ready")

    private val sensorService by lazy { SensorService(requireContext()) }
    private val orientation by lazy { sensorService.getOrientation() }
    private val cameraClinometer by lazy { Clinometer(orientation, isAugmentedReality = true) }
    private val sideClinometer by lazy { Clinometer(orientation, isAugmentedReality = false) }
    private val deviceOrientation by lazy { sensorService.getDeviceOrientationSensor() }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val formatter by lazy { FormatService.getInstance(requireContext()) }
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

    override fun beforeCreateContent() {
        clinometerViews = createClinometerViews(requireContext())
    }

    @Composable
    override fun FragmentContent() {
        ClinometerHost(
            views = views,
            modifier = Modifier.fillMaxSize()
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toast(getString(R.string.set_inclination_instructions))

        views.clinometerTitle.isLeftButtonSelected = false
        views.clinometerTitle.isRightButtonSelected = false

        views.cameraViewHolder.clipToOutline = true
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

        views.root.setOnTouchListener { _, event ->
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

        views.arView.bind(views.camera)

        observe(sideClinometer) { updateUI() }
        observe(cameraClinometer) { updateUI() }
        observe(deviceOrientation) { updateUI() }
    }

    private fun startSideClinometer() {
        views.camera.stop()
        views.arView.stop()
        views.clinometerTitle.leftButtonIcon = R.drawable.ic_camera
        views.clinometerTitle.isLeftButtonSelected = false
        useCamera = false
    }

    private fun startCameraClinometer(showAlert: Boolean) {
        requestCamera { hasPermission ->
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
                    alertNoCameraPermission()
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
            requireContext(), getString(R.string.measure), listOf(
                getString(R.string.height), getString(R.string.distance)
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
            requireContext(),
            units,
            distanceAway,
            getString(R.string.clinometer_measure_height_title)
        ) { distance, _ ->
            if (distance != null) {
                distanceAway = distance
                knownHeight = null
                views.clinometerTitle.isRightButtonSelected = true
                CustomUiUtils.disclaimer(
                    requireContext(),
                    getString(R.string.instructions),
                    getMarkdown(
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
            requireContext(),
            units,
            knownHeight,
            getString(R.string.clinometer_measure_distance_title),
            hint = getString(R.string.height),
            showFeetAndInches = true
        ) { distance, _ ->
            if (distance != null) {
                knownHeight = distance
                distanceAway = null
                views.clinometerTitle.isRightButtonSelected = true

                CustomUiUtils.disclaimer(
                    requireContext(),
                    getString(R.string.instructions),
                    getMarkdown(
                        R.string.clinometer_measure_distance_instructions,
                        formatter.formatDistance(distance, 2, false)
                    ),
                    "pref_clinometer_measure_distance_read",
                    cancelText = null
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
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

    override fun onPause() {
        super.onPause()
        if (useCamera) {
            views.camera.stop()
            views.arView.stop()
        }
    }

    override fun onDestroyView() {
        views.arView.unbind()
        clinometerViews = null
        super.onDestroyView()
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
            getString(R.string.slope_amount, formatter.formatPercentage(getSlopePercent(incline)))

        views.avalancheRisk.title = getAvalancheRiskString(Geology.getAvalancheRisk(incline))

        updateMeasurement()
        updateARLine()
    }

    private fun updateMeasurement() {
        val distanceAway = distanceAway
        val knownHeight = knownHeight

        when {
            distanceAway != null -> {
                views.estimatedHeight.description = getString(R.string.height)
                views.estimatedHeight.title = formatter.formatDistance(
                    getHeight(distanceAway).toRelativeDistance(), 1, false
                )
            }

            knownHeight != null -> {
                views.estimatedHeight.description = getString(R.string.distance)
                views.estimatedHeight.title = formatter.formatDistance(
                    getDistance(knownHeight).toRelativeDistance(), 1, false
                )
            }

            else -> {
                views.estimatedHeight.title = getString(R.string.distance_unset)
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
            AvalancheRisk.Low -> getString(R.string.low)
            AvalancheRisk.Moderate -> getString(R.string.moderate)
            AvalancheRisk.High -> getString(R.string.high)
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

    fun onTouchDown() {
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

    fun onTouchUp() {
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
                point, CanvasCircle(Resources.getPrimaryMarkerColor(requireContext()))
            )
        )
        return point
    }


}

private data class ClinometerViews(
    val root: View,
    val clinometerTitle: ClinometerTitleState,
    val clinometer: ClinometerView,
    val cameraViewHolder: View,
    val camera: CameraState,
    val arView: AugmentedRealityView,
    val cameraClinometer: CameraClinometerView,
    val avalancheRisk: ClinometerDataPointState,
    val estimatedHeight: ClinometerDataPointState,
    val clinometerInstructions: ClinometerInstructionView,
    val cameraClinometerInstructions: ClinometerInstructionView
)

private class ClinometerDataPointState(
    initialDescription: String,
    private val icon: Int
) {
    var title by mutableStateOf("")
    var description by mutableStateOf(initialDescription)

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
    Row(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 8.dp),
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
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                if (state.isLocked) {
                    Icon(
                        painter = painterResource(R.drawable.lock),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(24.dp)
                    )
                }
            }
            Text(
                text = state.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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

@Composable
private fun ClinometerTitleButton(
    icon: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun ClinometerHost(
    views: ClinometerViews,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { views.root }
    )
}

private fun createClinometerViews(context: Context): ClinometerViews {
    val root = ConstraintLayout(context).apply {
        id = R.id.incline_container
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    val title = ClinometerTitleState()
    val titleView = ComposeView(context).apply {
        id = R.id.clinometer_title
        setContent {
            TrailSenseTheme {
                ClinometerTitle(title)
            }
        }
    }
    root.addView(
        titleView,
        ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        }
    )

    val avalancheRisk = ClinometerDataPointState(
        initialDescription = context.getString(R.string.avalanche_risk),
        icon = R.drawable.avalanche
    )
    val avalancheRiskView = ComposeView(context).apply {
        id = R.id.avalanche_risk
        setContent {
            TrailSenseTheme {
                avalancheRisk.Content()
            }
        }
    }
    val estimatedHeight = ClinometerDataPointState(
        initialDescription = context.getString(R.string.height),
        icon = R.drawable.ruler
    )
    val estimatedHeightView = ComposeView(context).apply {
        id = R.id.estimated_height
        setContent {
            TrailSenseTheme {
                estimatedHeight.Content()
            }
        }
    }
    root.addView(
        avalancheRiskView,
        ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToStart = R.id.estimated_height
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            marginStart = 16.dp(context)
            marginEnd = 16.dp(context)
            bottomMargin = 32.dp(context)
        }
    )
    root.addView(
        estimatedHeightView,
        ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            startToEnd = R.id.avalanche_risk
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = R.id.avalanche_risk
            bottomToBottom = R.id.avalanche_risk
            marginEnd = 16.dp(context)
        }
    )

    val instrumentFrame = ConstraintLayout(context).apply {
        id = View.generateViewId()
    }
    root.addView(
        instrumentFrame,
        ConstraintLayout.LayoutParams(0, 0).apply {
            topToBottom = R.id.clinometer_title
            bottomToTop = R.id.avalanche_risk
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            dimensionRatio = "1:1"
            setMargins(16.dp(context), 16.dp(context), 16.dp(context), 16.dp(context))
        }
    )

    val clinometer = ClinometerView(context).apply {
        id = R.id.clinometer
    }
    instrumentFrame.addView(
        clinometer,
        ConstraintLayout.LayoutParams(0, 0).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
    )

    val cameraViewHolder = ConstraintLayout(context).apply {
        id = R.id.camera_view_holder
        visibility = View.GONE
        background = ContextCompat.getDrawable(context, R.drawable.rounded_rectangle)
        backgroundTintList = ColorStateList.valueOf(Resources.color(context, R.color.colorSecondary))
        clipToOutline = true
    }
    val cameraFrame = ConstraintLayout(context).apply {
        id = View.generateViewId()
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
    val arView = AugmentedRealityView(context).apply {
        id = R.id.ar_view
    }
    val reticle = View(context).apply {
        setBackgroundColor(ContextCompat.getColor(context, R.color.orange_40))
    }
    cameraFrame.addView(
        cameraPreview,
        ConstraintLayout.LayoutParams(0, 0).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
    )
    cameraFrame.addView(
        arView,
        ConstraintLayout.LayoutParams(0, 0).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
    )
    cameraFrame.addView(
        reticle,
        ConstraintLayout.LayoutParams(60.dp(context), 1.dp(context)).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
    )
    val cameraClinometer = CameraClinometerView(context).apply {
        id = R.id.camera_clinometer
    }
    cameraViewHolder.addView(
        cameraFrame,
        ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToStart = R.id.camera_clinometer
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
    )
    cameraViewHolder.addView(
        cameraClinometer,
        ConstraintLayout.LayoutParams(50.dp(context), ViewGroup.LayoutParams.MATCH_PARENT).apply {
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
    )
    instrumentFrame.addView(
        cameraViewHolder,
        ConstraintLayout.LayoutParams(0, 0).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
    )

    val clinometerInstructions = createInstructionView(
        context,
        R.id.clinometer_instructions,
        R.drawable.ic_rotate_flat_to_portrait
    )
    val cameraClinometerInstructions = createInstructionView(
        context,
        R.id.camera_clinometer_instructions,
        R.drawable.ic_rotate_landscape_to_portrait
    )
    instrumentFrame.addView(
        clinometerInstructions,
        ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
    )
    instrumentFrame.addView(
        cameraClinometerInstructions,
        ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
    )

    return ClinometerViews(
        root = root,
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

private fun createInstructionView(
    context: Context,
    id: Int,
    icon: Int
): ClinometerInstructionView {
    return ClinometerInstructionView(context, icon).apply {
        this.id = id
    }
}

private class ClinometerInstructionView(
    context: Context,
    private val icon: Int
) : AbstractComposeView(context) {
    @Composable
    override fun Content() {
        TrailSenseTheme {
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
    }
}

private fun Int.dp(context: Context): Int {
    return Resources.dp(context, toFloat()).toInt()
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
