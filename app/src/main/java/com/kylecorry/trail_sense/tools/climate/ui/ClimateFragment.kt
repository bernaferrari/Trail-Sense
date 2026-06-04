package com.kylecorry.trail_sense.tools.climate.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kylecorry.andromeda.alerts.dialog
import com.kylecorry.andromeda.views.chart.Chart
import com.kylecorry.sol.math.MathExtensions.roundPlaces
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.meteorology.KoppenGeigerClimateGroup
import com.kylecorry.sol.science.meteorology.KoppenGeigerSeasonalPrecipitationPattern
import com.kylecorry.sol.science.meteorology.KoppenGeigerTemperaturePattern
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.views.CoordinateInputView
import com.kylecorry.trail_sense.shared.views.ElevationInputView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.climate.domain.BiologicalActivityType
import com.kylecorry.trail_sense.tools.climate.domain.PhenologyService
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Month

class ClimateFragment : Fragment() {

    private val locationSubsystem by lazy { LocationSubsystem.getInstance(requireContext()) }
    private val weatherSubsystem by lazy { WeatherSubsystem.getInstance(requireContext()) }
    private val formatter by lazy { FormatService.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }

    private val temperatureUnits: TemperatureUnits
        get() = prefs.temperatureUnits

    private val distanceUnits: DistanceUnits
        get() = prefs.baseDistanceUnits

    private val precipitationDistanceUnits: DistanceUnits
        get() = if (distanceUnits.isMetric) {
            DistanceUnits.Millimeters
        } else {
            DistanceUnits.Inches
        }

    private var date by mutableStateOf(LocalDate.now())
    private var location by mutableStateOf<Coordinate?>(null)
    private var elevation by mutableStateOf<Distance?>(null)
    private var temperatures by mutableStateOf<List<Pair<LocalDate, Range<Temperature>>>?>(null)
    private var precipitation by mutableStateOf<Map<Month, Distance>?>(null)
    private var climateClassification by mutableStateOf<Pair<String, String>?>(null)
    private var activityPatterns by mutableStateOf<Map<BiologicalActivityType, List<Range<LocalDate>>>?>(null)

    private var locationInputView: CoordinateInputView? = null
    private var elevationInputView: ElevationInputView? = null
    private var loadJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    ClimateScreen(
                        title = stringResource(R.string.tool_climate),
                        subtitle = stringResource(R.string.historic_temperature_years, 30),
                        date = date,
                        location = location,
                        elevation = this@ClimateFragment.elevation,
                        temperatureTitle = getTemperatureTitle(),
                        precipitationTitle = getPrecipitationTitle(),
                        climateZoneTitle = climateClassification?.first.orEmpty(),
                        climateZoneDescription = climateClassification?.second.orEmpty(),
                        ecologyDescription = getEcologySummary(),
                        showEcology = getActiveActivityEntries().isNotEmpty(),
                        temperatures = temperatures,
                        precipitation = precipitation,
                        temperatureUnits = temperatureUnits,
                        precipitationDistanceUnits = precipitationDistanceUnits,
                        onDateChange = ::onDateSelected,
                        onTemperatureDateSelected = ::onDateSelected,
                        onPrecipitationMonthSelected = ::setMonthFromChart,
                        onLocationChange = ::setLocationFromInput,
                        onElevationChange = ::setElevationFromInput,
                        onLocationCreated = {
                            locationInputView = it
                            bindCoordinateAndElevationViews()
                        },
                        onElevationCreated = {
                            elevationInputView = it
                            bindCoordinateAndElevationViews()
                        },
                        onEcologyClick = ::showEcologyDetails
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        location = locationSubsystem.location
        elevation = roundedElevation(locationSubsystem.elevation.convertTo(distanceUnits))
        loadClimateData()
    }

    override fun onPause() {
        locationInputView?.pause()
        elevationInputView?.pause()
        super.onPause()
    }

    override fun onDestroyView() {
        loadJob?.cancel()
        locationInputView = null
        elevationInputView = null
        super.onDestroyView()
    }

    private fun onDateSelected(newDate: LocalDate) {
        val shouldReload = date.year != newDate.year
        date = newDate
        if (shouldReload) {
            loadClimateData()
        }
    }

    private fun setMonthFromChart(month: Month) {
        onDateSelected(LocalDate.now().withDayOfMonth(15).withMonth(month.value))
    }

    private fun setLocationFromInput(newLocation: Coordinate?) {
        location = newLocation ?: locationSubsystem.location
        loadClimateData()
    }

    private fun setElevationFromInput(newElevation: Distance?) {
        elevation = newElevation ?: locationSubsystem.elevation
        loadClimateData()
    }

    private fun roundedElevation(value: Distance): Distance {
        return Distance.from(
            value.value.roundPlaces(Units.getDecimalPlaces(value.units)),
            value.units
        )
    }

    private fun loadClimateData() {
        val currentYear = date.year
        val currentLocation = location
        val currentElevation = elevation

        loadJob?.cancel()
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            val result = coroutineScope {
                val temperatures = async {
                    weatherSubsystem.getTemperatureRanges(
                        currentYear,
                        currentLocation,
                        currentElevation,
                        calibrated = false
                    )
                }
                val precipitation = async {
                    weatherSubsystem.getMonthlyPrecipitation(currentLocation)
                }
                val activity = async {
                    PhenologyService(weatherSubsystem).getYearlyActiveDays(
                        currentYear,
                        currentLocation,
                        currentElevation,
                        calibrated = false
                    )
                }
                Triple(temperatures.await(), precipitation.await(), activity.await())
            }

            temperatures = result.first
            precipitation = result.second
            activityPatterns = result.third
            climateClassification = getClimateDescription(result.first, result.second)
        }
    }

    private fun bindCoordinateAndElevationViews() {
        val coordinate = locationInputView ?: return
        val elevation = elevationInputView ?: return

        coordinate.setOnAutoLocationClickListener {
            if (elevation.elevation == null) {
                elevation.autofill()
            }
        }

        elevation.setOnAutoElevationClickListener {
            if (coordinate.coordinate == null) {
                coordinate.autofill()
            }
        }

        coordinate.setOnBeaconSelectedListener {
            it.elevation?.let { beaconElevation ->
                elevation.elevation = Distance.meters(beaconElevation).convertTo(distanceUnits)
            }
        }
    }

    private fun getTemperatureTitle(): String {
        val range = temperatures?.firstOrNull { it.first == date }?.second
            ?: return getString(R.string.temperature)
        val lowValue = formatter.formatTemperature(
            range.start.convertTo(temperatureUnits)
        )
        val highValue = formatter.formatTemperature(
            range.end.convertTo(temperatureUnits)
        )
        return getString(
            R.string.climate_temperature_amount,
            getString(R.string.slash_separated_pair, highValue, lowValue)
        )
    }

    private fun getPrecipitationTitle(): String {
        val value = precipitation?.get(date.month)?.convertTo(precipitationDistanceUnits)
            ?: return getString(R.string.weather_precipitation)
        val formattedValue = formatter.formatDistance(
            value,
            if (precipitationDistanceUnits.isMetric) 0 else 1
        )
        return getString(R.string.climate_precipitation_amount, formattedValue)
    }

    private fun getActiveActivityEntries(): List<Map.Entry<BiologicalActivityType, List<Range<LocalDate>>>> {
        return activityPatterns?.entries
            ?.filter { it.value.isNotEmpty() }
            ?.sortedBy { getBiologicalActivityName(it.key) }
            ?: emptyList()
    }

    private fun getEcologySummary(): String {
        return getActiveActivityEntries().joinToString("\n") {
            "${getBiologicalActivityName(it.key)}: ${formatActivity(formatter, it.value)}"
        }
    }

    private fun showEcologyDetails() {
        val fullText = getActiveActivityEntries().joinToString("\n\n") {
            "${getBiologicalActivityName(it.key)}\n${
                formatActivity(
                    formatter,
                    it.value,
                    true
                )
            }"
        }

        if (fullText.isNotBlank()) {
            dialog(getString(R.string.ecology), fullText, cancelText = null)
        }
    }

    private fun isActivityWrapped(activePeriods: List<Range<LocalDate>>): Boolean {
        if (activePeriods.size != 2) {
            return false
        }

        val first = activePeriods[0]
        val second = activePeriods[1]

        return first.start.dayOfYear == 1 &&
            second.end.month == Month.DECEMBER &&
            second.end.dayOfMonth == 31
    }

    private fun getBiologicalActivityName(activity: BiologicalActivityType): String {
        return when (activity) {
            BiologicalActivityType.FliesAndMosquitoes -> getString(R.string.biting_flies_mosquitoes)
            BiologicalActivityType.Ticks -> getString(R.string.ticks)
            else -> ""
        }
    }

    private fun formatActivity(
        formatter: FormatService,
        activity: List<Range<LocalDate>>,
        showTimeOfMonth: Boolean = false
    ): String {
        val start = if (isActivityWrapped(activity)) {
            activity[1].start
        } else {
            activity[0].start
        }

        val end = if (isActivityWrapped(activity)) {
            activity[0].end
        } else {
            activity.last().end
        }

        if (start.month == end.month) {
            return getString(R.string.active_in_month, formatter.formatMonth(start.month))
        }

        if (start.month == Month.JANUARY &&
            start.dayOfMonth < 10 &&
            end.month == Month.DECEMBER &&
            end.dayOfMonth > 20
        ) {
            return getString(R.string.active_all_year)
        }

        return getString(
            R.string.active_period,
            if (showTimeOfMonth) {
                formatTimeOfMonth(formatter, start)
            } else {
                formatter.formatMonth(start.month)
            },
            if (showTimeOfMonth) {
                formatTimeOfMonth(formatter, end)
            } else {
                formatter.formatMonth(end.month)
            }
        )
    }

    private fun formatTimeOfMonth(formatter: FormatService, date: LocalDate): String {
        val month = formatter.formatMonth(date.month)
        return when {
            date.dayOfMonth < 10 -> getString(R.string.early_month, month)
            date.dayOfMonth < 20 -> getString(R.string.mid_month, month)
            else -> getString(R.string.late_month, month)
        }
    }

    private fun getClimateDescription(
        temperatures: List<Pair<LocalDate, Range<Temperature>>>,
        precipitation: Map<Month, Distance>
    ): Pair<String, String> {
        val monthlyAverageTemperatures = temperatures
            .filter { it.first.dayOfMonth == 15 }
            .map {
                it.first.month to Temperature.celsius(
                    (it.second.start.celsius().value + it.second.end.celsius().value) / 2
                )
            }
            .toMap()

        val climate = Meteorology.getKoppenGeigerClimateClassification(
            monthlyAverageTemperatures,
            precipitation
        )

        val sentences = mutableListOf<String>()
        sentences.add(
            when (climate.climateGroup) {
                KoppenGeigerClimateGroup.Tropical -> getString(R.string.climate_tropical_description)
                KoppenGeigerClimateGroup.Dry -> getString(R.string.climate_dry_description)
                KoppenGeigerClimateGroup.Temperate -> getString(R.string.climate_temperate_description)
                KoppenGeigerClimateGroup.Continental -> getString(R.string.climate_continental_description)
                KoppenGeigerClimateGroup.Polar -> getString(R.string.climate_polar_description)
            }
        )
        sentences.add(
            when (climate.seasonalPrecipitationPattern) {
                KoppenGeigerSeasonalPrecipitationPattern.Rainforest -> getString(R.string.climate_rainforest_description)
                KoppenGeigerSeasonalPrecipitationPattern.Monsoon -> getString(R.string.climate_monsoon_description)
                KoppenGeigerSeasonalPrecipitationPattern.Savanna -> getString(R.string.climate_savanna_description)
                KoppenGeigerSeasonalPrecipitationPattern.Desert -> getString(R.string.climate_desert_description)
                KoppenGeigerSeasonalPrecipitationPattern.Steppe -> getString(R.string.climate_steppe_description)
                KoppenGeigerSeasonalPrecipitationPattern.DrySummer -> getString(R.string.climate_dry_summer_description)
                KoppenGeigerSeasonalPrecipitationPattern.DryWinter -> getString(R.string.climate_dry_winter_description)
                KoppenGeigerSeasonalPrecipitationPattern.NoDrySeason -> getString(R.string.climate_no_dry_season_description)
                KoppenGeigerSeasonalPrecipitationPattern.Tundra -> getString(R.string.climate_tundra_description)
                KoppenGeigerSeasonalPrecipitationPattern.IceCap -> getString(R.string.climate_ice_cap_description)
                null -> ""
            }
        )
        sentences.add(
            when (climate.temperaturePattern) {
                KoppenGeigerTemperaturePattern.Hot -> getString(R.string.climate_hot_description)
                KoppenGeigerTemperaturePattern.Cold -> getString(R.string.climate_cold_description)
                KoppenGeigerTemperaturePattern.HotSummer -> getString(R.string.climate_hot_summer_description)
                KoppenGeigerTemperaturePattern.WarmSummer -> getString(R.string.climate_warm_summer_description)
                KoppenGeigerTemperaturePattern.ColdSummer -> getString(R.string.climate_cold_summer_description)
                KoppenGeigerTemperaturePattern.VeryColdWinter -> getString(R.string.climate_very_cold_winter_description)
                null -> ""
            }
        )

        val name = when {
            climate.seasonalPrecipitationPattern == KoppenGeigerSeasonalPrecipitationPattern.Monsoon -> getString(
                R.string.climate_tropical_monsoon
            )

            climate.seasonalPrecipitationPattern == KoppenGeigerSeasonalPrecipitationPattern.Savanna -> getString(
                R.string.climate_tropical_savanna
            )

            climate.seasonalPrecipitationPattern == KoppenGeigerSeasonalPrecipitationPattern.Rainforest -> getString(
                R.string.climate_tropical_rainforest
            )

            climate.seasonalPrecipitationPattern == KoppenGeigerSeasonalPrecipitationPattern.Desert -> getString(
                R.string.climate_desert
            )

            climate.seasonalPrecipitationPattern == KoppenGeigerSeasonalPrecipitationPattern.Steppe -> getString(
                R.string.climate_steppe
            )

            climate.seasonalPrecipitationPattern == KoppenGeigerSeasonalPrecipitationPattern.Tundra -> getString(
                R.string.climate_tundra
            )

            climate.seasonalPrecipitationPattern == KoppenGeigerSeasonalPrecipitationPattern.IceCap -> getString(
                R.string.climate_ice_cap
            )

            climate.climateGroup == KoppenGeigerClimateGroup.Temperate -> getString(R.string.climate_temperate)
            climate.climateGroup == KoppenGeigerClimateGroup.Continental -> getString(R.string.climate_continental)
            else -> getString(R.string.unknown)
        }

        return name to sentences.filter { it.isNotEmpty() }.joinToString(" ")
    }
}

@Composable
private fun ClimateScreen(
    title: String,
    subtitle: String,
    date: LocalDate,
    location: Coordinate?,
    elevation: Distance?,
    temperatureTitle: String,
    precipitationTitle: String,
    climateZoneTitle: String,
    climateZoneDescription: String,
    ecologyDescription: String,
    showEcology: Boolean,
    temperatures: List<Pair<LocalDate, Range<Temperature>>>?,
    precipitation: Map<Month, Distance>?,
    temperatureUnits: TemperatureUnits,
    precipitationDistanceUnits: DistanceUnits,
    onDateChange: (LocalDate) -> Unit,
    onTemperatureDateSelected: (LocalDate) -> Unit,
    onPrecipitationMonthSelected: (Month) -> Unit,
    onLocationChange: (Coordinate?) -> Unit,
    onElevationChange: (Distance?) -> Unit,
    onLocationCreated: (CoordinateInputView) -> Unit,
    onElevationCreated: (ElevationInputView) -> Unit,
    onEcologyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ClimateHeader(title = title, subtitle = subtitle)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .testTag("climate_scroll")
                    .padding(horizontal = 16.dp)
                    .padding(bottom = dimensionResource(R.dimen.default_bottom_margin))
            ) {
                DatePickerInput(
                    date = date,
                    onDateChange = onDateChange
                )
                Spacer(modifier = Modifier.height(16.dp))
                CoordinateInput(
                    location = location,
                    onLocationChange = onLocationChange,
                    onLocationCreated = onLocationCreated
                )
                Spacer(modifier = Modifier.height(16.dp))
                ElevationInput(
                    elevation = elevation,
                    onElevationChange = onElevationChange,
                    onElevationCreated = onElevationCreated
                )
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.default_bottom_margin)))
                SectionTitle(text = temperatureTitle)
                SourceLabel(text = stringResource(R.string.source_merra_2))
                TemperatureChart(
                    temperatures = temperatures,
                    selectedDate = date,
                    units = temperatureUnits,
                    onDateSelected = onTemperatureDateSelected
                )
                Spacer(modifier = Modifier.height(16.dp))
                SectionTitle(text = precipitationTitle)
                SourceLabel(text = stringResource(R.string.source_era5))
                PrecipitationChart(
                    precipitation = precipitation,
                    selectedMonth = date.month,
                    units = precipitationDistanceUnits,
                    onMonthSelected = onPrecipitationMonthSelected
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (climateZoneTitle.isNotBlank()) {
                    SectionTitle(text = climateZoneTitle)
                    Text(
                        text = climateZoneDescription,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (showEcology) {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionTitle(text = stringResource(R.string.ecology))
                    Text(
                        text = ecologyDescription,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onEcologyClick),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ClimateHeader(
    title: String,
    subtitle: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .testTag("climate_title")
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun SourceLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DatePickerInput(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formatter = remember(context) { FormatService.getInstance(context) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onDateChange(date.minusDays(1)) },
            modifier = Modifier.testTag("prev_date")
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_keyboard_arrow_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(180f)
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = formatter.formatRelativeDate(date),
            modifier = Modifier.testTag("display_date"),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Box(
            modifier = Modifier
                .padding(start = 16.dp)
                .size(48.dp)
                .combinedClickable(
                    onClick = {
                        CustomUiUtils.pickDate(context, date, null) {
                            if (it != null) {
                                onDateChange(it)
                            }
                        }
                    },
                    onLongClick = { onDateChange(LocalDate.now()) }
                )
                .testTag("date_btn"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_date),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = { onDateChange(date.plusDays(1)) },
            modifier = Modifier.testTag("next_date")
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_keyboard_arrow_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CoordinateInput(
    location: Coordinate?,
    onLocationChange: (Coordinate?) -> Unit,
    onLocationCreated: (CoordinateInputView) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            CoordinateInputView(context).apply {
                id = R.id.location
                setOnCoordinateChangeListener(onLocationChange)
                onLocationCreated(this)
            }
        },
        update = { input ->
            if (input.coordinate != location) {
                input.coordinate = location
            }
        }
    )
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
private fun TemperatureChart(
    temperatures: List<Pair<LocalDate, Range<Temperature>>>?,
    selectedDate: LocalDate,
    units: TemperatureUnits,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.chart_height))
            .padding(top = 8.dp),
        factory = { context ->
            Chart(context).apply {
                id = R.id.temperature_chart
                tag = YearlyTemperatureRangeChart(this, onDateSelected)
            }
        },
        update = { chart ->
            val temperatureChart = chart.tag as? YearlyTemperatureRangeChart ?: return@AndroidView
            temperatures?.let {
                temperatureChart.plot(it, units)
                temperatureChart.highlight(selectedDate)
            }
        }
    )
}

@Composable
private fun PrecipitationChart(
    precipitation: Map<Month, Distance>?,
    selectedMonth: Month,
    units: DistanceUnits,
    onMonthSelected: (Month) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.chart_height))
            .padding(top = 8.dp),
        factory = { context ->
            Chart(context).apply {
                id = R.id.precipitation_chart
                tag = MonthlyPrecipitationChart(this, onMonthSelected)
            }
        },
        update = { chart ->
            val precipitationChart = chart.tag as? MonthlyPrecipitationChart ?: return@AndroidView
            precipitation?.let {
                precipitationChart.plot(it, units)
                precipitationChart.highlight(selectedMonth)
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun ClimateScreenPreview() {
    TrailSenseTheme {
        ClimateScreen(
            title = "Climate",
            subtitle = "30-year historical normals",
            date = LocalDate.now(),
            location = null,
            elevation = null,
            temperatureTitle = "Temperature: 82 °F / 62 °F",
            precipitationTitle = "Precipitation: 3.8 in monthly total",
            climateZoneTitle = "Continental",
            climateZoneDescription = "Warm summers with no dry season.",
            ecologyDescription = "Ticks: Active from March to November",
            showEcology = true,
            temperatures = null,
            precipitation = null,
            temperatureUnits = TemperatureUnits.Fahrenheit,
            precipitationDistanceUnits = DistanceUnits.Inches,
            onDateChange = {},
            onTemperatureDateSelected = {},
            onPrecipitationMonthSelected = {},
            onLocationChange = {},
            onElevationChange = {},
            onLocationCreated = {},
            onElevationCreated = {},
            onEcologyClick = {}
        )
    }
}
