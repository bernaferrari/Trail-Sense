package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.dialog
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.observe
import com.kylecorry.sol.math.MathExtensions.roundNearestAngle
import com.kylecorry.sol.math.trigonometry.Trigonometry
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
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
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment
import com.kylecorry.trail_sense.tools.beacons.map_layers.BeaconGeoJsonSource
import com.kylecorry.trail_sense.tools.beacons.infrastructure.BeaconPickers
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.ClosestBeaconSort
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PhotoMapCalibrationFragment : TrailSenseComposeFragment() {

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

    override fun beforeCreateContent() {
        calibrationViews = createPhotoMapCalibrationViews(requireContext())
    }

    @Composable
    override fun FragmentContent() {
        PhotoMapCalibrationHost(
            views = views,
            modifier = Modifier.fillMaxSize()
        )
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

        views.mapCalibrationBottomPanel.onTitleClick = {
            dialog(
                getString(R.string.map_calibration),
                getString(R.string.map_calibration_instructions),
                cancelText = null
            )
        }

        reloadMap()

        views.mapCalibrationBottomPanel.onNextClick = {
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

        views.mapCalibrationBottomPanel.onPreviousClick = {
            calibrationIndex--
            selectPoint(calibrationIndex)
        }


        views.mapCalibrationBottomPanel.setOnCoordinateChangeListener {
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

        views.zoomInBtn.setState(false)
        views.zoomOutBtn.setState(false)

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
            views.previewButton.checked = showPreview
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
        views.mapCalibrationBottomPanel.title =
            getString(R.string.calibrate_map_point, index + 1, maxPoints)
        views.mapCalibrationBottomPanel.coordinate = if (manager.isCalibrated(index)) {
            manager.getCalibrationPoint(index).location
        } else {
            null
        }
        views.mapCalibrationBottomPanel.isVisible = true
        views.mapCalibrationBottomPanel.nextText =
            if (index == (maxPoints - 1)) getString(R.string.done) else getString(R.string.next)
        views.mapCalibrationBottomPanel.showPrevious = index == 1

        updateCompletionState()
    }

    private fun updateCompletionState() {
        val validation = validateCalibration()
        val notice = getCalibrationNotice(validation)
        updateCalibrationText(notice)

        val isValid = validation == MapCalibrationValidationResult.Valid
        views.previewButton.isEnabled = isValid

        views.mapCalibrationBottomPanel.notice = notice
        views.mapCalibrationBottomPanel.icon = when {
            notice.isNotBlank() -> R.drawable.ic_alert
            manager.isCalibrated(calibrationIndex) -> R.drawable.ic_check_outline
            else -> R.drawable.ic_info
        }
        views.mapCalibrationBottomPanel.iconTint = when {
            notice.isNotBlank() -> Color(getErrorColor())
            manager.isCalibrated(calibrationIndex) -> Color(AppColor.Green.color)
            else -> Color(Resources.androidTextColorSecondary(requireContext()))
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

        views.previewButton.checked = enabled

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
        views.mapCalibrationBottomPanel.title =
            getString(R.string.calibrate_map_point, calibrationIndex + 1, maxPoints)
        views.mapCalibrationBottomPanel.notice = notice
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
    val previewButton: PhotoMapTextButton,
    val calibrationMap: MapCalibrationView,
    val zoomOutBtn: PhotoMapIconButton,
    val zoomInBtn: PhotoMapIconButton,
    val mapCalibrationBottomPanel: PhotoMapCalibrationPanelView
)

private data class PhotoMapCalibrationControls(
    val previewButton: PhotoMapTextButton,
    val calibrationMap: MapCalibrationView,
    val zoomOutBtn: PhotoMapIconButton,
    val zoomInBtn: PhotoMapIconButton,
    val bottomPanel: PhotoMapCalibrationPanelView
)

@Suppress("FunctionNaming")
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
    val root = ConstraintLayout(context)
    val controls = createPhotoMapCalibrationControls(context)
    addPhotoMapCalibrationControls(context, root, controls)

    return PhotoMapCalibrationViews(
        root = root,
        previewButton = controls.previewButton,
        calibrationMap = controls.calibrationMap,
        zoomOutBtn = controls.zoomOutBtn,
        zoomInBtn = controls.zoomInBtn,
        mapCalibrationBottomPanel = controls.bottomPanel
    )
}

private fun createPhotoMapCalibrationControls(context: Context): PhotoMapCalibrationControls {
    val previewButton = PhotoMapTextButton(context, context.getString(R.string.preview)).apply {
        id = R.id.preview_button
        alpha = 0.86f
        visibility = View.GONE
    }

    val calibrationMap = MapCalibrationView(context).apply {
        id = R.id.calibration_map
    }

    val zoomOutBtn = PhotoMapIconButton(context, R.drawable.ic_zoom_out).apply {
        id = R.id.zoom_out_btn
        alpha = 0.86f
        isClickable = true
        isFocusable = true
    }

    val zoomInBtn = PhotoMapIconButton(context, R.drawable.ic_add).apply {
        id = R.id.zoom_in_btn
        alpha = 0.86f
        isClickable = true
        isFocusable = true
    }

    val bottomPanel = PhotoMapCalibrationPanelView(context).apply {
        id = R.id.map_calibration_bottom_panel
        visibility = View.GONE
    }

    return PhotoMapCalibrationControls(
        previewButton = previewButton,
        calibrationMap = calibrationMap,
        zoomOutBtn = zoomOutBtn,
        zoomInBtn = zoomInBtn,
        bottomPanel = bottomPanel
    )
}

private fun addPhotoMapCalibrationControls(
    context: Context,
    root: ConstraintLayout,
    controls: PhotoMapCalibrationControls
) {
    val margin = Resources.dp(context, 16f).toInt()
    val zoomBottom = Resources.dp(context, 176f).toInt()

    root.addView(
        controls.calibrationMap,
        ConstraintLayout.LayoutParams(0, 0).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
    )
    root.addView(
        controls.previewButton,
        ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            topMargin = margin
        }
    )
    root.addView(
        controls.zoomOutBtn,
        ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            marginEnd = margin
            bottomMargin = zoomBottom
        }
    )
    root.addView(
        controls.zoomInBtn,
        ConstraintLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            marginEnd = margin
            bottomMargin = zoomBottom + Resources.dp(context, 56f).toInt()
        }
    )
    root.addView(
        controls.bottomPanel,
        ConstraintLayout.LayoutParams(
            0,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
    )
}

private class PhotoMapCalibrationPanelView(
    context: Context
) : AbstractComposeView(context) {

    private val formatService by lazy { FormatService.getInstance(context) }
    private val sensorService by lazy { SensorService(context) }
    private val gps by lazy { sensorService.getGPS() }

    var title by mutableStateOf("")
    var notice by mutableStateOf("")
    var icon by mutableStateOf(R.drawable.ic_info)
    var iconTint by mutableStateOf(Color(Resources.androidTextColorSecondary(context)))
    var nextText by mutableStateOf(context.getString(R.string.next))
    var showPrevious by mutableStateOf(false)
    var onTitleClick: () -> Unit = {}
    var onPreviousClick: () -> Unit = {}
    var onNextClick: () -> Unit = {}

    private var coordinateText by mutableStateOf("")
    private var isLoadingLocation by mutableStateOf(false)
    private var coordinateChangeListener: ((Coordinate?) -> Unit)? = null

    var coordinate: Coordinate?
        get() = Coordinate.parse(coordinateText)
        set(value) {
            coordinateText = value?.let { formatService.formatLocation(it) } ?: ""
        }

    fun setOnCoordinateChangeListener(listener: ((Coordinate?) -> Unit)?) {
        coordinateChangeListener = listener
    }

    override fun onDetachedFromWindow() {
        gps.stop(this::onGPSUpdate)
        super.onDetachedFromWindow()
    }

    private fun updateCoordinate(text: String) {
        coordinateText = text
        coordinateChangeListener?.invoke(Coordinate.parse(text))
    }

    private fun autofill() {
        if (isLoadingLocation) {
            return
        }
        isLoadingLocation = true
        gps.start(this::onGPSUpdate)
    }

    private fun onGPSUpdate(): Boolean {
        isLoadingLocation = false
        val location = gps.location
        coordinate = location
        coordinateChangeListener?.invoke(location)
        return false
    }

    private fun pickAutofillSource() {
        Pickers.item(
            context,
            context.getString(R.string.autofill_source),
            listOf(
                context.getString(R.string.gps),
                context.getString(R.string.beacon)
            ),
            0
        ) {
            when (it) {
                0 -> autofill()
                1 -> pickBeacon()
            }
        }
    }

    private fun pickBeacon() {
        CoroutineScope(Dispatchers.Main).launch {
            val beacon = BeaconPickers.pickBeacon(
                context,
                sort = ClosestBeaconSort(BeaconService(context), gps::location)
            ) ?: return@launch
            coordinate = beacon.coordinate
            coordinateChangeListener?.invoke(beacon.coordinate)
        }
    }

    @Composable
    override fun Content() {
        TrailSenseTheme {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                panelHeader()
                Spacer(modifier = Modifier.height(16.dp))
                coordinateInput()
                Spacer(modifier = Modifier.height(16.dp))
                panelActions()
            }
        }
    }

    @Composable
    private fun panelHeader() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onTitleClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (notice.isNotBlank()) {
                    Text(
                        text = notice,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(
                            Resources.getAndroidColorAttr(
                                context,
                                androidx.appcompat.R.attr.colorError
                            )
                        )
                    )
                }
            }
        }
    }

    @Composable
    private fun coordinateInput() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoadingLocation) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            } else {
                IconButton(
                    onClick = { pickAutofillSource() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_location),
                        contentDescription = null
                    )
                }
            }

            Spacer(modifier = Modifier.size(8.dp))

            OutlinedTextField(
                value = coordinateText,
                onValueChange = ::updateCoordinate,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text(context.getString(R.string.location)) },
                isError = coordinateText.isNotBlank() && Coordinate.parse(coordinateText) == null
            )

            IconButton(
                onClick = {
                    Alerts.dialog(
                        context,
                        context.getString(R.string.location_input_help_title),
                        context.getString(R.string.location_input_help),
                        cancelText = null
                    )
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_help),
                    contentDescription = null
                )
            }
        }
    }

    @Composable
    private fun panelActions() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (showPrevious) {
                Button(onClick = { onPreviousClick() }) {
                    Text(context.getString(R.string.previous))
                }
                Spacer(modifier = Modifier.size(16.dp))
            }
            Button(onClick = { onNextClick() }) {
                Text(nextText)
            }
        }
    }
}

private class PhotoMapTextButton(
    context: Context,
    text: String = ""
) : AbstractComposeView(context) {
    var text by mutableStateOf(text)
    var checked by mutableStateOf(false)

    @Composable
    override fun Content() {
        TrailSenseTheme {
            Button(
                onClick = { performClick() },
                colors = if (checked) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(text = text)
            }
        }
    }
}

@Suppress("FunctionNaming")
@Preview(showBackground = true)
@Composable
private fun PhotoMapCalibrationHostPreview() {
    TrailSenseTheme {
        PhotoMapCalibrationHost(
            views = createPhotoMapCalibrationViews(androidx.compose.ui.platform.LocalContext.current)
        )
    }
}
