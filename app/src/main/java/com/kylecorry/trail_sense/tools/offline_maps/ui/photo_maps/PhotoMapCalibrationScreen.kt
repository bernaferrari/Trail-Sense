@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.annotation.StringRes
import com.kylecorry.trail_sense.shared.dialogs.Alerts
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
import com.kylecorry.trail_sense.shared.views.compose.UnsavedChangesBackHandler
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.setLayersWithPreferences
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PercentCoordinate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.receiveAsFlow
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
internal fun PhotoMapCalibrationToolScreen(
    mapId: Long,
    viewModelKey: String,
    commands: Flow<PhotoMapChildCommand>,
    onRotation: (Float) -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember { UserPreferences(context) }
    val viewModel = viewModel(key = viewModelKey) {
        PhotoMapCalibrationViewModel(getAppService(), mapId)
    }
    val calibrationMap = remember { MapCalibrationView(context) }
    val panel = remember { PhotoMapCalibrationPanelState(context) }
    val currentOnRotation by rememberUpdatedState(onRotation)
    val currentOnComplete by rememberUpdatedState(onComplete)

    UnsavedChangesBackHandler(hasChanges = viewModel.hasUnsavedChanges)

    LaunchedEffect(Unit) {
        CustomUiUtils.disclaimer(
            context,
            context.getString(R.string.map_calibration),
            context.getString(R.string.map_calibration_instructions),
            context.getString(R.string.map_calibration_shown),
            cancelText = null
        )
    }

    // The calibration view shows the map as calibrated so far
    LaunchedEffect(calibrationMap, viewModel.mapVersion) {
        val map = viewModel.map ?: return@LaunchedEffect
        calibrationMap.mapAzimuth = 0f
        calibrationMap.keepMapUp = true
        calibrationMap.showMap(map)
        calibrationMap.highlightedIndex = viewModel.highlightedIndex
    }

    LaunchedEffect(calibrationMap) {
        calibrationMap.onMapClick = { viewModel.calibrate(it) }
    }

    // Choosing a point fills in its location
    LaunchedEffect(viewModel.pointSelection) {
        if (viewModel.pointSelection > 0) {
            panel.coordinate = viewModel.selectedPointLocation
        }
    }

    LaunchedEffect(viewModel.rotation) {
        viewModel.rotation?.let { currentOnRotation(it) }
    }

    // The preview draws the paths, location and beacons over the map
    LaunchedEffect(calibrationMap) {
        snapshotFlow { viewModel.showPreview }.drop(1).collect { showPreview ->
            calibrationMap.stop()
            val layers = if (showPreview) listOf(
                PathGeoJsonSource.SOURCE_ID,
                MyLocationGeoJsonSource.SOURCE_ID,
                BeaconGeoJsonSource.SOURCE_ID
            ) else emptyList()
            calibrationMap.setLayersWithPreferences(
                OfflineMapsToolRegistration.PHOTO_MAPS_ID,
                layers
            )
            if (showPreview) {
                calibrationMap.start()
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.completed.collect { currentOnComplete() }
    }

    LaunchedEffect(commands) {
        commands.collect {
            when (it) {
                PhotoMapChildCommand.Recenter -> calibrationMap.recenter()
                PhotoMapChildCommand.Reload -> viewModel.load()
                else -> Unit
            }
        }
    }

    // Sensors: the user's location and heading on the map
    DisposableEffect(lifecycleOwner, calibrationMap) {
        val sensorService = SensorService(context)
        val gps = sensorService.getGPS()
        val compass = sensorService.getCompass()
        val declinationStrategy = GPSDeclinationStrategy(gps)
        val gpsData = gps.asLiveData()
        val compassData = compass.asLiveData()
        val gpsObserver = Observer<Any> {
            calibrationMap.userLocation = gps.location
            calibrationMap.userLocationAccuracy = gps.horizontalAccuracy?.let { Distance.meters(it) }
            compass.declination = declinationStrategy.getDeclination()
        }
        val compassObserver = Observer<Any> {
            calibrationMap.userAzimuth = compass.bearing
        }
        gpsData.observe(lifecycleOwner, gpsObserver)
        compassData.observe(lifecycleOwner, compassObserver)
        onDispose {
            gpsData.removeObserver(gpsObserver)
            compassData.removeObserver(compassObserver)
        }
    }

    LifecycleResumeEffect(calibrationMap) {
        // Populate the last known location
        val gps = SensorService(context).getGPS()
        calibrationMap.useDensityPixelsForZoom = !prefs.photoMaps.highDetailMode
        calibrationMap.userLocation = gps.location
        calibrationMap.userLocationAccuracy = gps.horizontalAccuracy?.let { Distance.meters(it) }
        onPauseOrDispose {
            calibrationMap.stop()
        }
    }

    val validation = viewModel.validation
    val isValid = validation == MapCalibrationValidationResult.Valid
    val notice = getCalibrationNotice(validation)
    val isPointCalibrated = viewModel.isCurrentPointCalibrated

    PhotoMapCalibrationHost(
        calibrationMap = calibrationMap,
        panel = panel,
        showPanel = viewModel.isPanelVisible,
        title = stringResource(
            R.string.calibrate_map_point_title,
            viewModel.calibrationIndex + 1,
            viewModel.maxPoints
        ),
        notice = notice,
        icon = when {
            notice.isNotBlank() -> TrailIcons.Alert
            isPointCalibrated -> TrailIcons.CheckOutline
            else -> TrailIcons.Info
        },
        iconTint = when {
            notice.isNotBlank() -> Color(getErrorColor(context))
            isPointCalibrated -> Color(AppColor.Green.color)
            else -> Color(Resources.androidTextColorSecondary(context))
        },
        nextText = stringResource(
            when {
                viewModel.calibrationIndex != viewModel.maxPoints - 1 -> R.string.next
                isValid -> R.string.finish_calibration
                else -> R.string.save_draft
            }
        ),
        showPrevious = viewModel.calibrationIndex == 1,
        showPreviewButton = viewModel.isFullyCalibrated,
        isPreviewChecked = viewModel.showPreview,
        isPreviewEnabled = isValid,
        onPreviewClick = viewModel::togglePreview,
        onTitleClick = {
            Alerts.dialog(
                context,
                context.getString(R.string.map_calibration),
                context.getString(R.string.map_calibration_instructions),
                cancelText = null
            )
        },
        onPreviousClick = viewModel::previous,
        onNextClick = viewModel::next,
        onCoordinateChange = viewModel::calibrate,
        onZoomIn = { calibrationMap.zoomBy(2f) },
        onZoomOut = { calibrationMap.zoomBy(0.5f) },
        modifier = modifier
    )
}

@Composable
private fun getCalibrationNotice(result: MapCalibrationValidationResult): String {
    return when (result) {
        MapCalibrationValidationResult.Valid -> ""
        MapCalibrationValidationResult.Uncalibrated -> ""
        MapCalibrationValidationResult.SamePixel ->
            stringResource(R.string.map_calibration_error_same_pixel)

        MapCalibrationValidationResult.SameImageAxis ->
            stringResource(R.string.map_calibration_error_same_image_axis)

        MapCalibrationValidationResult.SameLocation ->
            stringResource(R.string.map_calibration_error_same_location)

        MapCalibrationValidationResult.ImplausibleScale ->
            stringResource(R.string.map_calibration_error_implausible_scale)
    }
}

private fun getErrorColor(context: Context): Int {
    return Resources.getAndroidColorAttr(
        context,
        androidx.appcompat.R.attr.colorError
    )
}

internal class PhotoMapCalibrationViewModel(
    private val service: MapService,
    private val mapId: Long
) : ViewModel() {

    val maxPoints = 2

    /** The map with the calibration so far (and the preview's rotation). */
    var map by mutableStateOf<PhotoMap?>(null)
        private set

    /** Changes whenever the calibration view should show [map] again. */
    var mapVersion by mutableIntStateOf(0)
        private set

    var calibrationIndex by mutableIntStateOf(0)
        private set

    /** The point the calibration view highlights (and moves to); it follows the index once a point changes. */
    var highlightedIndex by mutableIntStateOf(0)
        private set

    var showPreview by mutableStateOf(false)
        private set
    var isFullyCalibrated by mutableStateOf(false)
        private set
    var validation by mutableStateOf(MapCalibrationValidationResult.Uncalibrated)
        private set
    var isCurrentPointCalibrated by mutableStateOf(false)
        private set
    var isPanelVisible by mutableStateOf(false)
        private set

    /** Counts point selections; the panel shows [selectedPointLocation] each time. */
    var pointSelection by mutableIntStateOf(0)
        private set
    var selectedPointLocation: Coordinate? = null
        private set

    /** The map's rotation from the nearest quarter turn. */
    var rotation by mutableStateOf<Float?>(null)
        private set

    /** Whether back should ask before leaving: the calibration changed and hasn't been saved. */
    var hasUnsavedChanges by mutableStateOf(false)
        private set

    private val _completed = Channel<Unit>(Channel.BUFFERED)
    val completed: Flow<Unit> = _completed.receiveAsFlow()

    private val rotationCalculator = MapRotationCalculator()
    private var originalRotation = 0f
    private var isSaved = false

    private val manager = MapCalibrationManager(maxPoints) {
        updateMapCalibration()
        updateHasUnsavedChanges()
    }

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val loaded = service.getPhotoMap(mapId)
            map = loaded
            loaded?.let(::onMapLoad)
        }
    }

    fun calibrate(location: PercentCoordinate) {
        manager.calibrate(calibrationIndex, location)
    }

    fun calibrate(coordinate: Coordinate?) {
        manager.calibrate(calibrationIndex, coordinate)
    }

    fun next() {
        if (calibrationIndex == (maxPoints - 1)) {
            viewModelScope.launch {
                map = map?.let { save(it) }
                manager.reset(map?.georeference?.calibrationPoints ?: emptyList())
                isSaved = true
                updateHasUnsavedChanges()
                _completed.send(Unit)
            }
        } else {
            calibrationIndex++
            selectPoint(calibrationIndex)
        }
    }

    fun previous() {
        calibrationIndex--
        selectPoint(calibrationIndex)
    }

    fun togglePreview() {
        if (validateCalibration() == MapCalibrationValidationResult.Valid) {
            setPreviewMode(!showPreview)
        } else {
            updateCompletionState()
        }
    }

    private fun onMapLoad(map: PhotoMap) {
        this.map = map
        originalRotation = map.georeference.rotation
        mapVersion++
        calibrateMap()
    }

    private fun updateMapCalibration() {
        isFullyCalibrated = hasAllPoints()

        map = map?.copy(
            georeference = map!!.georeference.copy(
                calibrationPoints = manager.getCalibration(false)
            )
        )

        val isValid = validateCalibration() == MapCalibrationValidationResult.Valid

        if ((!isFullyCalibrated || !isValid) && showPreview) {
            setPreviewMode(false)
        }

        map = map?.copy(
            georeference = map!!.georeference.copy(
                rotation = if (showPreview) rotationCalculator.calculate(map!!) else originalRotation
            )
        )
        map ?: return
        highlightedIndex = calibrationIndex
        mapVersion++
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

        this.rotation = Trigonometry.deltaAngle(baseRotation, rotation)
    }

    private fun calibrateMap() {
        val map = map ?: return
        manager.reset(map.georeference.calibrationPoints)
        updateHasUnsavedChanges()

        // Find the first uncalibrated point
        calibrationIndex = manager.getNextUncalibratedIndex().coerceAtLeast(0)

        selectPoint(calibrationIndex)
    }

    private fun selectPoint(index: Int) {
        selectedPointLocation = if (manager.isCalibrated(index)) {
            manager.getCalibrationPoint(index).location
        } else {
            null
        }
        pointSelection++
        isPanelVisible = true
        updateCompletionState()
    }

    private fun updateCompletionState() {
        validation = validateCalibration()
        isCurrentPointCalibrated = manager.isCalibrated(calibrationIndex)
    }

    private fun setPreviewMode(enabled: Boolean) {
        if (showPreview == enabled) {
            return
        }
        showPreview = enabled
        updateMapCalibration()
    }

    private fun hasAllPoints(): Boolean {
        return manager.getCalibration(true).size == maxPoints
    }

    private fun validateCalibration(): MapCalibrationValidationResult {
        val map = map
        return if (map != null && hasAllPoints()) {
            MapCalibrationValidator.validate(map)
        } else {
            MapCalibrationValidationResult.Uncalibrated
        }
    }

    private suspend fun save(map: PhotoMap): PhotoMap {
        var updated = service.getPhotoMap(map.id) ?: return map
        updated =
            updated.copy(georeference = updated.georeference.copy(calibrationPoints = manager.getCalibration()))
        service.add(updated)
        return updated
    }

    private fun updateHasUnsavedChanges() {
        hasUnsavedChanges = !isSaved && manager.hasChanges()
    }
}

@Suppress("FunctionNaming")
@Composable
private fun PhotoMapCalibrationHost(
    calibrationMap: MapCalibrationView?,
    panel: PhotoMapCalibrationPanelState,
    showPanel: Boolean,
    title: String,
    notice: String,
    icon: ImageVector,
    iconTint: Color,
    nextText: String,
    showPrevious: Boolean,
    showPreviewButton: Boolean,
    isPreviewChecked: Boolean,
    isPreviewEnabled: Boolean,
    onPreviewClick: () -> Unit,
    onTitleClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onCoordinateChange: (Coordinate?) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        calibrationMap?.Content(Modifier.fillMaxSize().testTag("calibration_map"))
        if (showPreviewButton) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = TrailSenseSpacing.lg)
            ) {
                PhotoMapTextButton(
                    text = stringResource(R.string.preview),
                    checked = isPreviewChecked,
                    enabled = isPreviewEnabled,
                    onClick = onPreviewClick
                )
            }
        }
        PhotoMapCalibrationIconButton(
            icon = TrailIcons.Add,
            contentDescription = R.string.zoom_in,
            onClick = onZoomIn,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = TrailSenseSpacing.screen, bottom = 232.dp)
        )
        PhotoMapCalibrationIconButton(
            icon = TrailIcons.ZoomOut,
            contentDescription = R.string.zoom_out,
            onClick = onZoomOut,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = TrailSenseSpacing.screen, bottom = 176.dp)
        )
        if (showPanel) {
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                panel.Content(
                    title = title,
                    notice = notice,
                    icon = icon,
                    iconTint = iconTint,
                    nextText = nextText,
                    showPrevious = showPrevious,
                    onTitleClick = onTitleClick,
                    onPreviousClick = onPreviousClick,
                    onNextClick = onNextClick,
                    onCoordinateChange = onCoordinateChange
                )
            }
        }
    }
}

private class PhotoMapCalibrationPanelState(
    private val context: Context
) {

    private val formatService by lazy { FormatService.getInstance(context) }
    private val sensorService by lazy { SensorService(context) }
    private val gps by lazy { sensorService.getGPS() }

    private var coordinateText by mutableStateOf("")
    private var isLoadingLocation by mutableStateOf(false)
    private var coordinateChangeListener: ((Coordinate?) -> Unit)? = null

    var coordinate: Coordinate?
        get() = Coordinate.parse(coordinateText)
        set(value) {
            coordinateText = value?.let { formatService.formatLocation(it) } ?: ""
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
    fun Content(
        title: String,
        notice: String,
        icon: ImageVector,
        iconTint: Color,
        nextText: String,
        showPrevious: Boolean,
        onTitleClick: () -> Unit,
        onPreviousClick: () -> Unit,
        onNextClick: () -> Unit,
        onCoordinateChange: (Coordinate?) -> Unit
    ) {
        coordinateChangeListener = onCoordinateChange
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
            instructionCard(title, notice, icon, iconTint, onTitleClick)
            coordinateCard()
            panelActions(nextText, showPrevious, onPreviousClick, onNextClick)
        }
    }

    @Composable
    private fun instructionCard(
        title: String,
        notice: String,
        icon: ImageVector,
        iconTint: Color,
        onTitleClick: () -> Unit
    ) {
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
    private fun panelActions(
        nextText: String,
        showPrevious: Boolean,
        onPreviousClick: () -> Unit,
        onNextClick: () -> Unit
    ) {
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

@Composable
private fun PhotoMapTextButton(
    text: String,
    checked: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
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

@Composable
private fun PhotoMapCalibrationIconButton(
    icon: ImageVector,
    @StringRes contentDescription: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = TrailSenseElevation.high
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = rememberVectorPainter(icon),
                contentDescription = stringResource(contentDescription),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Suppress("FunctionNaming")
@Preview(showBackground = true)
@Composable
private fun PhotoMapCalibrationHostPreview() {
    TrailSenseTheme {
        PhotoMapCalibrationHost(
            calibrationMap = null,
            panel = PhotoMapCalibrationPanelState(LocalContext.current),
            showPanel = true,
            title = "Calibrate point 1 of 2",
            notice = "",
            icon = TrailIcons.Info,
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
            nextText = "Next",
            showPrevious = false,
            showPreviewButton = true,
            isPreviewChecked = false,
            isPreviewEnabled = true,
            onPreviewClick = {},
            onTitleClick = {},
            onPreviousClick = {},
            onNextClick = {},
            onCoordinateChange = {},
            onZoomIn = {},
            onZoomOut = {}
        )
    }
}
