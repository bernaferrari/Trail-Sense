package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.andromeda.alerts.dialog
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.core.ui.setCompoundDrawables
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.sol.math.MathExtensions.roundNearestAngle
import com.kylecorry.sol.math.trigonometry.Trigonometry
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.declination.GPSDeclinationStrategy
import com.kylecorry.trail_sense.shared.extensions.promptIfUnsavedChanges
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.setLayersWithPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.start
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.stop
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.CoordinateInputView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.beacons.map_layers.BeaconGeoJsonSource
import com.kylecorry.trail_sense.tools.map.map_layers.MyLocationGeoJsonSource
import com.kylecorry.trail_sense.tools.offline_maps.OfflineMapsToolRegistration
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapService
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapState
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.calibration.MapCalibrationManager
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.calibration.MapCalibrationValidationResult
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.calibration.MapCalibrationValidator
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration.MapRotationCalculator
import com.kylecorry.trail_sense.tools.paths.map_layers.PathGeoJsonSource

class PhotoMapCalibrationFragment : Fragment() {

    private val service = getAppService<OfflineMapService>()
    private val prefs by lazy { UserPreferences(requireContext()) }
    private var calibrationViews: PhotoMapCalibrationViews? = null
    private val views: PhotoMapCalibrationViews
        get() = calibrationViews ?: error("Photo map calibration views are not ready")
    private val isBound: Boolean
        get() = calibrationViews != null

    private var mapId = 0L
    private var map: PhotoMap? = null

    private var calibrationIndex = 0
    private var maxPoints = 2
    private var onDone: () -> Unit = {}
    private var showRotation: (Float) -> Unit = {}
    private val rotationCalculator = MapRotationCalculator()
    private var originalRotation = 0f

    private lateinit var backCallback: OnBackPressedCallback

    private val manager = MapCalibrationManager(maxPoints) {
        updateMapCalibration()
    }

    // Sensors
    private val sensorService by lazy { SensorService(requireContext()) }
    private val gps by lazy { sensorService.getGPS() }
    private val compass by lazy { sensorService.getCompass() }

    private val declinationStrategy by lazy {
        GPSDeclinationStrategy(gps)
    }

    private var showPreview = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapId = requireArguments().getLong("mapId")
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val views = createPhotoMapCalibrationViews(requireContext())
        calibrationViews = views
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    PhotoMapCalibrationHost(
                        views = views,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Populate the last known location
        views.calibrationMap.useDensityPixelsForZoom = !prefs.photoMaps.highDetailMode
        views.calibrationMap.userLocation = gps.location
        views.calibrationMap.userLocationAccuracy =
            gps.horizontalAccuracy?.let { Distance.meters(it) }

        observe(gps) {
            views.calibrationMap.userLocation = gps.location
            views.calibrationMap.userLocationAccuracy =
                gps.horizontalAccuracy?.let { Distance.meters(it) }
            compass.declination = declinationStrategy.getDeclination()
        }

        observe(compass) {
            views.calibrationMap.userAzimuth = compass.bearing
        }
    }

    override fun onPause() {
        super.onPause()
        calibrationViews?.calibrationMap?.stop()
    }

    override fun onDestroyView() {
        calibrationViews = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CustomUiUtils.disclaimer(
            requireContext(),
            getString(R.string.map_calibration),
            getString(R.string.map_calibration_instructions),
            getString(R.string.map_calibration_shown),
            cancelText = null
        )

        views.mapCalibrationTitle.setOnClickListener {
            dialog(
                getString(R.string.map_calibration),
                getString(R.string.map_calibration_instructions),
                cancelText = null
            )
        }

        reloadMap()

        views.calibrationNext.setOnClickListener {
            if (calibrationIndex == (maxPoints - 1)) {
                inBackground {
                    map = map?.let { save(service.getPhotoMap(it.id) ?: it) }
                    manager.reset(map?.georeference?.calibrationPoints ?: emptyList())
                    backCallback.remove()
                    onDone()
                }
            } else {
                calibrationIndex++
                selectPoint(calibrationIndex)
            }
        }

        views.calibrationPrev.setOnClickListener {
            calibrationIndex--
            selectPoint(calibrationIndex)
        }


        views.mapCalibrationCoordinate.setOnCoordinateChangeListener {
            manager.calibrate(calibrationIndex, it)
        }

        views.calibrationMap.onMapClick = {
            manager.calibrate(calibrationIndex, it)
        }

        views.previewButton.setOnClickListener {
            val validation = validateCalibration()
            if (validation == MapCalibrationValidationResult.Valid) {
                setPreviewMode(!showPreview)
            } else {
                updateCompletionState()
            }
        }

        CustomUiUtils.setButtonState(views.zoomInBtn, false)
        CustomUiUtils.setButtonState(views.zoomOutBtn, false)

        views.zoomOutBtn.setOnClickListener {
            views.calibrationMap.zoomBy(0.5f)
        }

        views.zoomInBtn.setOnClickListener {
            views.calibrationMap.zoomBy(2f)
        }

        backCallback = promptIfUnsavedChanges {
            hasChanges()
        }
    }

    fun setOnCompleteListener(listener: () -> Unit) {
        onDone = listener
    }

    fun setOnRotationListener(listener: (Float) -> Unit) {
        showRotation = listener
    }

    fun reloadMap() {
        inBackground {
            map = service.getPhotoMap(mapId)
            onMain {
                map?.let(::onMapLoad)
            }
        }
    }

    private fun onMapLoad(map: PhotoMap) {
        this.map = map
        originalRotation = map.georeference.rotation
        views.calibrationMap.mapAzimuth = 0f
        views.calibrationMap.keepMapUp = true
        views.calibrationMap.showMap(map)
        calibrateMap()
    }

    private fun updateMapCalibration() {
        if (!isBound) {
            return
        }

        val isCalibrated = isFullyCalibrated()
        if (views.previewButton.isVisible != isCalibrated) {
            views.previewButton.isVisible = isCalibrated
            CustomUiUtils.setButtonState(views.previewButton, showPreview)
        }

        map = map?.copy(
            georeference = map!!.georeference.copy(
                calibrationPoints = manager.getCalibration(false)
            )
        )

        val validation = validateCalibration()
        val isValid = validation == MapCalibrationValidationResult.Valid
        views.previewButton.isEnabled = isValid

        if ((!isCalibrated || !isValid) && showPreview) {
            setPreviewMode(false)
        }

        map = map?.copy(
            georeference = map!!.georeference.copy(
                rotation = if (showPreview) rotationCalculator.calculate(map!!) else originalRotation
            )
        )
        val map = map ?: return
        views.calibrationMap.mapAzimuth = 0f
        views.calibrationMap.keepMapUp = true
        views.calibrationMap.showMap(map)
        views.calibrationMap.highlightedIndex = calibrationIndex
        updateCompletionState()
        updateRotation()
    }

    private fun updateRotation() {
        val map = map ?: return
        val rotation = if (map.state == OfflineMapState.Ready) {
            rotationCalculator.calculate(map)
        } else {
            map.baseRotation().toFloat()
        }

        val baseRotation = rotation.roundNearestAngle(90f)

        showRotation(Trigonometry.deltaAngle(baseRotation, rotation))
    }


    private fun calibrateMap() {
        map ?: return
        loadCalibrationPointsFromMap()

        // Find the first uncalibrated point
        calibrationIndex = manager.getNextUncalibratedIndex().coerceAtLeast(0)

        selectPoint(calibrationIndex)
    }

    fun recenter() {
        views.calibrationMap.recenter()
    }

    private fun selectPoint(index: Int) {
        views.mapCalibrationTitle.text =
            getString(R.string.calibrate_map_point, index + 1, maxPoints)
        views.mapCalibrationCoordinate.coordinate = if (manager.isCalibrated(index)) {
            manager.getCalibrationPoint(index).location
        } else {
            null
        }
        views.mapCalibrationBottomPanel.isVisible = true
        views.calibrationNext.text =
            if (index == (maxPoints - 1)) getString(R.string.done) else getString(R.string.next)
        views.calibrationPrev.isVisible = index == 1

        updateCompletionState()
    }

    private fun updateCompletionState() {
        val validation = validateCalibration()
        val notice = getCalibrationNotice(validation)
        updateCalibrationText(notice)

        val isValid = validation == MapCalibrationValidationResult.Valid
        views.previewButton.isEnabled = isValid

        // If it is calibrated, replace the info icon with a green checkmark
        if (notice.isNotBlank()) {
            views.mapCalibrationTitle.setCompoundDrawables(
                Resources.dp(requireContext(), 24f).toInt(),
                left = R.drawable.ic_alert
            )
            Colors.setImageColor(views.mapCalibrationTitle, getErrorColor())
        } else if (manager.isCalibrated(calibrationIndex)) {
            views.mapCalibrationTitle.setCompoundDrawables(
                Resources.dp(requireContext(), 24f).toInt(),
                left = R.drawable.ic_check_outline
            )
            Colors.setImageColor(views.mapCalibrationTitle, AppColor.Green.color)
        } else {
            views.mapCalibrationTitle.setCompoundDrawables(
                Resources.dp(requireContext(), 24f).toInt(),
                left = R.drawable.ic_info
            )
            Colors.setImageColor(
                views.mapCalibrationTitle,
                Resources.androidTextColorSecondary(requireContext())
            )
        }
    }

    private fun updateNextButtonText() {
        binding.calibrationNext.text = when {
            calibrationIndex != maxPoints - 1 -> getString(R.string.next)
            validateCalibration() == MapCalibrationValidationResult.Valid -> getString(R.string.finish_calibration)
            else -> getString(R.string.save_draft)
        }
    }

    private fun setPreviewMode(enabled: Boolean) {
        if (showPreview == enabled) {
            return
        }

        CustomUiUtils.setButtonState(views.previewButton, enabled)

        showPreview = enabled
        updateMapCalibration()

        views.calibrationMap.stop()

        val layers = if (enabled) listOf(
            PathGeoJsonSource.SOURCE_ID,
            MyLocationGeoJsonSource.SOURCE_ID,
            BeaconGeoJsonSource.SOURCE_ID
        ) else emptyList()
        inBackground {
            views.calibrationMap.setLayersWithPreferences(
                OfflineMapsToolRegistration.PHOTO_MAPS_ID,
                layers
            )

            if (showPreview) {
                views.calibrationMap.start()
            }
        }

    }

    private fun isFullyCalibrated(): Boolean {
        return manager.getCalibration(true).size == maxPoints
    }

    private fun validateCalibration(): MapCalibrationValidationResult {
        return if (map != null && isFullyCalibrated()) {
            MapCalibrationValidator.validate(map!!)
        } else {
            MapCalibrationValidationResult.Uncalibrated
        }
    }

    private fun getCalibrationNotice(result: MapCalibrationValidationResult): String {
        return when (result) {
            MapCalibrationValidationResult.Valid -> ""
            MapCalibrationValidationResult.Uncalibrated -> ""
            MapCalibrationValidationResult.SamePixel ->
                getString(R.string.map_calibration_error_same_pixel)

            MapCalibrationValidationResult.SameImageAxis ->
                getString(R.string.map_calibration_error_same_image_axis)

            MapCalibrationValidationResult.SameLocation ->
                getString(R.string.map_calibration_error_same_location)

            MapCalibrationValidationResult.ImplausibleScale ->
                getString(R.string.map_calibration_error_implausible_scale)
        }
    }

    private fun updateCalibrationTitle(notice: String) {
        val title = getString(R.string.calibrate_map_point, calibrationIndex + 1, maxPoints)
        views.mapCalibrationTitle.text = if (notice.isBlank()) {
            title
        } else {
            buildSpannedString {
                append(getString(R.string.calibrate_map_point_instructions))
                appendLine()
                color(getErrorColor()) {
                    append(notice)
                }
            }
        }
    }

    private fun getErrorColor(): Int {
        return Resources.getAndroidColorAttr(
            requireContext(),
            androidx.appcompat.R.attr.colorError
        )
    }

    private suspend fun save(map: PhotoMap): PhotoMap {
        return service.calibrate(map, manager.getCalibration())
    }

    private fun loadCalibrationPointsFromMap() {
        val map = map ?: return
        manager.reset(map.georeference.calibrationPoints)
    }

    private fun hasChanges(): Boolean {
        return manager.hasChanges()
    }

    companion object {
        fun create(
            mapId: Long,
            showRotation: (rotation: Float) -> Unit = {},
            onComplete: () -> Unit = {}
        ): PhotoMapCalibrationFragment {
            return PhotoMapCalibrationFragment().apply {
                arguments = Bundle().apply {
                    putLong("mapId", mapId)
                }
                setOnRotationListener(showRotation)
                setOnCompleteListener(onComplete)
            }
        }
    }

}

private data class PhotoMapCalibrationViews(
    val root: View,
    val previewButton: Button,
    val calibrationMap: MapCalibrationView,
    val zoomOutBtn: FloatingActionButton,
    val zoomInBtn: FloatingActionButton,
    val mapCalibrationBottomPanel: LinearLayout,
    val mapCalibrationTitle: TextView,
    val mapCalibrationCoordinate: CoordinateInputView,
    val calibrationPrev: Button,
    val calibrationNext: Button
)

@Composable
private fun PhotoMapCalibrationHost(
    views: PhotoMapCalibrationViews,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { views.root }
    )
}

private fun createPhotoMapCalibrationViews(context: Context): PhotoMapCalibrationViews {
    val margin = Resources.dp(context, 16f).toInt()
    val panelPadding = Resources.dp(context, 16f).toInt()
    val zoomBottom = Resources.dp(context, 176f).toInt()

    val root = FrameLayout(context)

    val previewButton = Button(context).apply {
        id = R.id.preview_button
        text = context.getString(R.string.preview)
        alpha = 0.86f
        visibility = View.GONE
    }

    val calibrationMap = MapCalibrationView(context).apply {
        id = R.id.calibration_map
    }

    val zoomOutBtn = FloatingActionButton(context).apply {
        id = R.id.zoom_out_btn
        alpha = 0.86f
        setImageResource(R.drawable.ic_zoom_out)
        isClickable = true
        isFocusable = true
    }

    val zoomInBtn = FloatingActionButton(context).apply {
        id = R.id.zoom_in_btn
        alpha = 0.86f
        setImageResource(R.drawable.ic_add)
        isClickable = true
        isFocusable = true
    }

    val bottomPanel = LinearLayout(context).apply {
        id = R.id.map_calibration_bottom_panel
        orientation = LinearLayout.VERTICAL
        setPadding(panelPadding, panelPadding, panelPadding, panelPadding)
        setBackgroundColor(Resources.getAndroidColorAttr(context, android.R.attr.colorBackground))
        visibility = View.GONE
    }

    val title = TextView(context).apply {
        id = R.id.map_calibration_title
        gravity = Gravity.CENTER_VERTICAL
        compoundDrawablePadding = Resources.dp(context, 8f).toInt()
    }

    val coordinate = CoordinateInputView(context).apply {
        id = R.id.map_calibration_coordinate
    }

    val prev = Button(context).apply {
        id = R.id.calibration_prev
        text = context.getString(R.string.previous)
    }

    val next = Button(context).apply {
        id = R.id.calibration_next
        text = context.getString(R.string.next)
    }

    val buttons = LinearLayout(context).apply {
        gravity = Gravity.CENTER or Gravity.END
        orientation = LinearLayout.HORIZONTAL
        addView(
            prev,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = margin
            }
        )
        addView(next)
    }

    bottomPanel.addView(title)
    bottomPanel.addView(
        coordinate,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = margin
        }
    )
    bottomPanel.addView(
        buttons,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = margin
        }
    )

    root.addView(
        calibrationMap,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    )
    root.addView(
        previewButton,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL
        ).apply {
            topMargin = margin
        }
    )
    root.addView(
        zoomOutBtn,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.END
        ).apply {
            marginEnd = margin
            bottomMargin = zoomBottom
        }
    )
    root.addView(
        zoomInBtn,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.END
        ).apply {
            marginEnd = margin
            bottomMargin = zoomBottom + Resources.dp(context, 56f).toInt()
        }
    )
    root.addView(
        bottomPanel,
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM
        )
    )

    return PhotoMapCalibrationViews(
        root = root,
        previewButton = previewButton,
        calibrationMap = calibrationMap,
        zoomOutBtn = zoomOutBtn,
        zoomInBtn = zoomInBtn,
        mapCalibrationBottomPanel = bottomPanel,
        mapCalibrationTitle = title,
        mapCalibrationCoordinate = coordinate,
        calibrationPrev = prev,
        calibrationNext = next
    )
}

@Preview(showBackground = true)
@Composable
private fun PhotoMapCalibrationHostPreview() {
    TrailSenseTheme {
        PhotoMapCalibrationHost(
            views = createPhotoMapCalibrationViews(androidx.compose.ui.platform.LocalContext.current)
        )
    }
}
