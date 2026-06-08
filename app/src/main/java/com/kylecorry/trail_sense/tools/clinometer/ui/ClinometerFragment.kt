package com.kylecorry.trail_sense.tools.clinometer.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.graphics.Typeface
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.luna.time.Throttle
import com.kylecorry.andromeda.core.ui.setCompoundDrawables
import com.kylecorry.andromeda.core.ui.setTextDistinct
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
import com.kylecorry.trail_sense.shared.views.CameraView
import com.kylecorry.trail_sense.shared.views.DataPointView
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

        CustomUiUtils.setButtonState(views.clinometerTitle.leftButton, false)
        CustomUiUtils.setButtonState(views.clinometerTitle.rightButton, false)

        views.cameraViewHolder.clipToOutline = true
        views.camera.setScaleType(PreviewView.ScaleType.FILL_CENTER)
        views.camera.setShowTorch(false)
        views.camera.passThroughTouchEvents = true

        views.clinometerTitle.leftButton.setOnClickListener {
            if (useCamera) {
                startSideClinometer()
            } else {
                startCameraClinometer(true)
            }
        }

        views.clinometerTitle.rightButton.setOnClickListener {
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
        views.clinometerTitle.leftButton.setImageResource(R.drawable.ic_camera)
        CustomUiUtils.setButtonState(views.clinometerTitle.leftButton, false)
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
                views.clinometerTitle.leftButton.setImageResource(R.drawable.ic_phone_portrait)
                CustomUiUtils.setButtonState(views.clinometerTitle.leftButton, false)
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
                CustomUiUtils.setButtonState(views.clinometerTitle.rightButton, true)
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
                CustomUiUtils.setButtonState(views.clinometerTitle.rightButton, true)

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
            CustomUiUtils.setButtonState(
                views.clinometerTitle.rightButton, distanceAway != null
            )
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
            views.clinometerTitle.title.setCompoundDrawables(
                Resources.dp(requireContext(), 24f).toInt(),
                right = if (lockState.read()) R.drawable.lock else null
            )
        }

        CustomUiUtils.setImageColor(
            views.clinometerTitle.title, Resources.androidTextColorPrimary(requireContext())
        )

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

        views.clinometerTitle.title.setTextDistinct(formatter.formatDegrees(incline))
        views.clinometerTitle.subtitle.setTextDistinct(
            getString(R.string.slope_amount, formatter.formatPercentage(getSlopePercent(incline)))
        )

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
    val clinometerTitle: ClinometerTitle,
    val clinometer: ClinometerView,
    val cameraViewHolder: LinearLayout,
    val camera: CameraView,
    val arView: AugmentedRealityView,
    val cameraClinometer: CameraClinometerView,
    val avalancheRisk: DataPointView,
    val estimatedHeight: DataPointView,
    val clinometerInstructions: TextView,
    val cameraClinometerInstructions: TextView
)

private data class ClinometerTitle(
    val root: View,
    val leftButton: ImageButton,
    val rightButton: ImageButton,
    val title: TextView,
    val subtitle: TextView
)

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

    val title = createClinometerTitle(context)
    root.addView(
        title.root,
        ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        }
    )

    val dataPoints = LinearLayout(context).apply {
        id = R.id.data_points
        orientation = LinearLayout.HORIZONTAL
    }
    val avalancheRisk = DataPointView(context, null).apply {
        id = R.id.avalanche_risk
        description = context.getString(R.string.avalanche_risk)
        setImageResource(R.drawable.avalanche)
    }
    val estimatedHeight = DataPointView(context, null).apply {
        id = R.id.estimated_height
        description = context.getString(R.string.height)
        setImageResource(R.drawable.ruler)
    }
    dataPoints.addView(
        avalancheRisk,
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    )
    dataPoints.addView(
        estimatedHeight,
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    )
    root.addView(
        dataPoints,
        ConstraintLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            setMargins(16.dp(context), 0, 16.dp(context), 32.dp(context))
        }
    )

    val instrumentFrame = FrameLayout(context).apply {
        id = View.generateViewId()
    }
    root.addView(
        instrumentFrame,
        ConstraintLayout.LayoutParams(0, 0).apply {
            topToBottom = R.id.clinometer_title
            bottomToTop = R.id.data_points
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
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    )

    val cameraViewHolder = LinearLayout(context).apply {
        id = R.id.camera_view_holder
        orientation = LinearLayout.HORIZONTAL
        visibility = View.GONE
        background = ContextCompat.getDrawable(context, R.drawable.rounded_rectangle)
        backgroundTintList = ColorStateList.valueOf(Resources.color(context, R.color.colorSecondary))
        clipToOutline = true
    }
    val cameraFrame = FrameLayout(context)
    val camera = CameraView(context, null).apply {
        id = R.id.camera
    }
    val arView = AugmentedRealityView(context).apply {
        id = R.id.ar_view
    }
    val reticle = View(context).apply {
        setBackgroundColor(ContextCompat.getColor(context, R.color.orange_40))
    }
    cameraFrame.addView(
        camera,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    )
    cameraFrame.addView(
        arView,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            Gravity.CENTER
        )
    )
    cameraFrame.addView(
        reticle,
        FrameLayout.LayoutParams(60.dp(context), 1.dp(context), Gravity.CENTER)
    )
    val cameraClinometer = CameraClinometerView(context).apply {
        id = R.id.camera_clinometer
    }
    cameraViewHolder.addView(
        cameraFrame,
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
    )
    cameraViewHolder.addView(
        cameraClinometer,
        LinearLayout.LayoutParams(50.dp(context), ViewGroup.LayoutParams.MATCH_PARENT)
    )
    instrumentFrame.addView(
        cameraViewHolder,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
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
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        )
    )
    instrumentFrame.addView(
        cameraClinometerInstructions,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        )
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

private fun createClinometerTitle(context: Context): ClinometerTitle {
    val root = LinearLayout(context).apply {
        id = R.id.clinometer_title
        gravity = Gravity.CENTER_VERTICAL
        orientation = LinearLayout.HORIZONTAL
        setPadding(8.dp(context), 8.dp(context), 8.dp(context), 8.dp(context))
    }
    val leftButton = ImageButton(context).apply {
        id = R.id.clinometer_mode_btn
        setImageResource(R.drawable.ic_camera)
        backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        imageTintList = ColorStateList.valueOf(Resources.androidTextColorSecondary(context))
    }
    val textHolder = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
    }
    val title = TextView(context).apply {
        gravity = Gravity.CENTER
        textSize = 24f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Resources.androidTextColorPrimary(context))
    }
    val subtitle = TextView(context).apply {
        gravity = Gravity.CENTER
        textSize = 14f
        setTextColor(Resources.androidTextColorSecondary(context))
    }
    textHolder.addView(
        title,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )
    textHolder.addView(
        subtitle,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )
    val rightButton = ImageButton(context).apply {
        id = R.id.clinometer_measure_btn
        setImageResource(R.drawable.ruler)
        backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        imageTintList = ColorStateList.valueOf(Resources.androidTextColorSecondary(context))
    }
    root.addView(leftButton, LinearLayout.LayoutParams(48.dp(context), 48.dp(context)))
    root.addView(textHolder, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    root.addView(rightButton, LinearLayout.LayoutParams(48.dp(context), 48.dp(context)))

    return ClinometerTitle(
        root = root,
        leftButton = leftButton,
        rightButton = rightButton,
        title = title,
        subtitle = subtitle
    )
}

private fun createInstructionView(
    context: Context,
    id: Int,
    icon: Int
): TextView {
    return TextView(context).apply {
        this.id = id
        gravity = Gravity.CENTER
        text = context.getString(R.string.clinometer_rotate_device)
        compoundDrawablePadding = 8.dp(context)
        setCompoundDrawables(top = icon)
        CustomUiUtils.setImageColor(this, Resources.androidTextColorSecondary(context))
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
