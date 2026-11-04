@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.field_guide.ui

import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSwitchRow
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.luna.text.toDoubleCompat
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.promptIfUnsavedChanges
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.BeaconPickers
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.ClosestBeaconSort
import com.kylecorry.trail_sense.tools.field_guide.domain.Sighting
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDialog

@Composable
fun CreateFieldGuideSightingToolScreen(host: TrailSenseDestinationHost) {
    val controller = remember(host) { CreateFieldGuideSightingController(host) }

    DisposableEffect(controller) {
        controller.onCreate()
        onDispose {
            controller.onDestroy()
        }
    }

    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    CreateFieldGuideSightingScreen(
        datetime = controller.datetime,
        location = controller.location,
        elevation = controller.elevation,
        notes = controller.notes,
        harvested = controller.harvested,
        showOnMap = controller.showOnMap,
        onDatetimeChange = { controller.datetime = it },
        onLocationChange = { controller.location = it },
        onElevationChange = { controller.elevation = it },
        onNotesChange = { controller.notes = it },
        onHarvestedChange = { controller.harvested = it },
        onShowOnMapChange = { controller.showOnMap = it },
        onSave = controller::save,
        locationAutofillRequest = controller.locationAutofillRequest,
        onAutoLocationClick = {
            if (controller.elevation == null) {
                controller.elevationAutofillRequest++
            }
        },
        onBeaconSelected = controller::onCoordinateBeaconSelected,
        elevationAutofillRequest = controller.elevationAutofillRequest,
        onAutoElevationClick = {
            if (controller.location == null) {
                controller.locationAutofillRequest++
            }
        },
        onBack = { dispatcher?.onBackPressed() }
    )
}

private class CreateFieldGuideSightingController(private val host: TrailSenseDestinationHost) {

    private val repo by lazy { FieldGuideRepo.getInstance(host.requireContext()) }
    private val prefs by lazy { UserPreferences(host.requireContext()) }
    private var isInitialized = false

    private var pageId: Long = 0L
    private var sightingId: Long = 0L
    private var initialSighting by mutableStateOf<Sighting?>(null)
    private var initialDatetime = LocalDateTime.now()

    var datetime by mutableStateOf<LocalDateTime?>(initialDatetime)
    var location by mutableStateOf<Coordinate?>(null)
    var elevation by mutableStateOf<Distance?>(null)
    var notes by mutableStateOf<String?>(null)
    var harvested by mutableStateOf(false)
    var showOnMap by mutableStateOf(true)

    var locationAutofillRequest by mutableStateOf(0)
    var elevationAutofillRequest by mutableStateOf(0)
    private var backCallback: OnBackPressedCallback? = null

    fun onCreate() {
        if (isInitialized) {
            return
        }
        isInitialized = true
        pageId = host.destinationArguments?.getLong("page_id") ?: 0L
        sightingId = host.destinationArguments?.getLong("sighting_id") ?: 0L
        backCallback = host.promptIfUnsavedChanges(this::hasChanges)
        if (sightingId != 0L) {
            loadSighting()
        }
    }

    fun onDestroy() {
        backCallback?.remove()
        backCallback = null
    }

    private fun loadSighting() {
        host.lifecycleScope.launch {
            val sighting = repo.getSighting(sightingId)
            initialSighting = sighting
            sighting ?: return@launch

            val sightingElevation = sighting.altitude?.let {
                Distance.meters(it).convertTo(prefs.baseDistanceUnits)
            }
            sighting.time?.let {
                datetime = it.toZonedDateTime().toLocalDateTime()
            }
            location = sighting.location
            elevation = sightingElevation
            notes = sighting.notes
            harvested = sighting.harvested == true
            showOnMap = sighting.showOnMap
        }
    }

    fun save() {
        val sighting = Sighting(
            sightingId,
            pageId,
            datetime?.toZonedDateTime()?.toInstant(),
            location,
            elevation?.meters()?.value,
            harvested,
            notes,
            showOnMap
        )

        host.lifecycleScope.launch {
            Alerts.withLoading(host.requireContext(), host.getString(R.string.saving)) {
                repo.addSighting(sighting)
                onMain {
                    backCallback?.remove()
                    host.findNavController().navigateUp()
                }
            }
        }
    }

    fun onCoordinateBeaconSelected(beacon: Beacon) {
        beacon.elevation?.let { beaconElevation ->
            elevation = Distance.meters(beaconElevation).convertTo(prefs.baseDistanceUnits)
        }
    }

    private fun hasChanges(): Boolean {
        val sighting = initialSighting
        return if (sighting != null) {
            datetime?.toZonedDateTime()?.toInstant() != sighting.time ||
                location != sighting.location ||
                elevation?.meters()?.value != sighting.altitude ||
                notes != sighting.notes ||
                harvested != sighting.harvested ||
                showOnMap != sighting.showOnMap
        } else {
            datetime != initialDatetime ||
                location != null ||
                elevation != null ||
                !notes.isNullOrEmpty() ||
                harvested ||
                !showOnMap
        }
    }
}

@Composable
private fun CreateFieldGuideSightingScreen(
    datetime: LocalDateTime?,
    location: Coordinate?,
    elevation: Distance?,
    notes: String?,
    harvested: Boolean,
    showOnMap: Boolean,
    onDatetimeChange: (LocalDateTime?) -> Unit,
    onLocationChange: (Coordinate?) -> Unit,
    onElevationChange: (Distance?) -> Unit,
    onNotesChange: (String?) -> Unit,
    onHarvestedChange: (Boolean) -> Unit,
    onShowOnMapChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    locationAutofillRequest: Int,
    onAutoLocationClick: () -> Unit,
    onBeaconSelected: (Beacon) -> Unit,
    elevationAutofillRequest: Int,
    onAutoElevationClick: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val saveLabel = stringResource(R.string.save)
    val locationTitle = stringResource(R.string.location)
    val notesTitle = stringResource(R.string.tool_notes_title)
    val harvestedLabel = stringResource(R.string.harvested)
    val showOnMapLabel = stringResource(R.string.show_on_map)

    Box(modifier = modifier.testTag("field_guide_sighting_scroll")) {
        TrailSensePage(
            title = stringResource(R.string.record_sighting),
            onBack = onBack,
            backLabel = stringResource(R.string.back),
            modifier = Modifier.testTag("field_guide_sighting_title"),
            actions = {
                IconButton(
                    onClick = onSave,
                    modifier = Modifier.testTag("field_guide_sighting_save_btn")
                ) {
                    Icon(
                        painter = rememberVectorPainter(TrailIcons.Check),
                        contentDescription = saveLabel
                    )
                }
            }
        ) {
            trailSenseSection(key = "time") {
                row { position ->
                    DateTimeInput(
                        datetime = datetime,
                        onDatetimeChange = onDatetimeChange,
                        position = position
                    )
                }
            }

            trailSenseSection(key = "where", title = locationTitle) {
                row { position ->
                    CoordinateInput(
                        location = location,
                        onLocationChange = onLocationChange,
                        autoFillRequest = locationAutofillRequest,
                        onAutoLocationClick = onAutoLocationClick,
                        onBeaconSelected = onBeaconSelected,
                        position = position
                    )
                }
                row { position ->
                    ElevationInput(
                        elevation = elevation,
                        onElevationChange = onElevationChange,
                        autoFillRequest = elevationAutofillRequest,
                        onAutoElevationClick = onAutoElevationClick,
                        position = position
                    )
                }
            }

            trailSenseSection(key = "options") {
                row { position ->
                    TrailSenseSwitchRow(
                        label = harvestedLabel,
                        checked = harvested,
                        onCheckedChange = onHarvestedChange,
                        position = position,
                        modifier = Modifier.testTag("harvested")
                    )
                }
                row { position ->
                    TrailSenseSwitchRow(
                        label = showOnMapLabel,
                        checked = showOnMap,
                        onCheckedChange = onShowOnMapChange,
                        position = position,
                        modifier = Modifier.testTag("show_on_map")
                    )
                }
            }

            trailSenseSection(key = "notes", title = notesTitle) {
                row { position ->
                    NotesInput(
                        notes = notes,
                        onNotesChange = onNotesChange,
                        position = position
                    )
                }
            }

            item(key = "save") {
                TrailSenseButton(
                    text = saveLabel,
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("field_guide_sighting_save_btn_primary")
                )
            }
        }
    }
}

@Composable
private fun DateTimeInput(
    datetime: LocalDateTime?,
    onDatetimeChange: (LocalDateTime?) -> Unit,
    position: TrailSenseGroupPosition,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formatter = remember { DependencyRegistry.get<FormatService>() }
    val prefs = remember { DependencyRegistry.get<UserPreferences>() }
    val selected = datetime ?: LocalDateTime.now()
    val text = formatter.formatDateTime(selected.toZonedDateTime(), relative = true)

    TrailSenseListItem(
        title = stringResource(R.string.time),
        subtitle = text,
        icon = rememberVectorPainter(TrailIcons.Date),
        position = position,
        onClick = {
            CustomUiUtils.pickDatetime(context, prefs.use24HourTime, selected) {
                onDatetimeChange(it)
            }
        },
        modifier = modifier.testTag("time")
    )
}

@Composable
private fun SightingAutofillButton(
    isLoading: Boolean,
    icon: Painter,
    contentDescription: String,
    buttonTag: String,
    loadingTag: String,
    onClick: () -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.size(TrailSenseSize.minimumTouchTarget),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(24.dp)
                    .testTag(loadingTag),
                strokeWidth = 2.dp
            )
        }
    } else {
        IconButton(
            onClick = onClick,
            modifier = Modifier.testTag(buttonTag)
        ) {
            Icon(
                painter = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun CoordinateInput(
    location: Coordinate?,
    onLocationChange: (Coordinate?) -> Unit,
    autoFillRequest: Int,
    onAutoLocationClick: () -> Unit,
    onBeaconSelected: (Beacon) -> Unit,
    position: TrailSenseGroupPosition,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formatter = remember { FormatService.getInstance(context) }
    val sensors = remember { SensorSubsystem.getInstance(context) }
    val gps = remember { SensorService(context).getGPS() }
    val scope = rememberCoroutineScope()
    var locationText by remember(location, formatter) {
        mutableStateOf(location?.let { formatter.formatLocation(it) }.orEmpty())
    }
    val parsedCoordinate = remember(locationText) {
        Coordinate.parse(locationText)
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
            locationText = formatter.formatLocation(updated)
            onLocationChange(updated)
            isLoadingLocation = false
        }
    }

    LaunchedEffect(location) {
        val formatted = location?.let { formatter.formatLocation(it) }.orEmpty()
        if (locationText != formatted) {
            locationText = formatted
        }
    }

    LaunchedEffect(autoFillRequest) {
        if (autoFillRequest > 0) {
            autofillFromGps(triggerLinkedAutofill = false)
        }
    }

    val isInvalid = locationText.isNotBlank() && parsedCoordinate == null
    FieldGuideTextField(
        value = locationText,
        onValueChange = {
            locationText = it
            onLocationChange(Coordinate.parse(it))
        },
        label = stringResource(R.string.location),
        position = position,
        modifier = modifier.testTag("location"),
        enabled = !isLoadingLocation,
        isError = isInvalid,
        supportingText = if (isInvalid) stringResource(R.string.coordinate_input_invalid_location) else null,
        leadingIcon = {
            SightingAutofillButton(
                isLoading = isLoadingLocation,
                icon = rememberVectorPainter(TrailIcons.Location),
                contentDescription = stringResource(R.string.gps),
                buttonTag = "gps_btn",
                loadingTag = "gps_loading",
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
                                locationText = formatter.formatLocation(beacon.coordinate)
                                onLocationChange(beacon.coordinate)
                                onBeaconSelected(beacon)
                            }
                        }
                    }
                }
            )
        },
        trailingIcon = {
            IconButton(
                onClick = { showHelp = true },
                modifier = Modifier.testTag("coordinate_input_help_btn")
            ) {
                Icon(
                    painter = rememberVectorPainter(TrailIcons.Help),
                    contentDescription = stringResource(R.string.location_input_help_title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )

    if (showHelp) {
        TrailSenseDialog(
            onDismissRequest = { showHelp = false },
            title = stringResource(R.string.location_input_help_title),
            confirmText = stringResource(android.R.string.ok)
        ) {
            Text(stringResource(R.string.location_input_help))
        }
    }
}

@Composable
private fun ElevationInput(
    elevation: Distance?,
    onElevationChange: (Distance?) -> Unit,
    autoFillRequest: Int,
    onAutoElevationClick: () -> Unit,
    position: TrailSenseGroupPosition,
    modifier: Modifier = Modifier
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
        mutableStateOf(formatElevationAmount(elevation, unit))
    }
    var isLoadingElevation by remember { mutableStateOf(false) }
    var isUnitOpen by remember { mutableStateOf(false) }

    fun publish(text: String = elevationText, newUnit: DistanceUnits = unit) {
        onElevationChange(parseElevation(text, newUnit))
    }

    fun autofillFromAltimeter(triggerLinkedAutofill: Boolean = true) {
        if (triggerLinkedAutofill) {
            onAutoElevationClick()
        }
        scope.launch {
            isLoadingElevation = true
            val updated = withContext(Dispatchers.IO) {
                sensors.getElevation(SensorSubsystem.SensorRefreshPolicy.Refresh)
            }.convertTo(unit)
            elevationText = formatElevationAmount(updated, unit)
            onElevationChange(updated)
            isLoadingElevation = false
        }
    }

    LaunchedEffect(elevation) {
        val current = parseElevation(elevationText, unit)
        if (current != elevation) {
            val newUnit = elevation?.units ?: unit
            unit = newUnit
            elevationText = formatElevationAmount(elevation, newUnit)
        }
    }

    LaunchedEffect(autoFillRequest) {
        if (autoFillRequest > 0) {
            autofillFromAltimeter(triggerLinkedAutofill = false)
        }
    }

    FieldGuideTextField(
        value = elevationText,
        onValueChange = {
            elevationText = it
            publish(text = it)
        },
        label = stringResource(R.string.elevation),
        position = position,
        modifier = modifier.testTag("elevation"),
        enabled = !isLoadingElevation,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        leadingIcon = {
            SightingAutofillButton(
                isLoading = isLoadingElevation,
                icon = rememberVectorPainter(TrailIcons.Altitude),
                contentDescription = stringResource(R.string.elevation),
                buttonTag = "elevation_gps_btn",
                loadingTag = "elevation_loading",
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
                                ) { beacons ->
                                    beacons.filterIsInstance<Beacon>().filter { it.elevation != null }
                                } ?: return@launch
                                val updated = Distance.meters(beacon.elevation!!).convertTo(unit)
                                elevationText = formatElevationAmount(updated, unit)
                                onElevationChange(updated)
                            }
                        }
                    }
                }
            )
        },
        trailingIcon = {
            Box {
                TextButton(
                    onClick = { isUnitOpen = true },
                    modifier = Modifier
                        .padding(end = TrailSenseSpacing.xs)
                        .testTag("elevation_unit")
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
                                val converted = parseElevation(elevationText, unit)?.convertTo(option)
                                unit = option
                                elevationText = formatElevationAmount(converted, option)
                                publish(text = elevationText, newUnit = option)
                            }
                        )
                    }
                }
            }
        }
    )
}

private fun parseElevation(
    text: String,
    unit: DistanceUnits
): Distance? {
    return text.toDoubleCompat()?.let {
        Distance.from(it.toFloat(), unit)
    }
}

private fun formatElevationAmount(
    elevation: Distance?,
    unit: DistanceUnits
): String {
    val converted = elevation?.convertTo(unit) ?: return ""
    return DecimalFormatter.format(
        converted.value,
        Units.getDecimalPlaces(converted.units),
        strict = false
    )
}

@Composable
private fun NotesInput(
    notes: String?,
    onNotesChange: (String?) -> Unit,
    position: TrailSenseGroupPosition,
    modifier: Modifier = Modifier
) {
    FieldGuideTextField(
        value = notes.orEmpty(),
        onValueChange = { onNotesChange(it.ifBlank { null }) },
        label = stringResource(R.string.tool_notes_title),
        position = position,
        modifier = modifier.testTag("notes"),
        singleLine = false,
        minLines = 6,
        maxLines = 8,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
    )
}

@Preview(showBackground = true)
@Composable
private fun CreateFieldGuideSightingScreenPreview() {
    TrailSenseTheme {
        CreateFieldGuideSightingScreen(
            datetime = LocalDateTime.now(),
            location = null,
            elevation = null,
            notes = "Found near the trail.",
            harvested = false,
            showOnMap = true,
            onDatetimeChange = {},
            onLocationChange = {},
            onElevationChange = {},
            onNotesChange = {},
            onHarvestedChange = {},
            onShowOnMapChange = {},
            onSave = {},
            locationAutofillRequest = 0,
            onAutoLocationClick = {},
            onBeaconSelected = {},
            elevationAutofillRequest = 0,
            onAutoElevationClick = {}
        )
    }
}
