package com.kylecorry.trail_sense.tools.field_guide.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.promptIfUnsavedChanges
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.shared.views.ElevationInputView
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

class CreateFieldGuideSightingFragment : Fragment() {

    private val repo by lazy { FieldGuideRepo.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }

    private var pageId: Long = 0L
    private var sightingId: Long = 0L
    private var initialSighting by mutableStateOf<Sighting?>(null)
    private var initialDatetime = LocalDateTime.now()

    private var datetime by mutableStateOf<LocalDateTime?>(initialDatetime)
    private var location by mutableStateOf<Coordinate?>(null)
    private var elevation by mutableStateOf<Distance?>(null)
    private var notes by mutableStateOf<String?>(null)
    private var harvested by mutableStateOf(false)
    private var showOnMap by mutableStateOf(true)

    private var elevationView: ElevationInputView? = null
    private var locationAutofillRequest by mutableStateOf(0)
    private var backCallback: OnBackPressedCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageId = arguments?.getLong("page_id") ?: 0L
        sightingId = arguments?.getLong("sighting_id") ?: 0L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    CreateFieldGuideSightingScreen(
                        datetime = datetime,
                        location = location,
                        elevation = this@CreateFieldGuideSightingFragment.elevation,
                        notes = notes,
                        harvested = harvested,
                        showOnMap = showOnMap,
                        onDatetimeChange = { datetime = it },
                        onLocationChange = { location = it },
                        onElevationChange = { this@CreateFieldGuideSightingFragment.elevation = it },
                        onNotesChange = { notes = it },
                        onHarvestedChange = { harvested = it },
                        onShowOnMapChange = { showOnMap = it },
                        onSave = ::save,
                        locationAutofillRequest = locationAutofillRequest,
                        onAutoLocationClick = {
                            if (this@CreateFieldGuideSightingFragment.elevation == null) {
                                elevationView?.autofill()
                            }
                        },
                        onBeaconSelected = ::onCoordinateBeaconSelected,
                        onElevationCreated = {
                            elevationView = it
                            bindCoordinateAndElevationViews()
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        backCallback = promptIfUnsavedChanges(this::hasChanges)
        if (sightingId != 0L) {
            loadSighting()
        }
    }

    override fun onPause() {
        elevationView?.pause()
        super.onPause()
    }

    override fun onDestroyView() {
        elevationView = null
        super.onDestroyView()
    }

    private fun loadSighting() {
        viewLifecycleOwner.lifecycleScope.launch {
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

    private fun save() {
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

        viewLifecycleOwner.lifecycleScope.launch {
            Alerts.withLoading(requireContext(), getString(R.string.saving)) {
                repo.addSighting(sighting)
                onMain {
                    backCallback?.remove()
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun bindCoordinateAndElevationViews() {
        val elevation = elevationView ?: return

        elevation.setOnAutoElevationClickListener {
            if (location == null) {
                locationAutofillRequest++
            }
        }
    }

    private fun onCoordinateBeaconSelected(beacon: Beacon) {
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
    onElevationCreated: (ElevationInputView) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CreateFieldGuideSightingHeader(
                title = stringResource(R.string.record_sighting),
                onSave = onSave
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .testTag("field_guide_sighting_scroll")
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 16.dp)
            ) {
                DateTimeInput(
                    datetime = datetime,
                    onDatetimeChange = onDatetimeChange
                )
                Spacer(modifier = Modifier.height(16.dp))
                CoordinateInput(
                    location = location,
                    onLocationChange = onLocationChange,
                    autoFillRequest = locationAutofillRequest,
                    onAutoLocationClick = onAutoLocationClick,
                    onBeaconSelected = onBeaconSelected
                )
                Spacer(modifier = Modifier.height(16.dp))
                ElevationInput(
                    elevation = elevation,
                    onElevationChange = onElevationChange,
                    onElevationCreated = onElevationCreated
                )
                Spacer(modifier = Modifier.height(16.dp))
                SightingSwitch(
                    tag = "harvested",
                    text = stringResource(R.string.harvested),
                    checked = harvested,
                    onCheckedChange = onHarvestedChange
                )
                Spacer(modifier = Modifier.height(16.dp))
                SightingSwitch(
                    tag = "show_on_map",
                    text = stringResource(R.string.show_on_map),
                    checked = showOnMap,
                    onCheckedChange = onShowOnMapChange
                )
                Spacer(modifier = Modifier.height(16.dp))
                NotesInput(
                    notes = notes,
                    onNotesChange = onNotesChange
                )
            }
        }
    }
}

@Composable
private fun CreateFieldGuideSightingHeader(
    title: String,
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
                    .testTag("field_guide_sighting_title"),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = onSave,
                modifier = Modifier.testTag("field_guide_sighting_save_btn")
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun DateTimeInput(
    datetime: LocalDateTime?,
    onDatetimeChange: (LocalDateTime?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formatter = remember { AppServiceRegistry.get<FormatService>() }
    val prefs = remember { AppServiceRegistry.get<UserPreferences>() }
    val selected = datetime ?: LocalDateTime.now()
    val text = formatter.formatDateTime(selected.toZonedDateTime(), relative = true)

    OutlinedTextField(
        value = text,
        onValueChange = {},
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                val supportFragmentManager =
                    (context as? AppCompatActivity)?.supportFragmentManager
                if (supportFragmentManager != null) {
                    CustomUiUtils.pickMaterialDatetime(
                        supportFragmentManager,
                        prefs.use24HourTime,
                        selected
                    ) {
                        onDatetimeChange(it)
                    }
                } else {
                    CustomUiUtils.pickDatetime(context, prefs.use24HourTime, selected) {
                        onDatetimeChange(it)
                    }
                }
            }
            .testTag("time"),
        readOnly = true,
        singleLine = true
    )
}

@Composable
private fun CoordinateInput(
    location: Coordinate?,
    onLocationChange: (Coordinate?) -> Unit,
    autoFillRequest: Int,
    onAutoLocationClick: () -> Unit,
    onBeaconSelected: (Beacon) -> Unit,
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

    fun autofillFromGps() {
        onAutoLocationClick()
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
            autofillFromGps()
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
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
                                locationText = formatter.formatLocation(beacon.coordinate)
                                onLocationChange(beacon.coordinate)
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
            value = locationText,
            onValueChange = {
                locationText = it
                onLocationChange(Coordinate.parse(it))
            },
            modifier = Modifier
                .weight(1f)
                .testTag("location"),
            label = { Text(stringResource(R.string.location)) },
            isError = locationText.isNotBlank() && parsedCoordinate == null,
            supportingText = {
                if (locationText.isNotBlank() && parsedCoordinate == null) {
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
    onElevationCreated: (ElevationInputView) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            ElevationInputView(context).apply {
                id = R.id.elevation
                setOnElevationChangeListener(onElevationChange)
                onElevationCreated(this)
            }
        },
        update = { input ->
            if (input.elevation != elevation) {
                input.elevation = elevation
            }
        }
    )
}

@Composable
private fun SightingSwitch(
    tag: String,
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
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
private fun NotesInput(
    notes: String?,
    onNotesChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val hint = stringResource(R.string.tool_notes_title)

    OutlinedTextField(
        value = notes.orEmpty(),
        onValueChange = { onNotesChange(it.ifBlank { null }) },
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .testTag("notes"),
        label = { Text(hint) },
        minLines = 6,
        maxLines = 8
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
            onElevationCreated = {}
        )
    }
}
