@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.tides.ui

import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.shared.views.compose.UnsavedChangesBackHandler
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import com.kylecorry.sol.units.DistanceUnits


import com.kylecorry.trail_sense.shared.navigation.Route

import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
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
import androidx.compose.material3.Surface
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
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDialog

@Composable
fun CreateTideToolScreen(route: Route.CreateTide) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val navigator = LocalNavigator.current
    val effects = LocalScreenEffects.current
    val scope = rememberCoroutineScope()
    val prefs = remember(appContext) { UserPreferences(appContext) }
    val viewModel = viewModel {
        CreateTideViewModel(
            editTideId = route.editTideId,
            tideRepo = TideTableRepo.getInstance(appContext),
            units = prefs.baseDistanceUnits,
            includeHarmonic = isDebug()
        )
    }
    val formatService = remember(context) { FormatService.getInstance(context) }
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val estimateTypeNames = mapOf(
        EstimateType.Clock to stringResource(R.string.tide_clock),
        EstimateType.LunitidalIntervalAuto to stringResource(R.string.lunitidal_interval_auto),
        EstimateType.LunitidalIntervalManualLocal to stringResource(R.string.lunitidal_interval_local),
        EstimateType.LunitidalIntervalManualUTC to stringResource(R.string.lunitidal_interval_utc),
        EstimateType.TideModel to stringResource(R.string.tide_model_auto),
        EstimateType.Harmonic to stringResource(R.string.harmonic)
    )
    val estimateTypeDescriptions = mapOf(
        EstimateType.Clock to stringResource(R.string.tide_estimation_description_tide_clock),
        EstimateType.LunitidalIntervalAuto to stringResource(R.string.tide_estimation_description_lunitidal_interval_auto),
        EstimateType.LunitidalIntervalManualLocal to stringResource(R.string.tide_estimation_description_lunitidal_interval_local),
        EstimateType.LunitidalIntervalManualUTC to stringResource(R.string.tide_estimation_description_lunitidal_interval_utc),
        EstimateType.TideModel to stringResource(R.string.tide_estimation_description_tide_model),
        EstimateType.Harmonic to "!! NEED TO MANUALLY INSERT INTO DB !!"
    )

    UnsavedChangesBackHandler(hasChanges = viewModel.hasChanges)
    LaunchedEffect(viewModel) {
        viewModel.saved.collect { navigator.navigateUp() }
    }

    CreateTideScreen(
        name = viewModel.name,
        location = viewModel.location,
        isSemidiurnal = viewModel.isSemidiurnal,
        estimateType = viewModel.estimateType,
        estimateTypes = viewModel.estimateTypes,
        estimateTypeNames = estimateTypeNames,
        estimateTypeDescriptions = estimateTypeDescriptions,
        lunitidalInterval = viewModel.lunitidalInterval,
        tideEntries = viewModel.tideEntries,
        canSave = viewModel.formIsValid(),
        isLocationRequired = viewModel.estimateType == EstimateType.LunitidalIntervalManualLocal,
        isDurationVisible = viewModel.isLunitidalIntervalManual(),
        isTideTableRequired = viewModel.isTideTableRequired(),
        formatService = formatService,
        onNameChange = { viewModel.name = it },
        onLocationChange = { viewModel.location = it },
        onFrequencyChange = { viewModel.isSemidiurnal = it },
        onEstimateTypeChange = { viewModel.estimateType = it },
        onLunitidalIntervalChange = { viewModel.lunitidalInterval = it },
        onAddTideEntry = viewModel::addTideEntry,
        onTideEntryChange = viewModel::updateTideEntry,
        onTideEntryDelete = { entry ->
            viewModel.deleteTideEntry(entry)
            scope.launch {
                val undo = effects.showSnackbar(
                    context.getString(R.string.tide_deleted),
                    actionLabel = context.getString(R.string.undo)
                )
                if (undo) {
                    viewModel.restoreTideEntry(entry)
                }
            }
        },
        onTideTimeClick = { entry ->
            CustomUiUtils.pickDatetime(
                context,
                prefs.use24HourTime,
                entry.time?.toLocalDateTime() ?: LocalDateTime.now()
            ) {
                if (it != null) {
                    viewModel.updateTideEntry(entry.copy(time = it.toZonedDateTime()))
                }
            }
        },
        onTideHeightClick = { entry ->
            val distanceUnits = formatService.sortDistanceUnits(DistanceUtils.humanDistanceUnits)
            CustomUiUtils.pickDistance(
                context,
                distanceUnits,
                entry.height,
                context.getString(R.string.height)
            ) { distance, cancelled ->
                if (!cancelled) {
                    viewModel.updateTideEntry(entry.copy(height = distance))
                }
            }
        },
        onHelp = {
            UserGuideUtils.showGuide(
                context,
                navigator,
                R.raw.guide_tool_tides
            )
        },
        onSave = viewModel::saveTide,
        onBack = { dispatcher?.onBackPressed() }
    )
}

/**
 * The tide table form's state: plain Kotlin over the tide table repository. It loads the table being edited, builds
 * the table from the form (null while the form is incomplete), and reports [saved] once the table is stored.
 */
internal class CreateTideViewModel(
    editTideId: Long,
    private val tideRepo: TideTableRepo,
    private val units: DistanceUnits,
    includeHarmonic: Boolean
) : ViewModel() {

    private var editingId: Long = editTideId
    private var editingTide by mutableStateOf<TideTable?>(null)

    private var nextTideEntryId = 1L

    var name by mutableStateOf("")
    var location by mutableStateOf<Coordinate?>(null)
    var isSemidiurnal by mutableStateOf(true)
    var estimateType by mutableStateOf(EstimateType.Clock)
    var lunitidalInterval by mutableStateOf<Duration?>(null)
    var tideEntries by mutableStateOf<List<TideEntry>>(emptyList())

    val estimateTypes = listOfNotNull(
        EstimateType.Clock,
        EstimateType.LunitidalIntervalAuto,
        EstimateType.LunitidalIntervalManualLocal,
        EstimateType.LunitidalIntervalManualUTC,
        EstimateType.TideModel,
        if (includeHarmonic) EstimateType.Harmonic else null
    )

    private val _saved = Channel<Unit>(Channel.CONFLATED)
    /** One event per save, after the table is stored. */
    val saved: Flow<Unit> = _saved.receiveAsFlow()

    init {
        if (editingId != 0L) {
            loadEditingTide()
        } else {
            tideEntries = listOf(newTideEntry())
        }
    }

    private fun loadEditingTide() {
        viewModelScope.launch {
            val tide = withContext(Dispatchers.IO) {
                tideRepo.getTideTable(editingId)
            }
            editingTide = tide

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
    }

    /** Puts back an entry removed with [deleteTideEntry] (the snackbar's Undo). */
    fun restoreTideEntry(entry: TideEntry) {
        tideEntries = tideEntries + entry
    }

    fun saveTide() {
        val tide = getTide() ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                tideRepo.addTideTable(tide)
            }
            _saved.send(Unit)
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

    val hasChanges: Boolean
        get() = TideTableIsDirtySpecification(editingTide).isSatisfiedBy(getTide())

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

    internal fun getTide(): TideTable? {
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
}

internal data class TideEntry(
    val id: Long,
    val isHigh: Boolean,
    val time: ZonedDateTime?,
    val height: Distance?
)

internal enum class EstimateType {
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
    com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField(
        value = text,
        onValueChange = onTextChange,
        modifier = modifier.testTag(tag),
        label = label,
        keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
        inCard = true
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
        val isInvalidLocation = locationText.isNotBlank() && parsedCoordinate == null
        com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField(
            value = locationText,
            onValueChange = {
                locationText = it
                onLocationChange(Coordinate.parse(it))
            },
            modifier = Modifier
                .weight(1f)
                .testTag("utm"),
            label = if (isRequired) {
                stringResource(R.string.field_required, stringResource(R.string.location))
            } else {
                stringResource(R.string.location)
            },
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
            isError = isInvalidLocation,
            errorText = stringResource(R.string.coordinate_input_invalid_location),
            enabled = !isLoadingLocation,
            inCard = true
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
            onDismissRequest = { isOpen = false }
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
        com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField(
            value = hoursText,
            onValueChange = {
                hoursText = it.filter(Char::isDigit).take(2)
                publish(hours = hoursText)
            },
            modifier = Modifier
                .weight(1f)
                .testTag("duration_hours"),
            label = stringResource(R.string.hours),
            keyboardType = KeyboardType.Number,
            inCard = true
        )
        Spacer(modifier = Modifier.width(TrailSenseSpacing.sm))
        com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField(
            value = minutesText,
            onValueChange = {
                minutesText = it.filter(Char::isDigit).take(2)
                publish(minutes = minutesText)
            },
            modifier = Modifier
                .weight(1f)
                .testTag("duration_minutes"),
            label = stringResource(R.string.minutes),
            keyboardType = KeyboardType.Number,
            inCard = true
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
