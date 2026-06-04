package com.kylecorry.trail_sense.tools.tides.ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.system.Resources
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
import com.kylecorry.trail_sense.shared.views.CoordinateInputView
import com.kylecorry.trail_sense.shared.views.DurationInputView
import com.kylecorry.trail_sense.shared.views.MaterialSpinnerView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
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

class CreateTideFragment : Fragment() {

    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val tideRepo by lazy { TideTableRepo.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val units by lazy { prefs.baseDistanceUnits }

    private var editingId: Long = 0
    private var editingTide: TideTable? = null

    private var tideLocationView: CoordinateInputView? = null

    private var backCallback: OnBackPressedCallback? = null
    private var nextTideEntryId = 1L

    private var name by mutableStateOf("")
    private var location by mutableStateOf<Coordinate?>(null)
    private var isSemidiurnal by mutableStateOf(true)
    private var estimateType by mutableStateOf(EstimateType.Clock)
    private var lunitidalInterval by mutableStateOf<Duration?>(null)
    private var tideEntries by mutableStateOf<List<TideEntry>>(emptyList())
    private var tideEntryScrollRequest by mutableStateOf(0)

    private val estimateTypeNameMap by lazy {
        mapOf(
            EstimateType.Clock to getString(R.string.tide_clock),
            EstimateType.LunitidalIntervalAuto to getString(R.string.lunitidal_interval_auto),
            EstimateType.LunitidalIntervalManualLocal to getString(R.string.lunitidal_interval_local),
            EstimateType.LunitidalIntervalManualUTC to getString(R.string.lunitidal_interval_utc),
            EstimateType.TideModel to getString(R.string.tide_model_auto),
            EstimateType.Harmonic to getString(R.string.harmonic)
        )
    }

    private val estimateTypeDescriptionMap by lazy {
        mapOf(
            EstimateType.Clock to getString(R.string.tide_estimation_description_tide_clock),
            EstimateType.LunitidalIntervalAuto to getString(R.string.tide_estimation_description_lunitidal_interval_auto),
            EstimateType.LunitidalIntervalManualLocal to getString(R.string.tide_estimation_description_lunitidal_interval_local),
            EstimateType.LunitidalIntervalManualUTC to getString(R.string.tide_estimation_description_lunitidal_interval_utc),
            EstimateType.TideModel to getString(R.string.tide_estimation_description_tide_model),
            EstimateType.Harmonic to "!! NEED TO MANUALLY INSERT INTO DB !!"
        )
    }

    private val estimateTypes by lazy {
        listOfNotNull(
            EstimateType.Clock,
            EstimateType.LunitidalIntervalAuto,
            EstimateType.LunitidalIntervalManualLocal,
            EstimateType.LunitidalIntervalManualUTC,
            EstimateType.TideModel,
            if (isDebug()) EstimateType.Harmonic else null
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editingId = arguments?.getLong("edit_tide_id") ?: 0L
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
                    CreateTideScreen(
                        name = name,
                        location = location,
                        isSemidiurnal = isSemidiurnal,
                        estimateType = estimateType,
                        estimateTypes = estimateTypes,
                        estimateTypeNames = estimateTypeNameMap,
                        estimateTypeDescriptions = estimateTypeDescriptionMap,
                        lunitidalInterval = lunitidalInterval,
                        tideEntries = tideEntries,
                        tideEntryScrollRequest = tideEntryScrollRequest,
                        canSave = formIsValid(),
                        isLocationRequired = estimateType == EstimateType.LunitidalIntervalManualLocal,
                        isDurationVisible = isLunitidalIntervalManual(),
                        isTideTableRequired = isTideTableRequired(),
                        formatService = formatService,
                        onNameChange = { name = it },
                        onLocationChange = { location = it },
                        onFrequencyChange = { isSemidiurnal = it },
                        onEstimateTypeChange = { estimateType = it },
                        onLunitidalIntervalChange = { lunitidalInterval = it },
                        onAddTideEntry = ::addTideEntry,
                        onTideEntryChange = ::updateTideEntry,
                        onTideEntryDelete = ::deleteTideEntry,
                        onTideTimeClick = ::pickTideTime,
                        onTideHeightClick = ::pickTideHeight,
                        onHelp = {
                            UserGuideUtils.showGuide(
                                this@CreateTideFragment,
                                R.raw.guide_tool_tides
                            )
                        },
                        onSave = ::saveTide,
                        onLocationCreated = { tideLocationView = it }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        backCallback = promptIfUnsavedChanges(this::hasChanges)

        if (editingId != 0L) {
            loadEditingTide()
        } else if (tideEntries.isEmpty()) {
            tideEntries = listOf(newTideEntry())
        }
    }

    override fun onPause() {
        tideLocationView?.pause()
        super.onPause()
    }

    override fun onDestroyView() {
        tideLocationView = null
        super.onDestroyView()
    }

    private fun loadEditingTide() {
        viewLifecycleOwner.lifecycleScope.launch {
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

    private fun addTideEntry() {
        tideEntries = tideEntries + newTideEntry()
        tideEntryScrollRequest++
    }

    private fun newTideEntry(): TideEntry {
        return TideEntry(nextTideEntryId++, true, null, null)
    }

    private fun updateTideEntry(entry: TideEntry) {
        tideEntries = tideEntries.map {
            if (it.id == entry.id) entry else it
        }
    }

    private fun deleteTideEntry(entry: TideEntry) {
        tideEntries = tideEntries.filterNot { it.id == entry.id }
        CustomUiUtils.snackbar(
            this,
            getString(R.string.tide_deleted),
            action = getString(R.string.undo)
        ) {
            tideEntries = tideEntries + entry
        }
    }

    private fun pickTideTime(entry: TideEntry) {
        CustomUiUtils.pickDatetime(
            requireContext(),
            prefs.use24HourTime,
            entry.time?.toLocalDateTime() ?: LocalDateTime.now()
        ) {
            if (it != null) {
                updateTideEntry(entry.copy(time = it.toZonedDateTime()))
            }
        }
    }

    private fun pickTideHeight(entry: TideEntry) {
        val distanceUnits = formatService.sortDistanceUnits(DistanceUtils.humanDistanceUnits)
        CustomUiUtils.pickDistance(
            requireContext(),
            distanceUnits,
            entry.height,
            getString(R.string.height)
        ) { distance, cancelled ->
            if (!cancelled) {
                updateTideEntry(entry.copy(height = distance))
            }
        }
    }

    private fun saveTide() {
        val tide = getTide() ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                tideRepo.addTideTable(tide)
            }

            backCallback?.remove()
            findNavController().navigateUp()
        }
    }

    private fun isTideTableRequired(): Boolean {
        return estimateType == EstimateType.Clock || estimateType == EstimateType.LunitidalIntervalAuto
    }

    private fun isLunitidalIntervalManual(): Boolean {
        return estimateType == EstimateType.LunitidalIntervalManualLocal ||
            estimateType == EstimateType.LunitidalIntervalManualUTC
    }

    private fun formIsValid(): Boolean {
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
    tideEntryScrollRequest: Int,
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
    onLocationCreated: (CoordinateInputView) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CreateTideHeader(
                title = stringResource(R.string.tide_table),
                canSave = canSave,
                onHelp = onHelp,
                onSave = onSave
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .testTag("scroll_view")
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 96.dp)
            ) {
                TideTextInput(
                    text = name,
                    label = stringResource(R.string.name),
                    tag = "tide_name",
                    onTextChange = onNameChange
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.frequency),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.tide_frequency_description),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                TideFrequencyInput(
                    isSemidiurnal = isSemidiurnal,
                    onFrequencyChange = onFrequencyChange
                )
                Spacer(modifier = Modifier.height(16.dp))
                TideLocationInput(
                    location = location,
                    isRequired = isLocationRequired,
                    onLocationChange = onLocationChange,
                    onLocationCreated = onLocationCreated
                )
                Spacer(modifier = Modifier.height(16.dp))
                EstimateTypeInput(
                    estimateType = estimateType,
                    estimateTypes = estimateTypes,
                    estimateTypeNames = estimateTypeNames,
                    estimateTypeDescriptions = estimateTypeDescriptions,
                    onEstimateTypeChange = onEstimateTypeChange
                )
                if (isDurationVisible) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LunitidalIntervalInput(
                        duration = lunitidalInterval,
                        onDurationChange = onLunitidalIntervalChange
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                TideEntryHeader(onAddTideEntry = onAddTideEntry)
                TideEntriesList(
                    entries = tideEntries,
                    scrollRequest = tideEntryScrollRequest,
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

@Composable
private fun CreateTideHeader(
    title: String,
    canSave: Boolean,
    onHelp: () -> Unit,
    onSave: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onHelp,
                modifier = Modifier.testTag("create_tide_help_btn")
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_help),
                    contentDescription = null
                )
            }
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 18.dp)
                    .testTag("create_tide_title"),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (canSave) {
                IconButton(
                    onClick = onSave,
                    modifier = Modifier.testTag("create_tide_save_btn")
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
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
        Spacer(modifier = Modifier.width(8.dp))
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
    onLocationCreated: (CoordinateInputView) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            CoordinateInputView(context).apply {
                id = R.id.tide_location
                required = isRequired
                setOnCoordinateChangeListener {
                    onLocationChange(it)
                }
                onLocationCreated(this)
            }
        },
        update = { input ->
            input.required = isRequired
            if (input.coordinate != location) {
                input.coordinate = location
            }
        }
    )
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

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            MaterialSpinnerView(context, null).apply {
                id = R.id.estimate_algorithm_spinner
                setHint(method)
                setItems(estimateTypes.map { estimateTypeNames[it].orEmpty() })
                setDescriptions(estimateTypes.map { estimateTypeDescriptions[it].orEmpty() })
                setOnItemSelectedListener { position ->
                    onEstimateTypeChange(estimateTypes.getOrElse(position ?: 0) { EstimateType.Clock })
                }
            }
        },
        update = { spinner ->
            val index = estimateTypes.indexOf(estimateType).coerceAtLeast(0)
            spinner.setHint(method)
            spinner.setItems(estimateTypes.map { estimateTypeNames[it].orEmpty() })
            spinner.setDescriptions(estimateTypes.map { estimateTypeDescriptions[it].orEmpty() })
            if (spinner.selectedItemPosition != index) {
                spinner.setSelection(index)
            }
        }
    )
}

@Composable
private fun LunitidalIntervalInput(
    duration: Duration?,
    onDurationChange: (Duration?) -> Unit,
    modifier: Modifier = Modifier
) {
    val hint = stringResource(R.string.lunitidal_interval)

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            DurationInputView(context, null).apply {
                id = R.id.lunitidal_interval_duration
                showSeconds = false
                allowNegative = true
                this.hint = hint
                setOnDurationChangeListener(onDurationChange)
            }
        },
        update = { input ->
            input.showSeconds = false
            input.allowNegative = true
            input.hint = hint
            if (input.duration != duration) {
                input.updateDuration(duration)
            }
        }
    )
}

@Composable
private fun TideEntryHeader(
    onAddTideEntry: () -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            val padding = Resources.dp(context, 16f).toInt()
            val margin = Resources.dp(context, 8f).toInt()
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                addView(
                    ImageButton(context).apply {
                        id = R.id.add_tide_entry
                        setImageResource(R.drawable.ic_add)
                        background = null
                        setOnClickListener { onAddTideEntry() }
                        layoutParams = LinearLayout.LayoutParams(
                            Resources.dp(context, 48f).toInt(),
                            Resources.dp(context, 48f).toInt()
                        )
                    }
                )
                addView(
                    TextView(context).apply {
                        id = R.id.tide_time
                        text = context.getString(R.string.time)
                        setPadding(padding)
                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            2f
                        )
                    }
                )
                addView(
                    TextView(context).apply {
                        id = R.id.tide_height
                        text = context.getString(R.string.height)
                        setPadding(padding)
                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        ).apply {
                            marginStart = margin
                        }
                    }
                )
                addView(
                    View(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            Resources.dp(context, 24f).toInt(),
                            Resources.dp(context, 24f).toInt()
                        ).apply {
                            marginStart = margin
                        }
                    }
                )
            }
        }
    )
}

@Composable
private fun TideEntriesList(
    entries: List<TideEntry>,
    scrollRequest: Int,
    isTideTableRequired: Boolean,
    formatService: FormatService,
    onEntryChange: (TideEntry) -> Unit,
    onEntryDelete: (TideEntry) -> Unit,
    onTimeClick: (TideEntry) -> Unit,
    onHeightClick: (TideEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(scrollRequest) {
        if (scrollRequest > 0 && entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.lastIndex)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .testTag("tide_times"),
        state = listState
    ) {
        items(entries, key = { it.id }) { entry ->
            TideEntryRow(
                entry = entry,
                isTideTableRequired = isTideTableRequired,
                formatService = formatService,
                onEntryChange = onEntryChange,
                onEntryDelete = onEntryDelete,
                onTimeClick = onTimeClick,
                onHeightClick = onHeightClick
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun TideEntryRow(
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("tide_entry_${entry.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clickable { onEntryChange(entry.copy(isHigh = !entry.isHigh)) }
                .testTag("tide_type"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = tideType,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
        Text(
            text = timeText,
            modifier = Modifier
                .weight(2f)
                .clickable { onTimeClick(entry) }
                .padding(16.dp)
                .testTag("tide_time"),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = heightText,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
                .clickable { onHeightClick(entry) }
                .padding(16.dp)
                .testTag("tide_height"),
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            modifier = Modifier
                .size(24.dp)
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
            tideEntryScrollRequest = 0,
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
            onSave = {},
            onLocationCreated = {}
        )
    }
}
