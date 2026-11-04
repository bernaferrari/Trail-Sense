@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.beacons.ui

import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Surface
import androidx.compose.material3.toShape
import androidx.compose.ui.graphics.luminance
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpotlight
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesBadge
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesBearingField
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesRow
import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSwitchRow
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGroupShape
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.os.BundleCompat
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.luna.text.toDoubleCompat
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.extensions.promptIfUnsavedChanges
import com.kylecorry.trail_sense.shared.camera.SightingCompassDialog
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.shared.views.BeaconIconPickerDialog
import com.kylecorry.trail_sense.shared.views.ColorPickerDialog
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.BearingInput as ComposeBearingInput
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.IBeacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconIcon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.BeaconPickers
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.ClosestBeaconSort
import com.kylecorry.trail_sense.tools.beacons.ui.form.CreateBeaconData
import com.kylecorry.trail_sense.tools.beacons.ui.form.DoesBeaconFormDataHaveChanges
import com.kylecorry.trail_sense.tools.beacons.ui.form.IsBeaconFormDataComplete
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PlaceBeaconToolScreen(
    host: TrailSenseDestinationHost,
    modifier: Modifier = Modifier
) {
    val controller = remember(host) { PlaceBeaconController(host) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(controller, lifecycleOwner) {
        controller.onCreate()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> controller.onResume()
                Lifecycle.Event.ON_PAUSE -> controller.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            controller.onResume()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.onDestroy()
        }
    }

    controller.Content(modifier = modifier.fillMaxSize())
}

private class PlaceBeaconController(private val host: TrailSenseDestinationHost) {

    private val beaconService by lazy { BeaconService(host.requireContext()) }
    private val formatter by lazy { FormatService.getInstance(host.requireContext()) }
    private val prefs by lazy { UserPreferences(host.requireContext()) }
    private val units by lazy { prefs.baseDistanceUnits }

    private val isComplete = IsBeaconFormDataComplete()
    private var hasChanges = DoesBeaconFormDataHaveChanges(CreateBeaconData.empty)

    private var editingBeaconId by mutableStateOf<Long?>(null)
    private var initialLocation: GeoUri? = null
    private var backCallback: OnBackPressedCallback? = null

    private var data by mutableStateOf(CreateBeaconData.empty)
    private var groupName by mutableStateOf("")
    private var locationAutofillRequest by mutableStateOf(0)
    private var elevationAutofillRequest by mutableStateOf(0)
    private var compassBearingText by mutableStateOf("")

    private val compass by lazy { SensorService(host.requireContext()).getCompass() }
    private val hasCompass by lazy { SensorService(host.requireContext()).hasCompass() }
    private var cameraSheet: SightingCompassDialog? = null
    private var isInitialized = false

    fun onCreate() {
        if (isInitialized) {
            return
        }
        isInitialized = true

        val args = host.destinationArguments
        val beaconId = args?.getLong("edit_beacon") ?: 0L
        val groupId = args?.getLong("initial_group") ?: 0L
        initialLocation = BundleCompat.getParcelable(
            args ?: Bundle(),
            "initial_location",
            GeoUri::class.java
        )

        editingBeaconId = if (beaconId == 0L) null else beaconId
        data = data.copy(groupId = if (groupId == 0L) null else groupId)
        backCallback = host.promptIfUnsavedChanges { hasChanges.isSatisfiedBy(data) }

        updateBeaconGroupName()

        editingBeaconId?.let {
            loadExistingBeacon(it)
        } ?: run {
            val defaultColor = prefs.beacons.defaultBeaconColor
            updateData(data.copy(color = defaultColor))
            hasChanges = DoesBeaconFormDataHaveChanges(
                CreateBeaconData.empty.copy(color = defaultColor)
            )
        }

        initialLocation?.let {
            updateData(CreateBeaconData.from(it).copy(groupId = data.groupId, color = data.color))
        }
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        var showColorPicker by remember { mutableStateOf(false) }
        var showIconPicker by remember { mutableStateOf(false) }
        val colorTitle = stringResource(R.string.color)
        val iconTitle = stringResource(R.string.icon)

        PlaceBeaconScreen(
            data = data,
            isEditing = editingBeaconId != null,
            groupName = groupName.ifBlank { stringResource(R.string.no_group) },
            canSave = isComplete.isSatisfiedBy(data),
            hikingDistanceUnits = formatter.sortDistanceUnits(DistanceUtils.hikingDistanceUnits),
            onNameChange = { updateData(data.copy(name = it)) },
            onCoordinateChange = { updateData(data.copy(coordinate = it)) },
            onElevationChange = { updateData(data.copy(elevation = it)) },
            onCreateAtDistanceChange = {
                updateData(data.copy(createAtDistance = it))
            },
            onDistanceChange = { updateData(data.copy(distanceTo = it)) },
            onBearingChange = { bearing, isTrueNorth ->
                updateData(
                    data.copy(
                        bearingTo = bearing,
                        bearingIsTrueNorth = isTrueNorth
                    )
                )
            },
            onNotesChange = { updateData(data.copy(notes = it)) },
            onGroupClick = ::pickGroup,
            onColorClick = { showColorPicker = true },
            onIconClick = { showIconPicker = true },
            onSave = ::onSubmit,
            locationAutofillRequest = locationAutofillRequest,
            onAutoLocationClick = {
                if (data.elevation == null) {
                    elevationAutofillRequest++
                }
            },
            onCoordinateBeaconSelected = {
                if (data.elevation == null) {
                    elevationAutofillRequest++
                }
            },
            elevationAutofillRequest = elevationAutofillRequest,
            compassBearingText = compassBearingText,
            hasCompass = hasCompass,
            onCompassBearingClick = ::setBearingFromCompass,
            onCameraBearingClick = ::setBearingFromCamera,
            modifier = modifier
        )

        if (showColorPicker) {
            BeaconShapeSheet(
                icon = data.icon?.icon ?: TrailIcons.Location,
                color = data.color,
                shape = data.shape,
                onShape = { updateData(data.copy(shape = it)) },
                onColor = { updateData(data.copy(color = it)) },
                onDismiss = { showColorPicker = false }
            )
        }

        if (showIconPicker) {
            BeaconIconPickerDialog(
                title = iconTitle,
                selectedIcon = data.icon,
                icons = BeaconIcon.values().filter { it.isUserSelectable },
                onIconPicked = {
                    updateData(data.copy(icon = it))
                    showIconPicker = false
                },
                onDismiss = {
                    updateData(data.copy(icon = null))
                    showIconPicker = false
                }
            )
        }

        cameraSheet?.Content()
    }

    fun onResume() {
        if (!isInitialized) {
            return
        }
        compass.start {
            val bearing = compass.rawBearing
            compassBearingText = formatter.formatDegrees(bearing, replace360 = true)
            cameraSheet?.bearing = bearing
            true
        }
    }

    fun onPause() {
        compass.stop(null)
    }

    fun onDestroy() {
        onPause()
        backCallback?.remove()
        backCallback = null
        cameraSheet?.dismiss()
        cameraSheet = null
        isInitialized = false
    }

    private fun loadExistingBeacon(beaconId: Long) {
        host.lifecycleScope.launch {
            val beacon = withContext(Dispatchers.IO) {
                beaconService.getBeacon(beaconId)
            }
            beacon?.let {
                val loadedData = CreateBeaconData.from(it)
                hasChanges = DoesBeaconFormDataHaveChanges(loadedData)
                updateData(loadedData)
            }
        }
    }

    private fun updateData(newData: CreateBeaconData) {
        data = newData
        updateBeaconGroupName()
    }

    private fun pickGroup() {
        host.lifecycleScope.launch {
            val result = BeaconPickers.pickGroup(
                host.requireContext(),
                initialGroup = data.groupId
            )
            if (result.first) {
                return@launch
            }
            updateData(data.copy(groupId = result.second?.id))
        }
    }

    private fun setBearingFromCompass() {
        updateData(
            data.copy(
                bearingTo = compass.rawBearing,
                bearingIsTrueNorth = false
            )
        )
    }

    private fun setBearingFromCamera() {
        host.requestCamera { hasPermission ->
            if (hasPermission) {
                if (cameraSheet == null) {
                    cameraSheet = SightingCompassDialog(
                        host.requireContext(),
                        host.lifecycleOwner,
                        host.lifecycleScope
                    ) {
                        updateData(
                            data.copy(
                                bearingTo = it,
                                bearingIsTrueNorth = false
                            )
                        )
                    }
                }
                cameraSheet?.show()
            } else {
                host.alertNoCameraPermission()
            }
        }
    }

    private fun updateBeaconGroupName() {
        val parent = data.groupId
        host.lifecycleScope.launch {
            groupName = withContext(Dispatchers.IO) {
                if (parent == null) {
                    host.getString(R.string.no_group)
                } else {
                    beaconService.getGroup(parent)?.name ?: ""
                }
            }
        }
    }

    private fun onSubmit() {
        val beacon = data.toBeacon() ?: return
        host.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                beaconService.add(beacon)
            }

            backCallback?.remove()
            host.findNavController().navigateUp()
        }
    }
}

/**
 * Create or edit a beacon: each field is a row of a group (the field is the row, no outlines), grouped by what it
 * says about the beacon: its name, where it is, how it looks on the map, and notes. Distance and bearing rows only
 * appear when "create at distance" is on, and the location group grows to make room for them. Save sits in the top
 * bar so it is always reachable.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlaceBeaconScreen(
    data: CreateBeaconData,
    isEditing: Boolean = false,
    groupName: String,
    canSave: Boolean,
    hikingDistanceUnits: List<com.kylecorry.sol.units.DistanceUnits>,
    onNameChange: (String) -> Unit,
    onCoordinateChange: (Coordinate?) -> Unit,
    onElevationChange: (Distance?) -> Unit,
    onCreateAtDistanceChange: (Boolean) -> Unit,
    onDistanceChange: (Distance?) -> Unit,
    onBearingChange: (Float?, Boolean) -> Unit,
    onNotesChange: (String) -> Unit,
    onGroupClick: () -> Unit,
    onColorClick: () -> Unit,
    onIconClick: () -> Unit,
    onSave: () -> Unit,
    locationAutofillRequest: Int,
    onAutoLocationClick: () -> Unit,
    onCoordinateBeaconSelected: (Beacon) -> Unit,
    elevationAutofillRequest: Int,
    compassBearingText: String,
    hasCompass: Boolean,
    onCompassBearingClick: () -> Unit,
    onCameraBearingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val sizeSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntSize>()
    val appearanceTitle = stringResource(R.string.appearance)

    Box(modifier = modifier.testTag("create_beacon_title")) {
        TrailSensePage(
            title = stringResource(if (isEditing) R.string.edit_beacon else R.string.create_beacon),
            onBack = { dispatcher?.onBackPressed() },
            backLabel = stringResource(R.string.back),
            modifier = Modifier.testTag("create_beacon_scroll"),
            actions = {
                Button(
                    onClick = onSave,
                    enabled = canSave,
                    modifier = Modifier
                        .padding(end = TrailSenseSpacing.sm)
                        .testTag("create_beacon_save_btn")
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        ) {
            item(key = "name") {
                TrailSenseRowGroup {
                    row { position ->
                        BeaconNameField(
                            name = data.name,
                            position = position,
                            onNameChange = onNameChange
                        )
                    }
                }
            }

            item(key = "location") {
                TrailSenseRowGroup(
                    title = stringResource(R.string.location),
                    modifier = Modifier.animateContentSize(sizeSpec)
                ) {
                    row { position ->
                        CoordinateInput(
                            coordinate = data.coordinate,
                            position = position,
                            onCoordinateChange = onCoordinateChange,
                            autoFillRequest = locationAutofillRequest,
                            onAutoLocationClick = onAutoLocationClick,
                            onBeaconSelected = onCoordinateBeaconSelected
                        )
                    }
                    row { position ->
                        ElevationInput(
                            elevation = data.elevation,
                            position = position,
                            onElevationChange = onElevationChange,
                            autoFillRequest = elevationAutofillRequest
                        )
                    }
                    row { position ->
                        TrailSenseSwitchRow(
                            label = stringResource(R.string.create_at_distance),
                            checked = data.createAtDistance,
                            onCheckedChange = onCreateAtDistanceChange,
                            position = position,
                            modifier = Modifier.testTag("create_at_distance")
                        )
                    }
                    if (data.createAtDistance) {
                        row { position ->
                            DistanceInput(
                                distance = data.distanceTo,
                                units = hikingDistanceUnits,
                                position = position,
                                onDistanceChange = onDistanceChange
                            )
                        }
                        row { position ->
                            PlacesBearingField(
                                bearing = data.bearingTo,
                                trueNorth = data.bearingIsTrueNorth,
                                compassBearingText = compassBearingText,
                                hasCompass = hasCompass,
                                onBearingChange = onBearingChange,
                                onCompassClick = onCompassBearingClick,
                                onCameraClick = onCameraBearingClick,
                                position = position,
                                tag = "bearing_to"
                            )
                        }
                    }
                }
            }

            trailSenseSection(key = "appearance", title = appearanceTitle) {
                row { position ->
                    PlacesRow(
                        title = stringResource(R.string.group),
                        subtitle = groupName,
                        leading = {
                            com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowWell(androidx.compose.ui.graphics.vector.rememberVectorPainter(TrailIcons.BeaconGroup))
                        },
                        position = position,
                        onClick = onGroupClick,
                        modifier = Modifier.testTag("beacon_group_picker")
                    )
                }
                row { position ->
                    val color = Color(data.color.color)
                    PlacesRow(
                        title = stringResource(R.string.places_shape_and_color),
                        leading = {
                            BeaconShapeMark(
                                shape = data.shape,
                                color = color,
                                icon = data.icon?.icon ?: TrailIcons.Location,
                                size = 44.dp
                            )
                        },
                        position = position,
                        onClick = onColorClick,
                        modifier = Modifier.testTag("beacon_color_picker")
                    )
                }
                row { position ->
                    val color = Color(data.color.color)
                    PlacesRow(
                        title = stringResource(R.string.icon),
                        leading = {
                            PlacesBadge(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = color,
                                icon = data.icon?.icon ?: TrailIcons.Location,
                                shape = MaterialShapes.Cookie4Sided.toShape(),
                                size = 44.dp
                            )
                        },
                        position = position,
                        onClick = onIconClick,
                        modifier = Modifier.testTag("beacon_icon_picker")
                    )
                }
            }

            item(key = "notes") {
                TrailSenseRowGroup {
                    row { position ->
                        BeaconFormField(
                            value = data.notes,
                            onValueChange = onNotesChange,
                            label = stringResource(R.string.beacon_comment),
                            position = position,
                            singleLine = false,
                            modifier = Modifier.testTag("comment")
                        )
                    }
                }
            }
        }
    }
}

/**
 * One field of the form as a row of its group: the group's tone and silhouette, no outline or underline, the label
 * inside. Errors still read in the error color with a supporting line.
 */
@Composable
private fun BeaconFormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    position: TrailSenseGroupPosition,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    val container = MaterialTheme.colorScheme.surfaceContainer
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        isError = isError,
        supportingText = if (isError && errorText != null) {
            { Text(errorText) }
        } else {
            null
        },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 4,
        enabled = enabled,
        shape = trailSenseGroupShape(position),
        keyboardOptions = KeyboardOptions(
            capitalization = if (keyboardType == KeyboardType.Text) {
                KeyboardCapitalization.Sentences
            } else {
                KeyboardCapitalization.None
            },
            keyboardType = keyboardType
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = container,
            unfocusedContainerColor = container,
            disabledContainerColor = container,
            errorContainerColor = container,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent
        )
    )
}

@Composable
private fun BeaconNameField(
    name: String,
    position: TrailSenseGroupPosition,
    onNameChange: (String) -> Unit
) {
    var hasError by remember { mutableStateOf(false) }
    var wasFocused by remember { mutableStateOf(false) }
    BeaconFormField(
        value = name,
        onValueChange = {
            onNameChange(it)
            if (hasError && it.isNotBlank()) {
                hasError = false
            }
        },
        label = stringResource(R.string.name),
        position = position,
        isError = hasError,
        errorText = stringResource(R.string.beacon_invalid_name),
        modifier = Modifier
            .testTag("beacon_name")
            .onFocusChanged {
                // Only flag an empty name once the field has been left, not before it was ever touched
                if (wasFocused && !it.isFocused) {
                    hasError = name.isBlank()
                }
                wasFocused = it.isFocused
            }
    )
}

/** A compact unit chooser that sits at the end of a numeric field. */
@Composable
private fun UnitChooser(
    unit: DistanceUnits,
    units: List<DistanceUnits>,
    tag: String,
    onUnitChange: (DistanceUnits) -> Unit
) {
    val context = LocalContext.current
    val formatter = remember { FormatService.getInstance(context) }
    var isOpen by remember { mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { isOpen = true },
            modifier = Modifier
                .padding(end = TrailSenseSpacing.xs)
                .testTag(tag)
        ) {
            Text(formatter.getDistanceUnitName(unit))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null
            )
        }
        DropdownMenu(
            expanded = isOpen,
            onDismissRequest = { isOpen = false }
        ) {
            units.forEach { option ->
                DropdownMenuItem(
                    text = { Text(formatter.getDistanceUnitName(option)) },
                    onClick = {
                        isOpen = false
                        onUnitChange(option)
                    }
                )
            }
        }
    }
}

@Composable
private fun FieldProgress(tag: String) {
    Box(
        modifier = Modifier.size(TrailSenseSize.minimumTouchTarget),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(24.dp)
                .testTag(tag),
            strokeWidth = 2.dp
        )
    }
}

@Composable
private fun CoordinateInput(
    coordinate: Coordinate?,
    position: TrailSenseGroupPosition,
    onCoordinateChange: (Coordinate?) -> Unit,
    autoFillRequest: Int,
    onAutoLocationClick: () -> Unit,
    onBeaconSelected: (Beacon) -> Unit
) {
    val context = LocalContext.current
    val formatter = remember { FormatService.getInstance(context) }
    val sensors = remember { SensorSubsystem.getInstance(context) }
    val gps = remember { SensorService(context).getGPS() }
    val scope = rememberCoroutineScope()
    // The text is the user's: it starts from the coordinate and is replaced only when the coordinate changes from
    // outside (GPS, a picked beacon), never in response to their own typing.
    var coordinateText by remember { mutableStateOf(coordinate?.let { formatter.formatLocation(it) }.orEmpty()) }
    val parsedCoordinate = remember(coordinateText) {
        Coordinate.parse(coordinateText)
    }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    fun autofillFromGps(triggerLinkedAutofill: Boolean = true) {
        if (triggerLinkedAutofill) {
            onAutoLocationClick()
        }
        scope.launch {
            isLoadingLocation = true
            val updated = withContext(Dispatchers.IO) {
                sensors.getLocation(SensorSubsystem.SensorRefreshPolicy.Refresh)
            }
            coordinateText = formatter.formatLocation(updated)
            onCoordinateChange(updated)
            isLoadingLocation = false
        }
    }

    // Follow the coordinate only when it changes from outside (GPS, a picked beacon, an edit loaded later): while
    // typing, a partial entry parses to nothing, and replacing the text then would erase what was typed.
    // A coordinate that arrives later (editing a saved beacon) fills an empty field; GPS and picked beacons set the
    // text themselves.
    LaunchedEffect(coordinate) {
        if (coordinate != null && coordinateText.isBlank()) {
            coordinateText = formatter.formatLocation(coordinate)
        }
    }

    LaunchedEffect(autoFillRequest) {
        if (autoFillRequest > 0) {
            autofillFromGps(triggerLinkedAutofill = false)
        }
    }

    val isInvalid = coordinateText.isNotBlank() && parsedCoordinate == null

    BeaconFormField(
        value = coordinateText,
        onValueChange = {
            coordinateText = it
            onCoordinateChange(Coordinate.parse(it))
        },
        label = stringResource(R.string.location),
        position = position,
        enabled = !isLoadingLocation,
        isError = isInvalid,
        errorText = stringResource(R.string.coordinate_input_invalid_location),
        modifier = Modifier.testTag("beacon_location"),
        leadingIcon = {
            if (isLoadingLocation) {
                FieldProgress("gps_loading")
            } else {
                IconButton(
                    onClick = {
                        Pickers.item(
                            context,
                            context.getString(R.string.autofill_source),
                            listOf(
                                context.getString(R.string.gps),
                                context.getString(R.string.beacon)
                            ),
                            0
                        ) { selection ->
                            when (selection) {
                                0 -> autofillFromGps()
                                1 -> scope.launch {
                                    val beacon = BeaconPickers.pickBeacon(
                                        context,
                                        sort = ClosestBeaconSort(BeaconService(context), gps::location)
                                    ) ?: return@launch
                                    coordinateText = formatter.formatLocation(beacon.coordinate)
                                    onCoordinateChange(beacon.coordinate)
                                    onBeaconSelected(beacon)
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("gps_btn")
                ) {
                    Icon(
                        painter = rememberVectorPainter(TrailIcons.Location),
                        contentDescription = stringResource(R.string.gps),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        trailingIcon = {
            IconButton(
                onClick = { showHelp = true },
                modifier = Modifier.testTag("coordinate_input_help_btn")
            ) {
                Icon(
                    painter = rememberVectorPainter(TrailIcons.Help),
                    contentDescription = stringResource(R.string.location_input_help_title)
                )
            }
        }
    )

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            title = { Text(stringResource(R.string.location_input_help_title)) },
            text = { Text(stringResource(R.string.location_input_help)) }
        )
    }
}

@Composable
private fun ElevationInput(
    elevation: Distance?,
    position: TrailSenseGroupPosition,
    onElevationChange: (Distance?) -> Unit,
    autoFillRequest: Int
) {
    val context = LocalContext.current
    val formatter = remember { FormatService.getInstance(context) }
    val sensors = remember { SensorSubsystem.getInstance(context) }
    val gps = remember { SensorService(context).getGPS() }
    val scope = rememberCoroutineScope()
    val units = remember {
        formatter.sortDistanceUnits(DistanceUtils.elevationDistanceUnits)
    }
    var unit by remember(units) {
        mutableStateOf(elevation?.units ?: UserPreferences(context).baseDistanceUnits)
    }
    var elevationText by remember {
        mutableStateOf(formatDistanceAmount(elevation, unit))
    }
    var isLoadingElevation by remember { mutableStateOf(false) }

    fun publish(text: String = elevationText, newUnit: DistanceUnits = unit) {
        onElevationChange(parseDistance(text, newUnit))
    }

    fun autofillFromAltimeter() {
        scope.launch {
            isLoadingElevation = true
            val updated = withContext(Dispatchers.IO) {
                sensors.getElevation(SensorSubsystem.SensorRefreshPolicy.Refresh)
            }.convertTo(unit)
            elevationText = formatDistanceAmount(updated, unit)
            onElevationChange(updated)
            isLoadingElevation = false
        }
    }

    LaunchedEffect(elevation) {
        val current = parseDistance(elevationText, unit)
        if (current != elevation) {
            val newUnit = elevation?.units ?: unit
            unit = newUnit
            elevationText = formatDistanceAmount(elevation, newUnit)
        }
    }

    LaunchedEffect(autoFillRequest) {
        if (autoFillRequest > 0) {
            autofillFromAltimeter()
        }
    }

    BeaconFormField(
        value = elevationText,
        onValueChange = {
            elevationText = it
            publish(text = it)
        },
        label = stringResource(R.string.elevation),
        position = position,
        enabled = !isLoadingElevation,
        keyboardType = KeyboardType.Decimal,
        modifier = Modifier.testTag("beacon_elevation"),
        leadingIcon = {
            if (isLoadingElevation) {
                FieldProgress("elevation_loading")
            } else {
                IconButton(
                    onClick = {
                        Pickers.item(
                            context,
                            context.getString(R.string.autofill_source),
                            listOf(
                                context.getString(R.string.pref_altimeter_calibration_title),
                                context.getString(R.string.beacon)
                            ),
                            0
                        ) { selection ->
                            when (selection) {
                                0 -> autofillFromAltimeter()
                                1 -> scope.launch {
                                    val beacon = BeaconPickers.pickBeacon(
                                        context,
                                        sort = ClosestBeaconSort(BeaconService(context), gps::location)
                                    ) { beacons: List<IBeacon> ->
                                        beacons.filterIsInstance<Beacon>().filter { it.elevation != null }
                                    } ?: return@launch
                                    val updated = Distance.meters(beacon.elevation!!).convertTo(unit)
                                    elevationText = formatDistanceAmount(updated, unit)
                                    onElevationChange(updated)
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("elevation_gps_btn")
                ) {
                    Icon(
                        painter = rememberVectorPainter(TrailIcons.Altitude),
                        contentDescription = stringResource(R.string.elevation),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        trailingIcon = {
            UnitChooser(
                unit = unit,
                units = units,
                tag = "beacon_elevation_unit"
            ) { option ->
                val converted = parseDistance(elevationText, unit)?.convertTo(option)
                unit = option
                elevationText = formatDistanceAmount(converted, option)
                publish(text = elevationText, newUnit = option)
            }
        }
    )
}

@Composable
private fun DistanceInput(
    distance: Distance?,
    units: List<DistanceUnits>,
    position: TrailSenseGroupPosition,
    onDistanceChange: (Distance?) -> Unit
) {
    var unit by remember(units) {
        mutableStateOf(distance?.units ?: units.firstOrNull() ?: DistanceUnits.Meters)
    }
    var distanceText by remember {
        mutableStateOf(formatDistanceAmount(distance, unit))
    }

    fun publish(text: String = distanceText, newUnit: DistanceUnits = unit) {
        onDistanceChange(parseDistance(text, newUnit))
    }

    LaunchedEffect(distance) {
        val current = parseDistance(distanceText, unit)
        if (current != distance) {
            val newUnit = distance?.units ?: unit
            unit = newUnit
            distanceText = formatDistanceAmount(distance, newUnit)
        }
    }

    BeaconFormField(
        value = distanceText,
        onValueChange = {
            distanceText = it
            publish(text = it)
        },
        label = stringResource(R.string.distance),
        position = position,
        keyboardType = KeyboardType.Decimal,
        modifier = Modifier.testTag("distance_away"),
        trailingIcon = {
            UnitChooser(
                unit = unit,
                units = units,
                tag = "distance_away_unit"
            ) { option ->
                val converted = parseDistance(distanceText, unit)?.convertTo(option)
                unit = option
                distanceText = formatDistanceAmount(converted, option)
                publish(text = distanceText, newUnit = option)
            }
        }
    )
}

private fun parseDistance(text: String, unit: DistanceUnits): Distance? {
    return text.toDoubleCompat()?.let {
        Distance.from(it.toFloat(), unit)
    }
}

private fun formatDistanceAmount(distance: Distance?, unit: DistanceUnits): String {
    val converted = distance?.convertTo(unit) ?: return ""
    return DecimalFormatter.format(
        converted.value,
        Units.getDecimalPlaces(converted.units),
        strict = false
    )
}

@Composable
private fun BearingInput(
    bearing: Float?,
    trueNorth: Boolean,
    onBearingChange: (Float?, Boolean) -> Unit,
    compassBearingText: String,
    hasCompass: Boolean,
    onCompassClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    ComposeBearingInput(
        bearing = bearing,
        trueNorth = trueNorth,
        compassBearingText = compassBearingText,
        hasCompass = hasCompass,
        onBearingChange = onBearingChange,
        onCompassClick = onCompassClick,
        onCameraClick = onCameraClick,
        modifier = Modifier.fillMaxWidth(),
        tag = "bearing_to"
    )
}

@Preview(showBackground = true)
@Composable
private fun PlaceBeaconScreenPreview() {
    TrailSenseTheme {
        PlaceBeaconScreen(
            data = CreateBeaconData.empty.copy(name = "Camp"),
            groupName = "No group",
            canSave = false,
            hikingDistanceUnits = emptyList(),
            onNameChange = {},
            onCoordinateChange = {},
            onElevationChange = {},
            onCreateAtDistanceChange = {},
            onDistanceChange = {},
            onBearingChange = { _, _ -> },
            onNotesChange = {},
            onGroupClick = {},
            onColorClick = {},
            onIconClick = {},
            onSave = {},
            locationAutofillRequest = 0,
            onAutoLocationClick = {},
            onCoordinateBeaconSelected = {},
            elevationAutofillRequest = 0,
            compassBearingText = "120 deg",
            hasCompass = true,
            onCompassBearingClick = {},
            onCameraBearingClick = {}
        )
    }
}
