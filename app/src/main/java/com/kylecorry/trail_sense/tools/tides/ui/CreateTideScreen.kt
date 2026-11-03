@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.tides.ui

import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.dialogs.Pickers
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
import com.kylecorry.trail_sense.shared.extensions.promptIfUnsavedChanges
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
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
        onSave = controller::saveTide
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
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = TrailSenseSize.maximumContentWidth)
        ) {
            CreateTideHeader(
                title = stringResource(R.string.tide_table),
                onHelp = onHelp
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .testTag("scroll_view")
                    .padding(horizontal = TrailSenseSpacing.screen)
                    .padding(top = TrailSenseSpacing.lg, bottom = TrailSenseSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xl)
            ) {
                // Details: name + frequency
                TrailSenseCard {
                    SectionLabel(
                        text = stringResource(R.string.tide_table),
                        icon = painterResource(R.drawable.ic_tide_table)
                    )
                    Spacer(modifier = Modifier.height(TrailSenseSpacing.lg))
                    TideTextInput(
                        text = name,
                        label = stringResource(R.string.name),
                        tag = "tide_name",
                        onTextChange = onNameChange
                    )
                    Spacer(modifier = Modifier.height(TrailSenseSpacing.xl))
                    Text(
                        text = stringResource(R.string.frequency),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(TrailSenseSpacing.xxs))
                    Text(
                        text = stringResource(R.string.tide_frequency_description),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(TrailSenseSpacing.md))
                    TideFrequencyInput(
                        isSemidiurnal = isSemidiurnal,
                        onFrequencyChange = onFrequencyChange
                    )
                }

                // Estimation method + location + interval
                TrailSenseCard {
                    SectionLabel(
                        text = stringResource(R.string.estimate_method),
                        icon = painterResource(R.drawable.ic_tide_table)
                    )
                    Spacer(modifier = Modifier.height(TrailSenseSpacing.lg))
                    EstimateTypeInput(
                        estimateType = estimateType,
                        estimateTypes = estimateTypes,
                        estimateTypeNames = estimateTypeNames,
                        estimateTypeDescriptions = estimateTypeDescriptions,
                        onEstimateTypeChange = onEstimateTypeChange
                    )
                    Spacer(modifier = Modifier.height(TrailSenseSpacing.lg))
                    TideLocationInput(
                        location = location,
                        isRequired = isLocationRequired,
                        onLocationChange = onLocationChange
                    )
                    if (isDurationVisible) {
                        Spacer(modifier = Modifier.height(TrailSenseSpacing.lg))
                        LunitidalIntervalInput(
                            duration = lunitidalInterval,
                            onDurationChange = onLunitidalIntervalChange
                        )
                    }
                }

                // Reference tides
                TrailSenseCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionLabel(
                            text = stringResource(R.string.tides),
                            icon = painterResource(R.drawable.ic_tide_table),
                            modifier = Modifier.weight(1f)
                        )
                        TonalIconButton(
                            icon = painterResource(R.drawable.ic_add),
                            contentDescription = stringResource(R.string.add),
                            onClick = onAddTideEntry,
                            modifier = Modifier.testTag("add_tide_entry")
                        )
                    }
                    Spacer(modifier = Modifier.height(TrailSenseSpacing.lg))
                    if (tideEntries.isEmpty()) {
                        Text(
                            text = stringResource(R.string.time_not_set),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tide_times"),
                            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
                        ) {
                            tideEntries.forEach { entry ->
                                TideEntryCard(
                                    entry = entry,
                                    isTideTableRequired = isTideTableRequired,
                                    formatService = formatService,
                                    onEntryChange = onTideEntryChange,
                                    onEntryDelete = onTideEntryDelete,
                                    onTimeClick = onTideTimeClick,
                                    onHeightClick = onTideHeightClick
                                )
                            }
                        }
                    }
                }

                TrailSenseButton(
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
}

@Composable
private fun CreateTideHeader(
    title: String,
    onHelp: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TrailSenseSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 18.dp)
                    .testTag("create_tide_title"),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = onHelp,
                modifier = Modifier
                    .size(TrailSenseSize.minimumTouchTarget)
                    .testTag("create_tide_help_btn")
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_help),
                    contentDescription = stringResource(R.string.help)
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(
    text: String,
    icon: Painter,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(TrailSenseSize.leadingIconContainer)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(TrailSenseSize.leadingIcon)
                )
            }
        }
        Spacer(modifier = Modifier.width(TrailSenseSpacing.md))
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun TonalIconButton(
    icon: Painter,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier
            .size(TrailSenseSize.minimumTouchTarget)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(TrailSenseSize.leadingIcon)
            )
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
    OutlinedTextField(
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
private fun TideFrequencyInput(
    isSemidiurnal: Boolean,
    onFrequencyChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val diurnal = stringResource(R.string.tide_diurnal)
    val semidiurnal = stringResource(R.string.tide_semidiurnal)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tide_frequency")
    ) {
        TideFrequencyButton(
            text = "1    $diurnal",
            selected = !isSemidiurnal,
            onClick = { onFrequencyChange(false) },
            modifier = Modifier
                .weight(1f)
                .testTag("tide_frequency_diurnal")
        )
        Spacer(modifier = Modifier.width(TrailSenseSpacing.sm))
        TideFrequencyButton(
            text = "2    $semidiurnal",
            selected = isSemidiurnal,
            onClick = { onFrequencyChange(true) },
            modifier = Modifier
                .weight(1f)
                .testTag("tide_frequency_semidiurnal")
        )
    }
}

@Composable
private fun TideFrequencyButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(
            text = text,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
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
                    painter = painterResource(R.drawable.ic_location),
                    contentDescription = stringResource(R.string.gps)
                )
            }
        }
        Spacer(modifier = Modifier.width(TrailSenseSpacing.sm))
        OutlinedTextField(
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
private fun EstimateTypeInput(
    estimateType: EstimateType,
    estimateTypes: List<EstimateType>,
    estimateTypeNames: Map<EstimateType, String>,
    estimateTypeDescriptions: Map<EstimateType, String>,
    onEstimateTypeChange: (EstimateType) -> Unit,
    modifier: Modifier = Modifier
) {
    val method = stringResource(R.string.estimate_method)
    var isOpen by remember { mutableStateOf(false) }
    val selected = estimateTypeNames[estimateType].orEmpty()

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { isOpen = true },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("estimate_algorithm_spinner")
        ) {
            Text(selected.ifBlank { method })
        }
        DropdownMenu(
            expanded = isOpen,
            onDismissRequest = { isOpen = false }
        ) {
            estimateTypes.forEach { type ->
                DropdownMenuItem(
                    text = {
                        Column {
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
                painter = painterResource(if (isNegative) R.drawable.ic_minus else R.drawable.ic_add),
                contentDescription = stringResource(
                    if (isNegative) R.string.add else R.string.subtract
                )
            )
        }
        OutlinedTextField(
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
        OutlinedTextField(
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
    isTideTableRequired: Boolean,
    formatService: FormatService,
    onEntryChange: (TideEntry) -> Unit,
    onEntryDelete: (TideEntry) -> Unit,
    onTimeClick: (TideEntry) -> Unit,
    onHeightClick: (TideEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val tideType =
        if (entry.isHigh) {
            stringResource(R.string.high_tide_letter)
        } else {
            stringResource(R.string.low_tide_letter)
        }

    val defaultTimeText = if (isTideTableRequired) {
        stringResource(R.string.field_required, stringResource(R.string.time_not_set))
    } else {
        stringResource(R.string.time_not_set)
    }

    val timeText = entry.time?.let {
        formatService.formatDateTime(
            it,
            false,
            abbreviateMonth = true
        )
    } ?: defaultTimeText

    val heightText = entry.height?.let {
        formatService.formatDistance(it, 2)
    } ?: stringResource(R.string.dash)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tide_entry_${entry.id}"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = TrailSenseSpacing.md,
                    vertical = TrailSenseSpacing.sm
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .size(TrailSenseSize.minimumTouchTarget)
                    .clickable { onEntryChange(entry.copy(isHigh = !entry.isHigh)) }
                    .testTag("tide_type")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = tideType,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(TrailSenseSpacing.md))
            Text(
                text = timeText,
                modifier = Modifier
                    .weight(2f)
                    .clickable { onTimeClick(entry) }
                    .padding(vertical = TrailSenseSpacing.sm)
                    .testTag("tide_time"),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.width(TrailSenseSpacing.sm))
            Text(
                text = heightText,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onHeightClick(entry) }
                    .padding(vertical = TrailSenseSpacing.sm)
                    .testTag("tide_height"),
                style = MaterialTheme.typography.bodyMedium
            )
            IconButton(
                modifier = Modifier
                    .size(TrailSenseSize.minimumTouchTarget)
                    .testTag("delete"),
                onClick = { onEntryDelete(entry) }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_cancel),
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
