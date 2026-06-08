@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.climate.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kylecorry.andromeda.alerts.dialog
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.luna.text.toDoubleCompat
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
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.IBeacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.BeaconPickers
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.ClosestBeaconSort
import com.kylecorry.trail_sense.tools.climate.domain.BiologicalActivityType
import com.kylecorry.trail_sense.tools.climate.domain.PhenologyService
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.Month
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ClimateToolScreen(fragment: Fragment) {
    val controller = remember(fragment) { ClimateController(fragment) }

    DisposableEffect(controller) {
        controller.onCreate()
        onDispose {
            controller.onDestroy()
        }
    }

    controller.Content()
}

private class ClimateController(private val fragment: Fragment) {

    private val locationSubsystem by lazy { LocationSubsystem.getInstance(fragment.requireContext()) }
    private val weatherSubsystem by lazy { WeatherSubsystem.getInstance(fragment.requireContext()) }
    private val formatter by lazy { FormatService.getInstance(fragment.requireContext()) }
    private val prefs by lazy { UserPreferences(fragment.requireContext()) }

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
    private var locationAutofillRequest by mutableStateOf(0)
    private var elevationAutofillRequest by mutableStateOf(0)

    private var loadJob: Job? = null

    @Composable
    fun Content() {
        ClimateScreen(
            title = fragment.getString(R.string.tool_climate),
            subtitle = fragment.getString(R.string.historic_temperature_years, 30),
            date = date,
            location = location,
            elevation = this@ClimateController.elevation,
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
            locationAutofillRequest = locationAutofillRequest,
            onAutoLocationClick = {
                if (this@ClimateController.elevation == null) {
                    elevationAutofillRequest++
                }
            },
            onLocationBeaconSelected = {
                it.elevation?.let { beaconElevation ->
                    setElevationFromInput(
                        Distance.meters(beaconElevation).convertTo(distanceUnits)
                    )
                }
            },
            elevationAutofillRequest = elevationAutofillRequest,
            onAutoElevationClick = {
                if (this@ClimateController.location == null) {
                    locationAutofillRequest++
                }
            },
            onEcologyClick = ::showEcologyDetails
        )
    }

    fun onCreate() {
        location = locationSubsystem.location
        elevation = roundedElevation(locationSubsystem.elevation.convertTo(distanceUnits))
        loadClimateData()
    }

    fun onDestroy() {
        loadJob?.cancel()
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
        loadJob = fragment.viewLifecycleOwner.lifecycleScope.launch {
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
            fragment.dialog(getString(R.string.ecology), fullText, cancelText = null)
        }
    }

    private fun getString(@StringRes id: Int, vararg args: Any): String {
        return fragment.getString(id, *args)
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
    locationAutofillRequest: Int,
    onAutoLocationClick: () -> Unit,
    onLocationBeaconSelected: (Beacon) -> Unit,
    elevationAutofillRequest: Int,
    onAutoElevationClick: () -> Unit,
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
                    autoFillRequest = locationAutofillRequest,
                    onAutoLocationClick = onAutoLocationClick,
                    onBeaconSelected = onLocationBeaconSelected
                )
                Spacer(modifier = Modifier.height(16.dp))
                ElevationInput(
                    elevation = elevation,
                    onElevationChange = onElevationChange,
                    autoFillRequest = elevationAutofillRequest,
                    onAutoElevationClick = onAutoElevationClick
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
                .testTag("utm"),
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
    autoFillRequest: Int,
    onAutoElevationClick: () -> Unit,
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

    Row(
        modifier = modifier.fillMaxWidth(),
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
                                elevationText = formatElevationAmount(updated, unit)
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
                .testTag("elevation_input"),
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
}

private fun parseElevation(text: String, unit: DistanceUnits): Distance? {
    return text.toDoubleCompat()?.let {
        Distance.from(it.toFloat(), unit)
    }
}

private fun formatElevationAmount(elevation: Distance?, unit: DistanceUnits): String {
    val converted = elevation?.convertTo(unit) ?: return ""
    return DecimalFormatter.format(
        converted.value,
        Units.getDecimalPlaces(converted.units),
        strict = false
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
    val data = temperatures.orEmpty()
    val lowColor = Color(AppColor.Blue.color)
    val highColor = Color(AppColor.Red.color)
    val grid = MaterialTheme.colorScheme.outlineVariant
    val highlightColor = MaterialTheme.colorScheme.onSurface
    val freezingColor = Color(AppColor.Gray.color).copy(alpha = 0.2f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val year = data.firstOrNull()?.first?.year ?: selectedDate.year

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.chart_height))
            .padding(top = 8.dp)
            .testTag("temperature_chart")
            .pointerInput(data, year) {
                detectTapGestures { offset ->
                    if (data.isEmpty() || size.width == 0) {
                        return@detectTapGestures
                    }
                    val day = ((offset.x / size.width) * 365f)
                        .roundToInt()
                        .coerceIn(1, if (LocalDate.of(year, 12, 31).isLeapYear) 366 else 365)
                    onDateSelected(LocalDate.ofYearDay(year, day))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (data.isEmpty()) {
            Text(
                text = stringResource(R.string.no_data),
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
            return@Box
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val lows = data.map {
                it.first.dayOfYear.toFloat() to it.second.start.convertTo(units).value
            }
            val highs = data.map {
                it.first.dayOfYear.toFloat() to it.second.end.convertTo(units).value
            }
            val values = lows + highs
            val freezing = Temperature.celsius(0f).convertTo(units).value
            val minYValue = values.minOfOrNull { it.second } ?: freezing
            val maxYValue = values.maxOfOrNull { it.second } ?: freezing
            val yMinimum = min(minYValue - 5f, freezing)
            val yMaximum = max(maxYValue + 5f, yMinimum + 10f)
            val yRange = (yMaximum - yMinimum).takeIf { it > 0f } ?: 1f
            val maxDay = if (LocalDate.of(year, 12, 31).isLeapYear) 366f else 365f

            fun x(day: Float): Float = ((day - 1f) / (maxDay - 1f)).coerceIn(0f, 1f) * size.width
            fun y(value: Float): Float {
                return size.height - ((value - yMinimum) / yRange).coerceIn(0f, 1f) * size.height
            }

            repeat(5) { index ->
                val x = index / 4f * size.width
                drawLine(
                    color = grid,
                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                    end = androidx.compose.ui.geometry.Offset(x, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            repeat(5) { index ->
                val y = index / 4f * size.height
                drawLine(
                    color = grid,
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            drawRect(
                color = freezingColor,
                topLeft = androidx.compose.ui.geometry.Offset(0f, y(freezing)),
                size = androidx.compose.ui.geometry.Size(
                    size.width,
                    (size.height - y(freezing)).coerceAtLeast(0f)
                )
            )

            fun drawSeries(points: List<Pair<Float, Float>>, color: Color) {
                points.zipWithNext().forEach { (start, end) ->
                    drawLine(
                        color = color,
                        start = androidx.compose.ui.geometry.Offset(x(start.first), y(start.second)),
                        end = androidx.compose.ui.geometry.Offset(x(end.first), y(end.second)),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            drawSeries(lows, lowColor)
            drawSeries(highs, highColor)

            listOf(lows, highs).forEach { points ->
                val selected = points.firstOrNull { it.first.toInt() == selectedDate.dayOfYear }
                if (selected != null) {
                    drawCircle(
                        color = highlightColor,
                        radius = 4.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(
                            x(selected.first),
                            y(selected.second)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PrecipitationChart(
    precipitation: Map<Month, Distance>?,
    selectedMonth: Month,
    units: DistanceUnits,
    onMonthSelected: (Month) -> Unit,
    modifier: Modifier = Modifier
) {
    val data = precipitation.orEmpty()
    val barColor = Color(AppColor.Blue.color)
    val grid = MaterialTheme.colorScheme.outlineVariant
    val highlightColor = MaterialTheme.colorScheme.onSurface
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.chart_height))
            .padding(top = 8.dp)
            .testTag("precipitation_chart")
            .pointerInput(data) {
                detectTapGestures { offset ->
                    if (data.isEmpty() || size.width == 0) {
                        return@detectTapGestures
                    }
                    val month = ((offset.x / size.width) * 12f).toInt().coerceIn(0, 11) + 1
                    onMonthSelected(Month.of(month))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (data.isEmpty()) {
            Text(
                text = stringResource(R.string.no_data),
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
            return@Box
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val values = data.map {
                it.key.value.toFloat() to it.value.convertTo(units).value
            }.sortedBy { it.first }
            val margin = Distance.from(0.5f, DistanceUnits.Inches).convertTo(units).value
            val minRange = Distance.from(1f, DistanceUnits.Inches).convertTo(units).value
            val maxValue = values.maxOfOrNull { it.second } ?: 0f
            val yMaximum = max(maxValue + margin, minRange)

            fun x(month: Float): Float = (month / 13f).coerceIn(0f, 1f) * size.width
            fun y(value: Float): Float {
                return size.height - (value / yMaximum).coerceIn(0f, 1f) * size.height
            }

            repeat(5) { index ->
                val y = index / 4f * size.height
                drawLine(
                    color = grid,
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            values.forEach { (month, value) ->
                val centerX = x(month)
                val barWidth = size.width / 13f * 0.6f
                val top = y(value)
                drawRect(
                    color = barColor,
                    topLeft = androidx.compose.ui.geometry.Offset(centerX - barWidth / 2f, top),
                    size = androidx.compose.ui.geometry.Size(barWidth, size.height - top)
                )
            }

            values.firstOrNull { it.first.toInt() == selectedMonth.value }?.let { selected ->
                drawCircle(
                    color = highlightColor,
                    radius = 4.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(
                        x(selected.first),
                        y(selected.second)
                    )
                )
            }
        }
    }
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
            locationAutofillRequest = 0,
            onAutoLocationClick = {},
            onLocationBeaconSelected = {},
            elevationAutofillRequest = 0,
            onAutoElevationClick = {},
            onEcologyClick = {}
        )
    }
}
