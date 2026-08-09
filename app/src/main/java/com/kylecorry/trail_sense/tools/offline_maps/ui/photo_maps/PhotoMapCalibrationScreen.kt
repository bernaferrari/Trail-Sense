@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

import android.content.Context
import androidx.activity.OnBackPressedCallback
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.observe
import androidx.annotation.StringRes
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.pickers.Pickers
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
import com.kylecorry.trail_sense.shared.extensions.promptIfUnsavedChanges
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.setLayersWithPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.start
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.stop
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseElevation
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
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

@Composable
fun PhotoMapCalibrationToolScreen(
    host: TrailSenseDestinationHost,
    mapId: Long,
    onRotation: (Float) -> Unit,
    onComplete: () -> Unit,
    onController: (PhotoMapCalibrationController) -> Unit,
    modifier: Modifier = Modifier
) {
    val controller = remember(host, mapId, onRotation, onComplete) {
        PhotoMapCalibrationController(host, mapId, onRotation, onComplete)
    }
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
        }
    }

    controller.Content(modifier = modifier)
}

class PhotoMapCalibrationController(
    private val host: TrailSenseDestinationHost,
    private val mapId: Long,
    private var showRotation: (Float) -> Unit,
    private var onDone: () -> Unit
) {

    private val service = getAppService<MapService>()
    private val prefs by lazy { UserPreferences(host.requireContext()) }
    private var calibrationViews: PhotoMapCalibrationViews? =
        createPhotoMapCalibrationViews(host.requireContext())
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

    private var backCallback: OnBackPressedCallback? = null

    private val manager = MapCalibrationManager(maxPoints) {
        updateMapCalibration()
    }

    // Sensors
    private val sensorService by lazy { SensorService(host.requireContext()) }
    private val gps by lazy { sensorService.getGPS() }
    private val compass by lazy { sensorService.getCompass() }

    private val declinationStrategy by lazy {
        GPSDeclinationStrategy(gps)
    }

    private var showPreview = false

    fun onCreate() {
        CustomUiUtils.disclaimer(
            host.requireContext(),
            host.getString(R.string.map_calibration),
            host.getString(R.string.map_calibration_instructions),
            host.getString(R.string.map_calibration_shown),
            cancelText = null
        )

        views.mapCalibrationBottomPanel.onTitleClick = {
            Alerts.dialog(
                host.requireContext(),
                host.getString(R.string.map_calibration),
                host.getString(R.string.map_calibration_instructions),
                cancelText = null
            )
        }

        reloadMap()

        views.mapCalibrationBottomPanel.onNextClick = {
            if (calibrationIndex == (maxPoints - 1)) {
                host.inBackground {
                    map = map?.let { save(it) }
                    manager.reset(map?.georeference?.calibrationPoints ?: emptyList())
                    backCallback?.remove()
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

        backCallback = host.promptIfUnsavedChanges {
            hasChanges()
        }
    }

    fun onResume() {
        // Populate the last known location
        views.calibrationMap.useDensityPixelsForZoom = !prefs.photoMaps.highDetailMode
        views.calibrationMap.userLocation = gps.location
        views.calibrationMap.userLocationAccuracy =
            gps.horizontalAccuracy?.let { Distance.meters(it) }

        gps.asLiveData().observe(host.lifecycleOwner) {
            views.calibrationMap.userLocation = gps.location
            views.calibrationMap.userLocationAccuracy =
                gps.horizontalAccuracy?.let { Distance.meters(it) }
            compass.declination = declinationStrategy.getDeclination()
        }

        compass.asLiveData().observe(host.lifecycleOwner) {
            views.calibrationMap.userAzimuth = compass.bearing
        }
    }

    fun onPause() {
        calibrationViews?.calibrationMap?.stop()
    }

    fun onDestroy() {
        backCallback?.remove()
        backCallback = null
        calibrationViews = null
    }

    fun reloadMap() {
        host.inBackground {
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
            else -> Color(Resources.androidTextColorSecondary(host.requireContext()))
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
        host.inBackground {
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
            host.requireContext(),
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

    private fun getString(@StringRes id: Int, vararg args: Any): String {
        return host.getString(id, *args)
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
                .padding(end = TrailSenseSpacing.lg, bottom = 232.dp)
        )
        views.zoomOutBtn.Content(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = TrailSenseSpacing.lg, bottom = 176.dp)
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
        zoomOutBtn = PhotoMapIconButtonState(R.drawable.ic_zoom_out),
        zoomInBtn = PhotoMapIconButtonState(R.drawable.ic_add),
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
    var icon by mutableStateOf(R.drawable.ic_info)
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
                .padding(TrailSenseSpacing.lg),
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
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.size(TrailSenseSpacing.md))
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
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_location),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.size(TrailSenseSpacing.sm))

            OutlinedTextField(
                value = coordinateText,
                onValueChange = ::updateCoordinate,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text(context.getString(R.string.location)) },
                isError = coordinateText.isNotBlank() && Coordinate.parse(coordinateText) == null
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
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_help),
                    contentDescription = null,
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
                        painter = painterResource(R.drawable.ic_prev),
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
    icon: Int
) {
    var iconRes by mutableIntStateOf(icon)
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
            modifier = modifier.size(44.dp),
            shape = CircleShape,
            color = background,
            shadowElevation = TrailSenseElevation.high
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
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
