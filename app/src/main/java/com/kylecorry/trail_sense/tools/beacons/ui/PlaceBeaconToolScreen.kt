package com.kylecorry.trail_sense.tools.beacons.ui

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.os.BundleCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.andromeda.pickers.Pickers
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
import com.kylecorry.trail_sense.shared.permissions.alertNoCameraPermission
import com.kylecorry.trail_sense.shared.permissions.requestCamera
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.shared.views.BeaconIconPickerDialog
import com.kylecorry.trail_sense.shared.views.ColorPickerDialog
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.BearingInput as ComposeBearingInput
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
    fragment: Fragment,
    modifier: Modifier = Modifier
) {
    val controller = remember(fragment) { PlaceBeaconController(fragment) }
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

private class PlaceBeaconController(private val fragment: Fragment) {

    private val beaconService by lazy { BeaconService(fragment.requireContext()) }
    private val formatter by lazy { FormatService.getInstance(fragment.requireContext()) }
    private val prefs by lazy { UserPreferences(fragment.requireContext()) }
    private val units by lazy { prefs.baseDistanceUnits }

    private val isComplete = IsBeaconFormDataComplete()
    private var hasChanges = DoesBeaconFormDataHaveChanges(CreateBeaconData.empty)

    private var editingBeaconId: Long? = null
    private var initialLocation: GeoUri? = null
    private var backCallback: OnBackPressedCallback? = null

    private var data by mutableStateOf(CreateBeaconData.empty)
    private var groupName by mutableStateOf("")
    private var locationAutofillRequest by mutableStateOf(0)
    private var elevationAutofillRequest by mutableStateOf(0)
    private var compassBearingText by mutableStateOf("")

    private val compass by lazy { SensorService(fragment.requireContext()).getCompass() }
    private val hasCompass by lazy { SensorService(fragment.requireContext()).hasCompass() }
    private var cameraSheet: SightingCompassDialog? = null
    private var isInitialized = false

    fun onCreate() {
        if (isInitialized) {
            return
        }
        isInitialized = true

        val beaconId = fragment.arguments?.getLong("edit_beacon") ?: 0L
        val groupId = fragment.arguments?.getLong("initial_group") ?: 0L
        initialLocation = BundleCompat.getParcelable(
            fragment.arguments ?: Bundle(),
            "initial_location",
            GeoUri::class.java
        )

        editingBeaconId = if (beaconId == 0L) null else beaconId
        data = data.copy(groupId = if (groupId == 0L) null else groupId)
        backCallback = fragment.promptIfUnsavedChanges { hasChanges.isSatisfiedBy(data) }

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
            ColorPickerDialog(
                title = colorTitle,
                selectedColor = data.color,
                onColorPicked = {
                    if (it != null) {
                        updateData(data.copy(color = it))
                    }
                    showColorPicker = false
                },
                onDismiss = {
                    showColorPicker = false
                }
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
        cameraSheet = null
        isInitialized = false
    }

    private fun loadExistingBeacon(beaconId: Long) {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
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
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            val result = BeaconPickers.pickGroup(
                fragment.requireContext(),
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
        fragment.requestCamera { hasPermission ->
            if (hasPermission) {
                if (cameraSheet == null) {
                    cameraSheet = SightingCompassDialog(fragment) {
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
                fragment.alertNoCameraPermission()
            }
        }
    }

    private fun updateBeaconGroupName() {
        val parent = data.groupId
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            groupName = withContext(Dispatchers.IO) {
                if (parent == null) {
                    fragment.getString(R.string.no_group)
                } else {
                    beaconService.getGroup(parent)?.name ?: ""
                }
            }
        }
    }

    private fun onSubmit() {
        val beacon = data.toBeacon() ?: return
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                beaconService.add(beacon)
            }

            backCallback?.remove()
            fragment.findNavController().navigateUp()
        }
    }
}

@Composable
private fun PlaceBeaconScreen(
    data: CreateBeaconData,
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
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlaceBeaconHeader(
                title = stringResource(R.string.create_beacon),
                canSave = canSave,
                onSave = onSave
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .testTag("create_beacon_scroll")
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 96.dp)
            ) {
                TextInput(
                    text = data.name,
                    label = stringResource(R.string.name),
                    tag = "beacon_name",
                    errorText = stringResource(R.string.beacon_invalid_name),
                    validateOnBlur = { it.isNotBlank() },
                    onTextChange = onNameChange
                )
                Spacer(modifier = Modifier.height(24.dp))
                CoordinateInput(
                    coordinate = data.coordinate,
                    onCoordinateChange = onCoordinateChange,
                    autoFillRequest = locationAutofillRequest,
                    onAutoLocationClick = onAutoLocationClick,
                    onBeaconSelected = onCoordinateBeaconSelected
                )
                Spacer(modifier = Modifier.height(24.dp))
                ElevationInput(
                    elevation = data.elevation,
                    onElevationChange = onElevationChange,
                    autoFillRequest = elevationAutofillRequest
                )
                Spacer(modifier = Modifier.height(24.dp))
                CreateAtDistanceSwitch(
                    checked = data.createAtDistance,
                    onCheckedChange = onCreateAtDistanceChange
                )
                if (data.createAtDistance) {
                    Spacer(modifier = Modifier.height(24.dp))
                    DistanceInput(
                        distance = data.distanceTo,
                        units = hikingDistanceUnits,
                        onDistanceChange = onDistanceChange
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    BearingInput(
                        bearing = data.bearingTo,
                        trueNorth = data.bearingIsTrueNorth,
                        onBearingChange = onBearingChange,
                        compassBearingText = compassBearingText,
                        hasCompass = hasCompass,
                        onCompassClick = onCompassBearingClick,
                        onCameraClick = onCameraBearingClick
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                PickerRow(
                    tag = "beacon_group_picker",
                    text = groupName,
                    icon = R.drawable.ic_beacon_group,
                    iconTint = AppColor.Gray.color,
                    onClick = onGroupClick
                )
                Spacer(modifier = Modifier.height(24.dp))
                PickerRow(
                    tag = "beacon_color_picker",
                    text = stringResource(R.string.color),
                    icon = R.drawable.bubble,
                    iconTint = data.color.color,
                    onClick = onColorClick
                )
                Spacer(modifier = Modifier.height(24.dp))
                PickerRow(
                    tag = "beacon_icon_picker",
                    text = stringResource(R.string.icon),
                    icon = data.icon?.icon ?: R.drawable.bubble,
                    iconTint = null,
                    onClick = onIconClick
                )
                Spacer(modifier = Modifier.height(24.dp))
                TextInput(
                    text = data.notes,
                    label = stringResource(R.string.beacon_comment),
                    tag = "comment",
                    singleLine = false,
                    onTextChange = onNotesChange
                )
            }
        }
    }
}

@Composable
private fun PlaceBeaconHeader(
    title: String,
    canSave: Boolean,
    onSave: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 18.dp)
                    .testTag("create_beacon_title"),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (canSave) {
                IconButton(
                    onClick = onSave,
                    modifier = Modifier.testTag("create_beacon_save_btn")
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
private fun TextInput(
    text: String,
    label: String,
    tag: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    errorText: String? = null,
    validateOnBlur: ((String) -> Boolean)? = null
) {
    var hasError by androidx.compose.runtime.remember { mutableStateOf(false) }

    OutlinedTextField(
        value = text,
        onValueChange = {
            onTextChange(it)
            if (hasError && validateOnBlur?.invoke(it) != false) {
                hasError = false
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .testTag(tag)
            .onFocusChanged {
                if (!it.isFocused && validateOnBlur != null) {
                    hasError = !validateOnBlur(text)
                }
            },
        label = { Text(label) },
        isError = hasError,
        supportingText = {
            if (hasError && errorText != null) {
                Text(errorText)
            }
        },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 4
    )
}

@Composable
private fun CoordinateInput(
    coordinate: Coordinate?,
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
    var coordinateText by remember(coordinate, formatter) {
        mutableStateOf(coordinate?.let { formatter.formatLocation(it) }.orEmpty())
    }
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

    LaunchedEffect(coordinate) {
        val formatted = coordinate?.let { formatter.formatLocation(it) }.orEmpty()
        if (coordinateText != formatted) {
            coordinateText = formatted
        }
    }

    LaunchedEffect(autoFillRequest) {
        if (autoFillRequest > 0) {
            autofillFromGps(triggerLinkedAutofill = false)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLoadingLocation) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(40.dp)
                    .testTag("gps_loading")
            )
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
                    painter = painterResource(R.drawable.ic_location),
                    contentDescription = stringResource(R.string.gps)
                )
            }
        }
        Spacer(modifier = Modifier.size(8.dp))
        OutlinedTextField(
            value = coordinateText,
            onValueChange = {
                coordinateText = it
                onCoordinateChange(Coordinate.parse(it))
            },
            modifier = Modifier
                .weight(1f)
                .testTag("beacon_location"),
            label = { Text(stringResource(R.string.location)) },
            isError = coordinateText.isNotBlank() && parsedCoordinate == null,
            supportingText = {
                if (coordinateText.isNotBlank() && parsedCoordinate == null) {
                    Text(stringResource(R.string.coordinate_input_invalid_location))
                }
            },
            singleLine = true,
            enabled = !isLoadingLocation
        )
        Spacer(modifier = Modifier.size(8.dp))
        IconButton(
            onClick = { showHelp = true },
            modifier = Modifier.testTag("coordinate_input_help_btn")
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_help),
                contentDescription = stringResource(R.string.location_input_help_title)
            )
        }
    }

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
    var isUnitOpen by remember { mutableStateOf(false) }

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

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        if (isLoadingElevation) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(40.dp)
                    .testTag("elevation_loading")
            )
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
                    painter = painterResource(R.drawable.ic_altitude),
                    contentDescription = stringResource(R.string.elevation)
                )
            }
        }
        Spacer(modifier = Modifier.size(8.dp))
        OutlinedTextField(
            value = elevationText,
            onValueChange = {
                elevationText = it
                publish(text = it)
            },
            modifier = Modifier
                .weight(1f)
                .testTag("beacon_elevation"),
            label = { Text(stringResource(R.string.elevation)) },
            singleLine = true,
            enabled = !isLoadingElevation
        )
        Spacer(modifier = Modifier.size(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { isUnitOpen = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("beacon_elevation_unit")
            ) {
                Text(formatter.getDistanceUnitName(unit))
            }
            DropdownMenu(
                expanded = isUnitOpen,
                onDismissRequest = { isUnitOpen = false }
            ) {
                units.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(formatter.getDistanceUnitName(option)) },
                        onClick = {
                            isUnitOpen = false
                            val converted = parseDistance(elevationText, unit)?.convertTo(option)
                            unit = option
                            elevationText = formatDistanceAmount(converted, option)
                            publish(text = elevationText, newUnit = option)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateAtDistanceSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("create_at_distance"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.create_at_distance),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun DistanceInput(
    distance: Distance?,
    units: List<DistanceUnits>,
    onDistanceChange: (Distance?) -> Unit
) {
    val context = LocalContext.current
    val formatter = remember { FormatService.getInstance(context) }
    var unit by remember(units) {
        mutableStateOf(distance?.units ?: units.firstOrNull() ?: DistanceUnits.Meters)
    }
    var distanceText by remember {
        mutableStateOf(formatDistanceAmount(distance, unit))
    }
    var isUnitOpen by remember { mutableStateOf(false) }

    fun publish(text: String = distanceText, newUnit: DistanceUnits = unit) {
        onDistanceChange(parseDistance(text, newUnit))
    }

    androidx.compose.runtime.LaunchedEffect(distance) {
        val current = parseDistance(distanceText, unit)
        if (current != distance) {
            val newUnit = distance?.units ?: unit
            unit = newUnit
            distanceText = formatDistanceAmount(distance, newUnit)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        OutlinedTextField(
            value = distanceText,
            onValueChange = {
                distanceText = it
                publish(text = it)
            },
            modifier = Modifier
                .weight(1f)
                .testTag("distance_away"),
            label = { Text(stringResource(R.string.distance)) },
            singleLine = true
        )
        Spacer(modifier = Modifier.size(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { isUnitOpen = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("distance_away_unit")
            ) {
                Text(formatter.getDistanceUnitName(unit))
            }
            DropdownMenu(
                expanded = isUnitOpen,
                onDismissRequest = { isUnitOpen = false }
            ) {
                units.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(formatter.getDistanceUnitName(option)) },
                        onClick = {
                            isUnitOpen = false
                            val converted = parseDistance(distanceText, unit)?.convertTo(option)
                            unit = option
                            distanceText = formatDistanceAmount(converted, option)
                            publish(text = distanceText, newUnit = option)
                        }
                    )
                }
            }
        }
    }
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

@Composable
private fun PickerRow(
    tag: String,
    text: String,
    @DrawableRes icon: Int,
    iconTint: Int?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(4.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier
                .padding(end = 8.dp)
                .size(24.dp),
            tint = iconTint?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
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
