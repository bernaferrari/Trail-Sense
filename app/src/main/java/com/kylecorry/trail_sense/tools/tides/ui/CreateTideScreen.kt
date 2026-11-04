@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.tides.ui

import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesFilledField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.debugging.isDebug
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.shared.extensions.promptIfUnsavedChanges
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.beacons.infrastructure.BeaconPickers
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.ClosestBeaconSort
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.domain.TideTableIsDirtySpecification
import com.kylecorry.trail_sense.tools.tides.domain.waterlevel.TideEstimator
import com.kylecorry.trail_sense.tools.tides.infrastructure.persistence.TideTableRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZonedDateTime

@Composable
fun CreateTideToolScreen(host: TrailSenseDestinationHost) {
    val controller = remember(host) { CreateTideController(host) }
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    DisposableEffect(controller) {
        controller.onCreate()
        onDispose {
            controller.onDestroy()
        }
    }

    CreateTideScreen(
        name = controller.name,
        location = controller.location,
        isSemidiurnal = controller.isSemidiurnal,
        estimateType = controller.estimateType,
        estimateTypes = controller.estimateTypes,
        estimateTypeNames = controller.estimateTypeNameMap,
        estimateTypeDescriptions = controller.estimateTypeDescriptionMap,
        lunitidalInterval = controller.lunitidalInterval,
        tideEntries = controller.tideEntries,
        canSave = controller.formIsValid(),
        isLocationRequired = controller.estimateType == EstimateType.LunitidalIntervalManualLocal,
        isDurationVisible = controller.isLunitidalIntervalManual(),
        isTideTableRequired = controller.isTideTableRequired(),
        formatService = controller.formatService,
        onNameChange = { controller.name = it },
        onLocationChange = { controller.location = it },
        onFrequencyChange = { controller.isSemidiurnal = it },
        onEstimateTypeChange = { controller.estimateType = it },
        onLunitidalIntervalChange = { controller.lunitidalInterval = it },
        onAddTideEntry = controller::addTideEntry,
        onTideEntryChange = controller::updateTideEntry,
        onTideEntryDelete = controller::deleteTideEntry,
        onTideTimeClick = controller::pickTideTime,
        onTideHeightClick = controller::pickTideHeight,
        onHelp = controller::showHelp,
        onSave = controller::saveTide,
        onBack = { dispatcher?.onBackPressed() }
    )
}

private class CreateTideController(private val host: TrailSenseDestinationHost) {

    val formatService by lazy { FormatService.getInstance(host.requireContext()) }
    private val tideRepo by lazy { TideTableRepo.getInstance(host.requireContext()) }
    private val prefs by lazy { UserPreferences(host.requireContext()) }
    private val units by lazy { prefs.baseDistanceUnits }

    private var editingId: Long = 0
    private var editingTide: TideTable? = null

    private var backCallback: OnBackPressedCallback? = null
    private var nextTideEntryId = 1L
    private var isInitialized = false

    var name by mutableStateOf("")
    var location by mutableStateOf<Coordinate?>(null)
    var isSemidiurnal by mutableStateOf(true)
    var estimateType by mutableStateOf(EstimateType.Clock)
    var lunitidalInterval by mutableStateOf<Duration?>(null)
    var tideEntries by mutableStateOf<List<TideEntry>>(emptyList())

    val estimateTypeNameMap by lazy {
        mapOf(
            EstimateType.Clock to host.getString(R.string.tide_clock),
            EstimateType.LunitidalIntervalAuto to host.getString(R.string.lunitidal_interval_auto),
            EstimateType.LunitidalIntervalManualLocal to host.getString(R.string.lunitidal_interval_local),
            EstimateType.LunitidalIntervalManualUTC to host.getString(R.string.lunitidal_interval_utc),
            EstimateType.TideModel to host.getString(R.string.tide_model_auto),
            EstimateType.Harmonic to host.getString(R.string.harmonic)
        )
    }

    val estimateTypeDescriptionMap by lazy {
        mapOf(
            EstimateType.Clock to host.getString(R.string.tide_estimation_description_tide_clock),
            EstimateType.LunitidalIntervalAuto to host.getString(R.string.tide_estimation_description_lunitidal_interval_auto),
            EstimateType.LunitidalIntervalManualLocal to host.getString(R.string.tide_estimation_description_lunitidal_interval_local),
            EstimateType.LunitidalIntervalManualUTC to host.getString(R.string.tide_estimation_description_lunitidal_interval_utc),
            EstimateType.TideModel to host.getString(R.string.tide_estimation_description_tide_model),
            EstimateType.Harmonic to "!! NEED TO MANUALLY INSERT INTO DB !!"
        )
    }

    val estimateTypes by lazy {
        listOfNotNull(
            EstimateType.Clock,
            EstimateType.LunitidalIntervalAuto,
            EstimateType.LunitidalIntervalManualLocal,
            EstimateType.LunitidalIntervalManualUTC,
            EstimateType.TideModel,
            if (isDebug()) EstimateType.Harmonic else null
        )
    }

    fun onCreate() {
        if (isInitialized) {
            return
        }
        isInitialized = true
        editingId = host.destinationArguments?.getLong("edit_tide_id") ?: 0L
        backCallback = host.promptIfUnsavedChanges(this::hasChanges)

        if (editingId != 0L) {
            loadEditingTide()
        } else if (tideEntries.isEmpty()) {
            tideEntries = listOf(newTideEntry())
        }
    }

    fun onDestroy() {
        backCallback?.remove()
        backCallback = null
    }

    private fun loadEditingTide() {
        host.lifecycleScope.launch {
            editingTide = withContext(Dispatchers.IO) {
                tideRepo.getTideTable(editingId)
            }

            val tide = editingTide
            if (tide != null) {
                fillExistingTideValues(tide)
            } else {
                editingId = 0L
                if (tideEntries.isEmpty()) {
                    tideEntries = listOf(newTideEntry())
                }
            }
        }
    }

    private fun fillExistingTideValues(tide: TideTable) {
        name = tide.name.orEmpty()
        location = tide.location
        isSemidiurnal = tide.isSemidiurnal
        tideEntries = tide.tides.map {
            val height = it.height
            TideEntry(
                nextTideEntryId++,
                it.isHigh,
                it.time,
                if (height != null) Distance.meters(height).convertTo(units) else null
            )
        }

        estimateType = when {
            tide.estimator == TideEstimator.Clock -> EstimateType.Clock
            tide.estimator == TideEstimator.LunitidalInterval -> {
                if (tide.lunitidalInterval == null) {
                    EstimateType.LunitidalIntervalAuto
                } else if (tide.lunitidalIntervalIsUtc) {
                    EstimateType.LunitidalIntervalManualUTC
                } else {
                    EstimateType.LunitidalIntervalManualLocal
                }
            }

            tide.estimator == TideEstimator.Harmonic -> EstimateType.Harmonic
            tide.estimator == TideEstimator.TideModel -> EstimateType.TideModel
            else -> EstimateType.Clock
        }
        lunitidalInterval = tide.lunitidalInterval
    }

    fun addTideEntry() {
        tideEntries = tideEntries + newTideEntry()
    }

    private fun newTideEntry(): TideEntry {
        return TideEntry(nextTideEntryId++, true, null, null)
    }

    fun updateTideEntry(entry: TideEntry) {
        tideEntries = tideEntries.map {
            if (it.id == entry.id) entry else it
        }
    }

    fun deleteTideEntry(entry: TideEntry) {
        tideEntries = tideEntries.filterNot { it.id == entry.id }
        CustomUiUtils.snackbar(
            host,
            host.getString(R.string.tide_deleted),
            action = host.getString(R.string.undo)
        ) {
            tideEntries = tideEntries + entry
        }
    }

    fun pickTideTime(entry: TideEntry) {
        CustomUiUtils.pickDatetime(
            host.requireContext(),
            prefs.use24HourTime,
            entry.time?.toLocalDateTime() ?: LocalDateTime.now()
        ) {
            if (it != null) {
                updateTideEntry(entry.copy(time = it.toZonedDateTime()))
            }
        }
    }

    fun pickTideHeight(entry: TideEntry) {
        val distanceUnits = formatService.sortDistanceUnits(DistanceUtils.humanDistanceUnits)
        CustomUiUtils.pickDistance(
            host.requireContext(),
            distanceUnits,
            entry.height,
            host.getString(R.string.height)
        ) { distance, cancelled ->
            if (!cancelled) {
                updateTideEntry(entry.copy(height = distance))
            }
        }
    }

    fun showHelp() {
        UserGuideUtils.showGuide(
            host.requireContext(),
            host.findNavController(),
            R.raw.guide_tool_tides
        )
    }

    fun saveTide() {
        val tide = getTide() ?: return
        host.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                tideRepo.addTideTable(tide)
            }

            backCallback?.remove()
            host.findNavController().navigateUp()
        }
    }

    fun isTideTableRequired(): Boolean {
        return estimateType == EstimateType.Clock || estimateType == EstimateType.LunitidalIntervalAuto
    }

    fun isLunitidalIntervalManual(): Boolean {
        return estimateType == EstimateType.LunitidalIntervalManualLocal ||
            estimateType == EstimateType.LunitidalIntervalManualUTC
    }

    fun formIsValid(): Boolean {
        return getTide() != null
    }

    private fun getEstimateAlgorithm(): TideEstimator {
        return when (estimateType) {
            EstimateType.Clock -> TideEstimator.Clock
            EstimateType.LunitidalIntervalAuto -> TideEstimator.LunitidalInterval
            EstimateType.LunitidalIntervalManualLocal -> TideEstimator.LunitidalInterval
            EstimateType.LunitidalIntervalManualUTC -> TideEstimator.LunitidalInterval
            EstimateType.Harmonic -> TideEstimator.Harmonic
            EstimateType.TideModel -> TideEstimator.TideModel
        }
    }

    private fun getTide(): TideTable? {
        val tides = tideEntries.mapNotNull {
            val time = it.time ?: return@mapNotNull null
            Tide(
                time,
                it.isHigh,
                it.height?.meters()?.value
            )
        }

        val estimateAlgorithm = getEstimateAlgorithm()

        if (editingId != 0L && editingTide == null) {
            return null
        }

        val interval =
            if (estimateType == EstimateType.LunitidalIntervalManualLocal ||
                estimateType == EstimateType.LunitidalIntervalManualUTC
            ) {
                lunitidalInterval
            } else {
                null
            }

        val lunitidalIntervalIsUtc = estimateType == EstimateType.LunitidalIntervalManualUTC

        if (
            (tides.isEmpty() && estimateAlgorithm == TideEstimator.Clock) ||
            (estimateType == EstimateType.LunitidalIntervalManualUTC && interval == null) ||
            (estimateType == EstimateType.LunitidalIntervalManualLocal && (interval == null || location == null)) ||
            (tides.isEmpty() && estimateType == EstimateType.LunitidalIntervalAuto)
        ) {
            return null
        }

        val tideName = if (name.isBlank()) null else name

        return TideTable(
            editingId,
            tides,
            tideName,
            location,
            isSemidiurnal = isSemidiurnal,
            isVisible = editingTide?.isVisible ?: true,
            estimator = estimateAlgorithm,
            lunitidalInterval = interval,
            lunitidalIntervalIsUtc = lunitidalIntervalIsUtc
        )
    }

    private fun hasChanges(): Boolean {
        val specification = TideTableIsDirtySpecification(editingTide)
        return specification.isSatisfiedBy(getTide())
    }

}

private data class TideEntry(
    val id: Long,
    val isHigh: Boolean,
    val time: ZonedDateTime?,
    val height: Distance?
)

private enum class EstimateType {
    Clock,
    LunitidalIntervalAuto,
    LunitidalIntervalManualLocal,
    LunitidalIntervalManualUTC,
    Harmonic,
    TideModel
}

/**
 * A tide table: its name and frequency, then how its tides are estimated (method, location, and the lunitidal interval
 * when that method needs one), then the reference tides as grouped rows, each a high/low toggle, its time and height,
 * with an "add" row closing the group. Help lives in the top bar; Save is the full-width button at the bottom.
 */
@Composable
private fun CreateTideScreen(
    name: String,
    location: Coordinate?,
    isSemidiurnal: Boolean,
    estimateType: EstimateType,
    estimateTypes: List<EstimateType>,
    estimateTypeNames: Map<EstimateType, String>,
    estimateTypeDescriptions: Map<EstimateType, String>,
    lunitidalInterval: Duration?,
    tideEntries: List<TideEntry>,
    canSave: Boolean,
    isLocationRequired: Boolean,
    isDurationVisible: Boolean,
    isTideTableRequired: Boolean,
    formatService: FormatService,
    onNameChange: (String) -> Unit,
    onLocationChange: (Coordinate?) -> Unit,
    onFrequencyChange: (Boolean) -> Unit,
    onEstimateTypeChange: (EstimateType) -> Unit,
    onLunitidalIntervalChange: (Duration?) -> Unit,
    onAddTideEntry: () -> Unit,
    onTideEntryChange: (TideEntry) -> Unit,
    onTideEntryDelete: (TideEntry) -> Unit,
    onTideTimeClick: (TideEntry) -> Unit,
    onTideHeightClick: (TideEntry) -> Unit,
    onHelp: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val estimateTitle = stringResource(R.string.estimate_method)
    Box(modifier = modifier.testTag("create_tide_title")) {
        TrailSensePage(
            title = stringResource(R.string.tide_table),
            onBack = onBack,
            backLabel = stringResource(R.string.back),
            modifier = Modifier.testTag("scroll_view"),
            actions = {
                IconButton(onClick = onHelp, modifier = Modifier.testTag("create_tide_help_btn")) {
                    Icon(
                        painter = rememberVectorPainter(TrailIcons.Help),
                        contentDescription = stringResource(R.string.help)
                    )
                }
            }
        ) {
            trailSenseSection(key = "details") {
                row { position ->
                    TrailSenseGroupCard(position = position) {
                        TideTextInput(
                            text = name,
                            label = stringResource(R.string.name),
                            tag = "tide_name",
                            onTextChange = onNameChange
                        )
                    }
                }
                row { position ->
                    TrailSenseGroupCard(position = position) {
                        Text(
                            text = stringResource(R.string.frequency),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.tide_frequency_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(TrailSenseSpacing.md))
                        TidesConnectedChoice(
                            options = listOf(
                                TidesChoice(false, stringResource(R.string.tide_diurnal), "tide_frequency_diurnal"),
                                TidesChoice(
                                    true,
                                    stringResource(R.string.tide_semidiurnal),
                                    "tide_frequency_semidiurnal"
                                )
                            ),
                            selected = isSemidiurnal,
                            onSelect = onFrequencyChange,
                            modifier = Modifier.testTag("tide_frequency")
                        )
                    }
                }
            }

            trailSenseSection(key = "estimate", title = estimateTitle) {
                row { position ->
                    EstimateTypeInput(
                        estimateType = estimateType,
                        estimateTypes = estimateTypes,
                        estimateTypeNames = estimateTypeNames,
                        estimateTypeDescriptions = estimateTypeDescriptions,
                        position = position,
                        onEstimateTypeChange = onEstimateTypeChange
                    )
                }
                row { position ->
                    TrailSenseGroupCard(position = position) {
                        TideLocationInput(
                            location = location,
                            isRequired = isLocationRequired,
                            onLocationChange = onLocationChange
                        )
                    }
                }
                if (isDurationVisible) {
                    row { position ->
                        TrailSenseGroupCard(position = position) {
                            Text(
                                text = stringResource(R.string.lunitidal_interval),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(TrailSenseSpacing.sm))
                            LunitidalIntervalInput(
                                duration = lunitidalInterval,
                                onDurationChange = onLunitidalIntervalChange
                            )
                        }
                    }
                }
            }

            item(key = "tides") {
                TrailSenseRowGroup(
                    modifier = Modifier
                        .animateContentSize(MaterialTheme.motionScheme.fastSpatialSpec())
                        .testTag("tide_times"),
                    title = stringResource(R.string.tides)
                ) {
                    tideEntries.forEach { entry ->
                        row { position ->
                            TideEntryCard(
                                entry = entry,
                                position = position,
                                isTideTableRequired = isTideTableRequired,
                                formatService = formatService,
                                onEntryChange = onTideEntryChange,
                                onEntryDelete = onTideEntryDelete,
                                onTimeClick = onTideTimeClick,
                                onHeightClick = onTideHeightClick
                            )
                        }
                    }
                    row { position ->
                        TidesGroupRow(
                            position = position,
                            modifier = Modifier.testTag("add_tide_entry"),
                            onClick = onAddTideEntry,
                            leading = { com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowWell(rememberVectorPainter(TrailIcons.Add)) }
                        ) {
                            Text(
                                text = stringResource(R.string.add),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item(key = "save") {
                com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton(
                    text = stringResource(R.string.save),
                    onClick = onSave,
                    enabled = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_tide_save_btn")
                )
            }
        }
    }
}

@Composable
private fun TideTextInput(
    text: String,
    label: String,
    tag: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    PlacesFilledField(
        value = text,
        onValueChange = onTextChange,
        modifier = modifier
            .fillMaxWidth()
            .testTag(tag),
        label = { Text(label) },
        singleLine = true
    )
}

@Composable
private fun TideLocationInput(
    location: Coordinate?,
    isRequired: Boolean,
    onLocationChange: (Coordinate?) -> Unit,
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

    LaunchedEffect(location) {
        val formatted = location?.let { formatter.formatLocation(it) }.orEmpty()
        if (locationText != formatted) {
            locationText = formatted
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
                            0 -> scope.launch {
                                isLoadingLocation = true
                                val updated = withContext(Dispatchers.IO) {
                                    sensors.getLocation(SensorSubsystem.SensorRefreshPolicy.Refresh)
                                }
                                locationText = formatter.formatLocation(updated)
                                onLocationChange(updated)
                                isLoadingLocation = false
                            }

                            1 -> scope.launch {
                                val beacon = BeaconPickers.pickBeacon(
                                    context,
                                    sort = ClosestBeaconSort(BeaconService(context), gps::location)
                                ) ?: return@launch
                                locationText = formatter.formatLocation(beacon.coordinate)
                                onLocationChange(beacon.coordinate)
                            }
                        }
                    }
                },
                modifier = Modifier
                    .size(TrailSenseSize.minimumTouchTarget)
                    .testTag("gps_btn")
            ) {
                Icon(
                    painter = rememberVectorPainter(TrailIcons.Location),
                    contentDescription = stringResource(R.string.gps)
                )
            }
        }
        Spacer(modifier = Modifier.width(TrailSenseSpacing.sm))
        PlacesFilledField(
            value = locationText,
            onValueChange = {
                locationText = it
                onLocationChange(Coordinate.parse(it))
            },
            modifier = Modifier
                .weight(1f)
                .testTag("utm"),
            label = {
                Text(
                    if (isRequired) {
                        stringResource(R.string.field_required, stringResource(R.string.location))
                    } else {
                        stringResource(R.string.location)
                    }
                )
            },
            isError = locationText.isNotBlank() && parsedCoordinate == null,
            supportingText = {
                if (locationText.isNotBlank() && parsedCoordinate == null) {
                    Text(stringResource(R.string.coordinate_input_invalid_location))
                }
            },
            singleLine = true,
            enabled = !isLoadingLocation
        )
        Spacer(modifier = Modifier.width(TrailSenseSpacing.sm))
        IconButton(
            onClick = { showHelp = true },
            modifier = Modifier
                .size(TrailSenseSize.minimumTouchTarget)
                .testTag("coordinate_input_help_btn")
        ) {
            Icon(
                painter = rememberVectorPainter(TrailIcons.Help),
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
private fun EstimateTypeInput(
    estimateType: EstimateType,
    estimateTypes: List<EstimateType>,
    estimateTypeNames: Map<EstimateType, String>,
    estimateTypeDescriptions: Map<EstimateType, String>,
    position: TrailSenseGroupPosition,
    onEstimateTypeChange: (EstimateType) -> Unit,
    modifier: Modifier = Modifier
) {
    val method = stringResource(R.string.estimate_method)
    var isOpen by remember { mutableStateOf(false) }
    val selected = estimateTypeNames[estimateType].orEmpty()
    val description = estimateTypeDescriptions[estimateType]

    Box(modifier = modifier.fillMaxWidth()) {
        TidesGroupRow(
            position = position,
            modifier = Modifier.testTag("estimate_algorithm_spinner"),
            onClick = { isOpen = true },
            trailing = {
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = TrailSenseSpacing.md)
                )
            }
        ) {
            Text(
                text = selected.ifBlank { method },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        DropdownMenu(
            expanded = isOpen,
            onDismissRequest = { isOpen = false },
            shape = RoundedCornerShape(TrailSenseShape.iconContainer),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            estimateTypes.forEach { type ->
                DropdownMenuItem(
                    text = {
                        Column(modifier = Modifier.padding(vertical = TrailSenseSpacing.xs)) {
                            Text(estimateTypeNames[type].orEmpty())
                            Text(
                                text = estimateTypeDescriptions[type].orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        isOpen = false
                        onEstimateTypeChange(type)
                    }
                )
            }
        }
    }
}

@Composable
private fun LunitidalIntervalInput(
    duration: Duration?,
    onDurationChange: (Duration?) -> Unit,
    modifier: Modifier = Modifier
) {
    val absoluteDuration = duration?.abs()
    var isNegative by remember {
        mutableStateOf(duration?.isNegative == true)
    }
    var hoursText by remember {
        mutableStateOf(formatDurationHours(absoluteDuration))
    }
    var minutesText by remember {
        mutableStateOf(formatDurationMinutes(absoluteDuration))
    }

    fun publish(
        hours: String = hoursText,
        minutes: String = minutesText,
        negative: Boolean = isNegative
    ) {
        onDurationChange(parseLunitidalInterval(hours, minutes, negative))
    }

    LaunchedEffect(duration) {
        val parsed = parseLunitidalInterval(hoursText, minutesText, isNegative)
        if (parsed != duration) {
            isNegative = duration?.isNegative == true
            hoursText = formatDurationHours(absoluteDuration)
            minutesText = formatDurationMinutes(absoluteDuration)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("lunitidal_interval_duration"),
        verticalAlignment = Alignment.Top
    ) {
        IconButton(
            onClick = {
                isNegative = !isNegative
                publish(negative = isNegative)
            },
            modifier = Modifier
                .size(TrailSenseSize.minimumTouchTarget)
                .testTag("negate_btn")
        ) {
            Icon(
                painter = rememberVectorPainter(if (isNegative) TrailIcons.Minus else TrailIcons.Add),
                contentDescription = stringResource(
                    if (isNegative) R.string.add else R.string.subtract
                )
            )
        }
        PlacesFilledField(
            value = hoursText,
            onValueChange = {
                hoursText = it.filter(Char::isDigit).take(2)
                publish(hours = hoursText)
            },
            modifier = Modifier
                .weight(1f)
                .testTag("duration_hours"),
            label = { Text(stringResource(R.string.hours)) },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )
        Spacer(modifier = Modifier.width(TrailSenseSpacing.sm))
        PlacesFilledField(
            value = minutesText,
            onValueChange = {
                minutesText = it.filter(Char::isDigit).take(2)
                publish(minutes = minutesText)
            },
            modifier = Modifier
                .weight(1f)
                .testTag("duration_minutes"),
            label = { Text(stringResource(R.string.minutes)) },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )
    }
}

private fun parseLunitidalInterval(
    hours: String,
    minutes: String,
    isNegative: Boolean
): Duration? {
    val parsedHours = hours.toLongOrNull()
    val parsedMinutes = minutes.toLongOrNull()
    if (parsedHours == null && parsedMinutes == null) {
        return null
    }

    val duration = Duration
        .ofHours((parsedHours ?: 0L).coerceIn(0L, 99L))
        .plusMinutes((parsedMinutes ?: 0L).coerceIn(0L, 99L))

    return if (isNegative) {
        duration.negated()
    } else {
        duration
    }
}

private fun formatDurationHours(duration: Duration?): String {
    return duration?.toHours()?.coerceIn(0L, 99L)?.toString() ?: ""
}

private fun formatDurationMinutes(duration: Duration?): String {
    return duration?.toMinutes()?.rem(60)?.coerceIn(0L, 99L)?.toString() ?: ""
}

@Composable
private fun TideEntryCard(
    entry: TideEntry,
    position: TrailSenseGroupPosition,
    isTideTableRequired: Boolean,
    formatService: FormatService,
    onEntryChange: (TideEntry) -> Unit,
    onEntryDelete: (TideEntry) -> Unit,
    onTimeClick: (TideEntry) -> Unit,
    onHeightClick: (TideEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val tideType = stringResource(if (entry.isHigh) R.string.high_tide_letter else R.string.low_tide_letter)
    val tideTypeName = stringResource(if (entry.isHigh) R.string.high_tide else R.string.low_tide)

    val defaultTimeText = if (isTideTableRequired) {
        stringResource(R.string.field_required, stringResource(R.string.time_not_set))
    } else {
        stringResource(R.string.time_not_set)
    }

    val timeText = entry.time?.let {
        formatService.formatDateTime(it, false, abbreviateMonth = true)
    } ?: defaultTimeText

    val heightText = entry.height?.let {
        formatService.formatDistance(it, 2)
    } ?: stringResource(R.string.dash)

    val badgeColor by animateColorAsState(
        targetValue = if (entry.isHigh) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.tertiaryContainer
        },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "tideTypeBadge"
    )
    val badgeContent = if (entry.isHigh) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }
    val valueShape = RoundedCornerShape(TrailSenseShape.groupInner * 2)

    TidesGroupRow(
        position = position,
        modifier = modifier.testTag("tide_entry_${entry.id}"),
        leading = {
            Surface(
                onClick = { onEntryChange(entry.copy(isHigh = !entry.isHigh)) },
                shape = RoundedCornerShape(TrailSenseShape.iconContainer),
                color = badgeColor,
                contentColor = badgeContent,
                modifier = Modifier
                    .size(TrailSenseSize.minimumTouchTarget)
                    .semantics { contentDescription = tideTypeName }
                    .testTag("tide_type")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = tideType,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        },
        trailing = {
            IconButton(
                modifier = Modifier.testTag("delete"),
                onClick = { onEntryDelete(entry) }
            ) {
                Icon(
                    painter = rememberVectorPainter(TrailIcons.Cancel),
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = timeText,
                modifier = Modifier
                    .weight(2f)
                    .clip(valueShape)
                    .clickable(role = Role.Button) { onTimeClick(entry) }
                    .heightIn(min = TrailSenseSize.minimumTouchTarget)
                    .wrapContentHeight(Alignment.CenterVertically)
                    .padding(horizontal = TrailSenseSpacing.xs)
                    .testTag("tide_time"),
                style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                color = if (entry.time == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = heightText,
                modifier = Modifier
                    .weight(1f)
                    .clip(valueShape)
                    .clickable(role = Role.Button) { onHeightClick(entry) }
                    .heightIn(min = TrailSenseSize.minimumTouchTarget)
                    .wrapContentHeight(Alignment.CenterVertically)
                    .padding(horizontal = TrailSenseSpacing.xs)
                    .testTag("tide_height"),
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CreateTideScreenPreview() {
    TrailSenseTheme {
        CreateTideScreen(
            name = "Harbor",
            location = null,
            isSemidiurnal = true,
            estimateType = EstimateType.Clock,
            estimateTypes = listOf(
                EstimateType.Clock,
                EstimateType.LunitidalIntervalAuto,
                EstimateType.TideModel
            ),
            estimateTypeNames = mapOf(
                EstimateType.Clock to "Tide clock",
                EstimateType.LunitidalIntervalAuto to "Lunitidal interval",
                EstimateType.TideModel to "Tide model"
            ),
            estimateTypeDescriptions = emptyMap(),
            lunitidalInterval = null,
            tideEntries = emptyList(),
            canSave = false,
            isLocationRequired = false,
            isDurationVisible = false,
            isTideTableRequired = true,
            formatService = FormatService.getInstance(androidx.compose.ui.platform.LocalContext.current),
            onNameChange = {},
            onLocationChange = {},
            onFrequencyChange = {},
            onEstimateTypeChange = {},
            onLunitidalIntervalChange = {},
            onAddTideEntry = {},
            onTideEntryChange = {},
            onTideEntryDelete = {},
            onTideTimeClick = {},
            onTideHeightClick = {},
            onHelp = {},
            onSave = {}
        )
    }
}
