@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LifecycleOwner
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.observe
import androidx.annotation.StringRes
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.shared.extensions.asLiveData
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
import com.kylecorry.trail_sense.shared.extensions.inBackground
import com.kylecorry.trail_sense.shared.views.compose.UnsavedChangesBackHandler
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.setLayersWithPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.start
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.stop
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseElevation
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.beacons.map_layers.BeaconGeoJsonSource
import com.kylecorry.trail_sense.tools.beacons.infrastructure.BeaconPickers
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.ClosestBeaconSort
import com.kylecorry.trail_sense.tools.map.map_layers.MyLocationGeoJsonSource
import com.kylecorry.trail_sense.tools.offline_maps.OfflineMapsToolRegistration
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.MapCalibrationManager
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.MapCalibrationValidationResult
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.MapCalibrationValidator
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.calibration.MapRotationCalculator
import com.kylecorry.trail_sense.tools.paths.map_layers.PathGeoJsonSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun PhotoMapCalibrationToolScreen(
    mapId: Long,
    onRotation: (Float) -> Unit,
    onComplete: () -> Unit,
    onController: (PhotoMapCalibrationController) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember(mapId, onRotation, onComplete) {
        PhotoMapCalibrationController(context, lifecycleOwner, mapId, onRotation, onComplete)
    }

    UnsavedChangesBackHandler(hasChanges = controller.hasUnsavedChanges)

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
        }
    }

    controller.Content(modifier = modifier)
}

class PhotoMapCalibrationController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val mapId: Long,
    private var showRotation: (Float) -> Unit,
    private var onDone: () -> Unit
) {

    private val service = getAppService<MapService>()
    private val prefs by lazy { UserPreferences(context) }
    private var calibrationViews: PhotoMapCalibrationViews? =
        createPhotoMapCalibrationViews(context)
    private val views: PhotoMapCalibrationViews
        get() = calibrationViews ?: error("Photo map calibration views are not ready")
    private val isBound: Boolean
        get() = calibrationViews != null

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        PhotoMapCalibrationHost(
            views = views,
            modifier = modifier
        )
    }

    private var map: PhotoMap? = null

    private var calibrationIndex = 0
    private var maxPoints = 2
    private val rotationCalculator = MapRotationCalculator()
    private var originalRotation = 0f

    /** Whether back should ask before leaving: the calibration changed and hasn't been saved. */
    var hasUnsavedChanges by mutableStateOf(false)
        private set
    private var isSaved = false

    private val manager = MapCalibrationManager(maxPoints) {
        updateMapCalibration()
        updateHasUnsavedChanges()
    }

    // Sensors
    private val sensorService by lazy { SensorService(context) }
    private val gps by lazy { sensorService.getGPS() }
    private val compass by lazy { sensorService.getCompass() }

    private val declinationStrategy by lazy {
        GPSDeclinationStrategy(gps)
    }

    private var showPreview = false

    fun onCreate() {
        CustomUiUtils.disclaimer(
            context,
            context.getString(R.string.map_calibration),
            context.getString(R.string.map_calibration_instructions),
            context.getString(R.string.map_calibration_shown),
            cancelText = null
        )

        views.mapCalibrationBottomPanel.onTitleClick = {
            Alerts.dialog(
                context,
                context.getString(R.string.map_calibration),
                context.getString(R.string.map_calibration_instructions),
                cancelText = null
            )
        }

        reloadMap()

        views.mapCalibrationBottomPanel.onNextClick = {
            if (calibrationIndex == (maxPoints - 1)) {
                lifecycleOwner.inBackground {
                    map = map?.let { save(it) }
                    manager.reset(map?.georeference?.calibrationPoints ?: emptyList())
                    isSaved = true
                    updateHasUnsavedChanges()
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

        views.previewButton.onClick = {
            val validation = validateCalibration()
            if (validation == MapCalibrationValidationResult.Valid) {
                setPreviewMode(!showPreview)
            } else {
                updateCompletionState()
            }
        }

        views.zoomInBtn.isOn = false
        views.zoomOutBtn.isOn = false

        views.zoomOutBtn.onClick = {
            views.calibrationMap.zoomBy(0.5f)
        }

        views.zoomInBtn.onClick = {
            views.calibrationMap.zoomBy(2f)
        }
    }

    fun onResume() {
        // Populate the last known location
        views.calibrationMap.useDensityPixelsForZoom = !prefs.photoMaps.highDetailMode
        views.calibrationMap.userLocation = gps.location
        views.calibrationMap.userLocationAccuracy =
            gps.horizontalAccuracy?.let { Distance.meters(it) }

        gps.asLiveData().observe(lifecycleOwner) {
            views.calibrationMap.userLocation = gps.location
            views.calibrationMap.userLocationAccuracy =
                gps.horizontalAccuracy?.let { Distance.meters(it) }
            compass.declination = declinationStrategy.getDeclination()
        }

        compass.asLiveData().observe(lifecycleOwner) {
            views.calibrationMap.userAzimuth = compass.bearing
        }
    }

    fun onPause() {
        calibrationViews?.calibrationMap?.stop()
    }

    fun onDestroy() {
        calibrationViews = null
    }

    fun reloadMap() {
        lifecycleOwner.inBackground {
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
        val rotation = if (map.isCalibrated) {
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
            getString(R.string.calibrate_map_point_title, index + 1, maxPoints)
        views.mapCalibrationBottomPanel.coordinate = if (manager.isCalibrated(index)) {
            manager.getCalibrationPoint(index).location
        } else {
            null
        }
        views.mapCalibrationBottomPanel.isVisible = true
        views.mapCalibrationBottomPanel.showPrevious = index == 1

        updateCompletionState()
    }

    private fun updateCompletionState() {
        val validation = validateCalibration()
        val notice = getCalibrationNotice(validation)
        updateCalibrationTitle(notice)

        val isValid = validation == MapCalibrationValidationResult.Valid
        views.previewButton.isEnabled = isValid
        views.mapCalibrationBottomPanel.nextText = when {
            calibrationIndex != maxPoints - 1 -> getString(R.string.next)
            isValid -> getString(R.string.finish_calibration)
            else -> getString(R.string.save_draft)
        }

        views.mapCalibrationBottomPanel.notice = notice
        views.mapCalibrationBottomPanel.icon = when {
            notice.isNotBlank() -> TrailIcons.Alert
            manager.isCalibrated(calibrationIndex) -> TrailIcons.CheckOutline
            else -> TrailIcons.Info
        }
        views.mapCalibrationBottomPanel.iconTint = when {
            notice.isNotBlank() -> Color(getErrorColor())
            manager.isCalibrated(calibrationIndex) -> Color(AppColor.Green.color)
            else -> Color(Resources.androidTextColorSecondary(context))
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
        lifecycleOwner.inBackground {
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
            getString(R.string.calibrate_map_point_title, calibrationIndex + 1, maxPoints)
        views.mapCalibrationBottomPanel.notice = notice
    }

    private fun getErrorColor(): Int {
        return Resources.getAndroidColorAttr(
            context,
            androidx.appcompat.R.attr.colorError
        )
    }

    private suspend fun save(map: PhotoMap): PhotoMap {
        var updated = service.getPhotoMap(map.id) ?: return map
        updated =
            updated.copy(georeference = updated.georeference.copy(calibrationPoints = manager.getCalibration()))
        service.add(updated)
        return updated
    }

    private fun loadCalibrationPointsFromMap() {
        val map = map ?: return
        manager.reset(map.georeference.calibrationPoints)
        updateHasUnsavedChanges()
    }

    private fun updateHasUnsavedChanges() {
        hasUnsavedChanges = !isSaved && manager.hasChanges()
    }

    private fun getString(@StringRes id: Int, vararg args: Any): String {
        return context.getString(id, *args)
    }

}

private data class PhotoMapCalibrationViews(
    val previewButton: PhotoMapTextButtonState,
    val calibrationMap: MapCalibrationView,
    val zoomOutBtn: PhotoMapIconButtonState,
    val zoomInBtn: PhotoMapIconButtonState,
    val mapCalibrationBottomPanel: PhotoMapCalibrationPanelState
)

@Suppress("FunctionNaming")
@Composable
private fun PhotoMapCalibrationHost(
    views: PhotoMapCalibrationViews,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        views.calibrationMap.Content(Modifier.fillMaxSize())
        if (views.previewButton.isVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = TrailSenseSpacing.lg)
            ) {
                views.previewButton.Content()
            }
        }
        views.zoomInBtn.Content(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = TrailSenseSpacing.screen, bottom = 232.dp)
        )
        views.zoomOutBtn.Content(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = TrailSenseSpacing.screen, bottom = 176.dp)
        )
        if (views.mapCalibrationBottomPanel.isVisible) {
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                views.mapCalibrationBottomPanel.Content()
            }
        }
    }
}

private fun createPhotoMapCalibrationViews(context: Context): PhotoMapCalibrationViews {
    return PhotoMapCalibrationViews(
        previewButton = PhotoMapTextButtonState(context.getString(R.string.preview)),
        calibrationMap = MapCalibrationView(context).apply { id = R.id.calibration_map },
        zoomOutBtn = PhotoMapIconButtonState(TrailIcons.ZoomOut, R.string.zoom_out),
        zoomInBtn = PhotoMapIconButtonState(TrailIcons.Add, R.string.zoom_in),
        mapCalibrationBottomPanel = PhotoMapCalibrationPanelState(context)
    )
}

private class PhotoMapCalibrationPanelState(
    private val context: Context
) {

    private val formatService by lazy { FormatService.getInstance(context) }
    private val sensorService by lazy { SensorService(context) }
    private val gps by lazy { sensorService.getGPS() }

    var title by mutableStateOf("")
    var notice by mutableStateOf("")
    var icon by mutableStateOf(TrailIcons.Info)
    var iconTint by mutableStateOf(Color(Resources.androidTextColorSecondary(context)))
    var nextText by mutableStateOf(context.getString(R.string.next))
    var showPrevious by mutableStateOf(false)
    var isVisible by mutableStateOf(false)
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
    fun Content() {
        DisposableEffect(Unit) {
            onDispose {
                gps.stop(this@PhotoMapCalibrationPanelState::onGPSUpdate)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TrailSenseSpacing.screen),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
        ) {
            instructionCard()
            coordinateCard()
            panelActions()
        }
    }

    @Composable
    private fun instructionCard() {
        TrailSenseCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onTitleClick() }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = rememberVectorPainter(icon),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.size(TrailSenseSpacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.calibrate_map_point_instructions),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
    }

    @Composable
    private fun coordinateCard() {
        TrailSenseCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = context.getString(R.string.location),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(TrailSenseSpacing.sm))
            coordinateInput()
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
                FilledIconButton(
                    onClick = { pickAutofillSource() },
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = TrailIcons.Location,
                        contentDescription = context.getString(R.string.location),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.size(TrailSenseSpacing.sm))

            com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField(
                value = coordinateText,
                onValueChange = ::updateCoordinate,
                modifier = Modifier.weight(1f),
                label = context.getString(R.string.location),
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                isError = coordinateText.isNotBlank() && Coordinate.parse(coordinateText) == null,
                inCard = true
            )

            Spacer(modifier = Modifier.size(TrailSenseSpacing.sm))

            FilledIconButton(
                onClick = {
                    Alerts.dialog(
                        context,
                        context.getString(R.string.location_input_help_title),
                        context.getString(R.string.location_input_help),
                        cancelText = null
                    )
                },
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = TrailIcons.Help,
                    contentDescription = context.getString(R.string.help),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    @Composable
    private fun panelActions() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showPrevious) {
                FilledIconButton(
                    onClick = { onPreviousClick() },
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = TrailIcons.Prev,
                        contentDescription = context.getString(R.string.previous),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            TrailSenseButton(
                text = nextText,
                onClick = { onNextClick() },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private class PhotoMapTextButtonState(
    text: String = ""
) {
    var text by mutableStateOf(text)
    var checked by mutableStateOf(false)
    var isVisible by mutableStateOf(false)
    var isEnabled by mutableStateOf(true)
    var onClick by mutableStateOf({})

    @Composable
    fun Content() {
        Button(
            onClick = onClick,
            enabled = isEnabled,
            shape = CircleShape,
            colors = if (checked) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            }
        ) {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private class PhotoMapIconButtonState(
    icon: ImageVector,
    @StringRes private val contentDescription: Int
) {
    var icon by mutableStateOf(icon)
    var isOn by mutableStateOf(false)
    var onClick by mutableStateOf({})

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        val background = if (isOn) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.primaryContainer
        }
        val foreground = if (isOn) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        }

        Surface(
            onClick = onClick,
            modifier = modifier.size(48.dp),
            shape = CircleShape,
            color = background,
            shadowElevation = TrailSenseElevation.high
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = rememberVectorPainter(icon),
                    contentDescription = stringResource(contentDescription),
                    tint = foreground,
                    modifier = Modifier.size(24.dp)
                )
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
