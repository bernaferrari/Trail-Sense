@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.climate.ui

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.luna.text.toDoubleCompat
import com.kylecorry.sol.math.MathExtensions.roundPlaces
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.meteorology.KoppenGeigerClimateGroup
import com.kylecorry.sol.science.meteorology.KoppenGeigerSeasonalPrecipitationPattern
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButtonStyle
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDialog
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseOptionsDialog
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSheet
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGroupShape
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.IBeacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.BeaconPickers
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.ClosestBeaconSort
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.Month
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@Composable
fun ClimateToolScreen(host: TrailSenseDestinationHost) {
    val controller = remember(host) { ClimateController(host) }

    DisposableEffect(controller) {
        controller.onCreate()
        onDispose {
            controller.onDestroy()
        }
    }

    controller.Content()
}

/** One month's normals: the average daily low and high (°C) and the month's total precipitation. */
private data class ClimateMonth(
    val month: Month,
    val lowCelsius: Float,
    val highCelsius: Float,
    val precipitation: Distance?
)

private class ClimateController(private val host: TrailSenseDestinationHost) {

    private val locationSubsystem by lazy { LocationSubsystem.getInstance(host.requireContext()) }
    private val weatherSubsystem by lazy { WeatherSubsystem.getInstance(host.requireContext()) }
    private val formatter by lazy { FormatService.getInstance(host.requireContext()) }
    private val prefs by lazy { UserPreferences(host.requireContext()) }

    private val distanceUnits: DistanceUnits
        get() = prefs.baseDistanceUnits

    private val precipitationDistanceUnits: DistanceUnits
        get() = if (distanceUnits.isMetric) DistanceUnits.Millimeters else DistanceUnits.Inches

    private var selectedMonth by mutableStateOf(LocalDate.now().month)
    private var location by mutableStateOf<Coordinate?>(null)
    private var elevation by mutableStateOf<Distance?>(null)
    private var months by mutableStateOf<List<ClimateMonth>?>(null)
    private var climateName by mutableStateOf<String?>(null)
    private var locationAutofillRequest by mutableStateOf(0)
    private var elevationAutofillRequest by mutableStateOf(0)

    private var loadJob: Job? = null

    @Composable
    fun Content() {
        val units = prefs.temperatureUnits
        val selected = months?.firstOrNull { it.month == selectedMonth }
        ClimateScreen(
            title = getString(R.string.tool_climate),
            subtitle = climateName?.let { getString(R.string.climate_zone_subtitle, it) }
                ?: getString(R.string.historic_temperature_years, 30),
            place = getPlaceText(),
            months = months,
            selectedMonth = selectedMonth,
            currentMonth = LocalDate.now().month,
            verdict = selected?.let(::getVerdict),
            formatTemperature = { formatter.formatDegrees(Temperature.celsius(it).convertTo(units).value) },
            formatPrecipitation = ::formatPrecipitation,
            onMonthSelected = { selectedMonth = it },
            location = location,
            elevation = elevation,
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
                    setElevationFromInput(Distance.meters(beaconElevation).convertTo(distanceUnits))
                }
            },
            elevationAutofillRequest = elevationAutofillRequest,
            onAutoElevationClick = {
                if (this@ClimateController.location == null) {
                    locationAutofillRequest++
                }
            }
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
        val year = LocalDate.now().year
        val currentLocation = location
        val currentElevation = elevation

        loadJob?.cancel()
        loadJob = host.lifecycleScope.launch {
            val (temperatures, precipitation) = coroutineScope {
                val temperatures = async {
                    weatherSubsystem.getTemperatureRanges(
                        year,
                        currentLocation,
                        currentElevation,
                        calibrated = false
                    )
                }
                val precipitation = async { weatherSubsystem.getMonthlyPrecipitation(currentLocation) }
                temperatures.await() to precipitation.await()
            }
            months = toMonths(temperatures, precipitation)
            climateName = getClimateName(temperatures, precipitation)
        }
    }

    private fun toMonths(
        temperatures: List<Pair<LocalDate, Range<Temperature>>>,
        precipitation: Map<Month, Distance>
    ): List<ClimateMonth>? {
        if (temperatures.isEmpty()) return null
        val byMonth = temperatures.groupBy { it.first.month }
        return Month.entries.mapNotNull { month ->
            val days = byMonth[month] ?: return@mapNotNull null
            ClimateMonth(
                month = month,
                lowCelsius = days.map { it.second.start.celsius().value }.average().toFloat(),
                highCelsius = days.map { it.second.end.celsius().value }.average().toFloat(),
                precipitation = precipitation[month]
            )
        }.takeIf { it.isNotEmpty() }
    }

    private fun getPlaceText(): String {
        val coordinate = location?.let { formatter.formatLocation(it) } ?: return getString(R.string.location)
        val height = elevation ?: return coordinate
        return getString(
            R.string.climate_place_line,
            coordinate,
            formatter.formatDistance(height, Units.getDecimalPlaces(height.units))
        )
    }

    private fun formatPrecipitation(amount: Distance): String {
        val value = amount.convertTo(precipitationDistanceUnits)
        return formatter.formatDistance(value, if (precipitationDistanceUnits.isMetric) 0 else 1)
    }

    /** "Mild, wet: 92 mm of rain": how the month feels, then what falls. */
    private fun getVerdict(month: ClimateMonth): String {
        val mean = (month.lowCelsius + month.highCelsius) / 2f
        val feel = getString(
            when {
                mean < 0f -> R.string.climate_feel_freezing
                mean < 8f -> R.string.climate_feel_cold
                mean < 14f -> R.string.climate_feel_cool
                mean < 20f -> R.string.climate_feel_mild
                mean < 26f -> R.string.climate_feel_warm
                else -> R.string.climate_feel_hot
            }
        )
        val precipitation = month.precipitation ?: return feel
        val millimeters = precipitation.convertTo(DistanceUnits.Millimeters).value
        val wetness = when {
            millimeters < 20f -> R.string.climate_feel_dry
            millimeters >= 200f -> R.string.climate_feel_very_wet
            millimeters >= 100f -> R.string.climate_feel_wet
            else -> null
        }?.let(::getString)
        val amount = getString(
            when {
                month.highCelsius <= 0f -> R.string.climate_snow_amount
                month.lowCelsius < 0f -> R.string.climate_rain_snow_amount
                else -> R.string.climate_rain_amount
            },
            formatPrecipitation(precipitation)
        )
        return if (wetness == null) {
            getString(R.string.climate_verdict, feel, amount)
        } else {
            getString(R.string.climate_verdict_wetness, feel, wetness, amount)
        }
    }

    private fun getString(@StringRes id: Int, vararg args: Any): String {
        return host.getString(id, *args)
    }

    private fun getClimateName(
        temperatures: List<Pair<LocalDate, Range<Temperature>>>,
        precipitation: Map<Month, Distance>
    ): String? {
        val monthlyAverageTemperatures = temperatures
            .filter { it.first.dayOfMonth == 15 }
            .associate {
                it.first.month to Temperature.celsius(
                    (it.second.start.celsius().value + it.second.end.celsius().value) / 2
                )
            }
        if (monthlyAverageTemperatures.isEmpty()) return null

        val climate = Meteorology.getKoppenGeigerClimateClassification(
            monthlyAverageTemperatures,
            precipitation
        )

        return when {
            climate.seasonalPrecipitationPattern == KoppenGeigerSeasonalPrecipitationPattern.Monsoon ->
                getString(R.string.climate_tropical_monsoon)

            climate.seasonalPrecipitationPattern == KoppenGeigerSeasonalPrecipitationPattern.Savanna ->
                getString(R.string.climate_tropical_savanna)

            climate.seasonalPrecipitationPattern == KoppenGeigerSeasonalPrecipitationPattern.Rainforest ->
                getString(R.string.climate_tropical_rainforest)

            climate.seasonalPrecipitationPattern == KoppenGeigerSeasonalPrecipitationPattern.Desert ->
                getString(R.string.climate_desert)

            climate.seasonalPrecipitationPattern == KoppenGeigerSeasonalPrecipitationPattern.Steppe ->
                getString(R.string.climate_steppe)

            climate.seasonalPrecipitationPattern == KoppenGeigerSeasonalPrecipitationPattern.Tundra ->
                getString(R.string.climate_tundra)

            climate.seasonalPrecipitationPattern == KoppenGeigerSeasonalPrecipitationPattern.IceCap ->
                getString(R.string.climate_ice_cap)

            climate.climateGroup == KoppenGeigerClimateGroup.Temperate -> getString(R.string.climate_temperate)
            climate.climateGroup == KoppenGeigerClimateGroup.Continental -> getString(R.string.climate_continental)
            else -> null
        }
    }
}

/**
 * Climate as a year of months, like a weather app's 10-day list: the place on one quiet line, the selected month's
 * high and low as the one display value with a single verdict line, then one row per month whose rounded bar spans
 * the month's low to high on a scale shared by the whole year, coloured by temperature. Tap a month to select it.
 */
@Composable
private fun ClimateScreen(
    title: String,
    subtitle: String,
    place: String,
    months: List<ClimateMonth>?,
    selectedMonth: Month,
    currentMonth: Month,
    verdict: String?,
    formatTemperature: (Float) -> String,
    formatPrecipitation: (Distance) -> String,
    onMonthSelected: (Month) -> Unit,
    location: Coordinate?,
    elevation: Distance?,
    onLocationChange: (Coordinate?) -> Unit,
    onElevationChange: (Distance?) -> Unit,
    locationAutofillRequest: Int,
    onAutoLocationClick: () -> Unit,
    onLocationBeaconSelected: (Beacon) -> Unit,
    elevationAutofillRequest: Int,
    onAutoElevationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPlaceOpen by rememberSaveable { mutableStateOf(false) }
    val selected = months?.firstOrNull { it.month == selectedMonth }

    // The bars grow out from their middles once the year arrives. Held here, outside the list, so scrolling the
    // months back into view never replays it.
    val reveal = remember { Animatable(0f) }
    val revealSpec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(months != null) {
        if (months != null) reveal.animateTo(1f, revealSpec)
    }

    TrailSensePage(
        title = title,
        subtitle = subtitle,
        backLabel = stringResource(R.string.back),
        modifier = modifier.testTag("climate_scroll")
    ) {
        item(key = "place") {
            ClimatePlaceLine(text = place, onClick = { isPlaceOpen = true })
        }

        item(key = "hero") {
            ClimateHero(
                monthName = selectedMonth.getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, Locale.getDefault()),
                high = selected?.let { formatTemperature(it.highCelsius) },
                low = selected?.let { formatTemperature(it.lowCelsius) },
                verdict = verdict
            )
        }

        item(key = "months") {
            if (months == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val coldest = months.minOf { it.lowCelsius }
                val warmest = months.maxOf { it.highCelsius }
                val wettest = months.maxOfOrNull {
                    it.precipitation?.convertTo(DistanceUnits.Millimeters)?.value ?: 0f
                } ?: 0f
                TrailSenseRowGroup(footer = stringResource(R.string.climate_sources)) {
                    months.forEach { month ->
                        row { position ->
                            ClimateMonthRow(
                                month = month,
                                position = position,
                                isSelected = month.month == selectedMonth,
                                isCurrent = month.month == currentMonth,
                                low = formatTemperature(month.lowCelsius),
                                high = formatTemperature(month.highCelsius),
                                precipitation = month.precipitation?.let(formatPrecipitation),
                                scaleMin = coldest,
                                scaleMax = warmest,
                                wettest = wettest,
                                reveal = { reveal.value },
                                onClick = { onMonthSelected(month.month) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (isPlaceOpen) {
        TrailSenseSheet(
            onDismissRequest = { isPlaceOpen = false },
            title = stringResource(R.string.location)
        ) {
            TrailSenseRowGroup {
                row { position ->
                    CoordinateInput(
                        location = location,
                        onLocationChange = onLocationChange,
                        autoFillRequest = locationAutofillRequest,
                        onAutoLocationClick = onAutoLocationClick,
                        onBeaconSelected = onLocationBeaconSelected,
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
            Spacer(Modifier.height(TrailSenseSpacing.xl))
            TrailSenseButton(
                text = stringResource(R.string.done),
                onClick = { isPlaceOpen = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("climate_location_done"),
                style = TrailSenseButtonStyle.Filled
            )
        }
    }
}

/** Where the climate is for, as one small tappable line; the inputs live in a sheet. */
@Composable
private fun ClimatePlaceLine(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TrailSenseSize.minimumTouchTarget)
            .clip(RoundedCornerShape(TrailSenseShape.groupOuter))
            .clickable(onClickLabel = stringResource(R.string.location), role = Role.Button, onClick = onClick)
            .padding(horizontal = TrailSenseSpacing.lg)
            .testTag("climate_location"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = TrailIcons.Location,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(TrailSenseSpacing.sm))
        Text(
            text = text,
            modifier = Modifier.weight(1f, fill = false),
            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            imageVector = TrailIcons.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = TrailSenseSpacing.xs)
                .size(18.dp)
                .rotate(90f)
        )
    }
}

/** The one answer: the selected month, its "18° / 7°" in display type, and one verdict line. */
@Composable
private fun ClimateHero(monthName: String, high: String?, low: String?, verdict: String?) {
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant
    val enter = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val exit = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    AnimatedContent(
        targetState = Triple(monthName, high to low, verdict),
        transitionSpec = {
            fadeIn(enter) togetherWith fadeOut(exit)
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("climate_title"),
        label = "climate_hero"
    ) { (name, values, line) ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = TrailSenseSpacing.lg,
                    end = TrailSenseSpacing.lg,
                    top = TrailSenseSpacing.lg,
                    bottom = TrailSenseSpacing.xl
                )
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = quiet
            )
            Text(
                text = buildAnnotatedString {
                    append(values.first ?: "--")
                    withStyle(SpanStyle(color = quiet)) {
                        append(" / ")
                        append(values.second ?: "--")
                    }
                },
                // Big and friendly; long pairs ("-12° / -25°") step down so they never clip.
                style = (
                    if ((values.first.orEmpty() + values.second.orEmpty()).length > 7) {
                        MaterialTheme.typography.displayMedium
                    } else {
                        MaterialTheme.typography.displayLarge
                    }
                    ).copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = line ?: " ",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = TrailSenseSpacing.xs)
            )
        }
    }
}

/**
 * One month: its name, the low, the rounded temperature bar on the year's shared scale, the high, and a small
 * droplet bar for its precipitation. The selected month is marked by a leading line, never a dot.
 */
@Composable
private fun ClimateMonthRow(
    month: ClimateMonth,
    position: TrailSenseGroupPosition,
    isSelected: Boolean,
    isCurrent: Boolean,
    low: String,
    high: String,
    precipitation: String?,
    scaleMin: Float,
    scaleMax: Float,
    wettest: Float,
    reveal: () -> Float,
    onClick: () -> Unit
) {
    val locale = Locale.getDefault()
    val name = month.month.getDisplayName(java.time.format.TextStyle.SHORT_STANDALONE, locale)
    val fullName = month.month.getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, locale)
    val description = stringResource(
        R.string.climate_month_row_description,
        fullName,
        low,
        high,
        precipitation.orEmpty()
    ).trimEnd(',', ' ')

    val container by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainer,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "month_container"
    )
    val accent by animateFloatAsState(
        if (isSelected) 1f else 0f,
        MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "month_accent"
    )
    val accentColor = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val lowFraction by animateFloatAsState(
        fraction(month.lowCelsius, scaleMin, scaleMax),
        MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "month_low"
    )
    val highFraction by animateFloatAsState(
        fraction(month.highCelsius, scaleMin, scaleMax),
        MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "month_high"
    )
    val wetFraction by animateFloatAsState(
        if (wettest > 0f) {
            ((month.precipitation?.convertTo(DistanceUnits.Millimeters)?.value ?: 0f) / wettest).coerceIn(0f, 1f)
        } else {
            0f
        },
        MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "month_wet"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("climate_month_${month.month.value}")
            .semantics {
                contentDescription = description
                selected = isSelected
            },
        shape = trailSenseGroupShape(position),
        color = container
    ) {
        Row(
            // The row reads as one sentence (its description), so its pieces stay silent.
            modifier = Modifier
                .heightIn(min = 64.dp)
                .padding(end = TrailSenseSpacing.lg)
                .clearAndSetSemantics { },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // The selection line: a short primary bar on the leading edge that grows in.
            Canvas(
                Modifier
                    .padding(start = TrailSenseSpacing.sm)
                    .size(width = 4.dp, height = 28.dp)
            ) {
                val h = size.height * accent
                if (h > 0f) {
                    drawRoundRect(
                        color = accentColor,
                        topLeft = Offset(0f, (size.height - h) / 2f),
                        size = Size(size.width, h),
                        cornerRadius = CornerRadius(size.width / 2f)
                    )
                }
            }
            Text(
                text = name,
                modifier = Modifier
                    .padding(start = TrailSenseSpacing.md)
                    .width(52.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected || isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = low,
                modifier = Modifier.width(48.dp),
                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                maxLines = 1
            )
            Canvas(
                Modifier
                    .weight(1f)
                    .padding(horizontal = TrailSenseSpacing.md)
                    .height(12.dp)
            ) {
                val radius = size.height / 2f
                drawRoundRect(color = track, cornerRadius = CornerRadius(radius))
                val lowX = lowFraction * size.width
                val highX = highFraction * size.width
                val middle = (lowX + highX) / 2f
                val grow = reveal()
                var start = middle + (lowX - middle) * grow
                var end = middle + (highX - middle) * grow
                if (end - start < size.height) {
                    val center = ((start + end) / 2f).coerceIn(radius, size.width - radius)
                    start = center - radius
                    end = center + radius
                }
                val stops = 6
                val colors = (0..stops).map {
                    temperatureColor(month.lowCelsius + (month.highCelsius - month.lowCelsius) * it / stops)
                }
                drawRoundRect(
                    brush = Brush.horizontalGradient(colors, startX = start, endX = end),
                    topLeft = Offset(start, 0f),
                    size = Size(end - start, size.height),
                    cornerRadius = CornerRadius(radius)
                )
            }
            Text(
                text = high,
                modifier = Modifier.width(48.dp),
                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            if (precipitation != null) {
                // The droplet bar: how wet the month is next to the wettest month of the year.
                Canvas(
                    Modifier
                        .padding(start = TrailSenseSpacing.sm)
                        .size(width = 6.dp, height = 28.dp)
                ) {
                    val radius = size.width / 2f
                    drawRoundRect(color = track, cornerRadius = CornerRadius(radius))
                    val h = max(size.width, size.height * wetFraction * reveal())
                    drawRoundRect(
                        color = RainColor,
                        topLeft = Offset(0f, size.height - h),
                        size = Size(size.width, h),
                        cornerRadius = CornerRadius(radius)
                    )
                }
            }
        }
    }
}

private fun fraction(value: Float, minimum: Float, maximum: Float): Float {
    val span = maximum - minimum
    if (span <= 0f) return 0.5f
    return ((value - minimum) / span).coerceIn(0f, 1f)
}

private val RainColor = Color(0xFF4F9DF0)

/**
 * The temperature's own colour, the same in light and dark: deep blue when it freezes, through teal and green when
 * it's mild, to amber and red when it's hot. The hue comes from the temperature, not the theme.
 */
private val TemperatureStops = listOf(
    -25f to Color(0xFF7C6CF2),
    -10f to Color(0xFF5B8DF5),
    0f to Color(0xFF4FB3F2),
    8f to Color(0xFF3DC7C2),
    15f to Color(0xFF6CCF7E),
    21f to Color(0xFFC9D35A),
    26f to Color(0xFFF4BA45),
    32f to Color(0xFFF38B3C),
    40f to Color(0xFFE8553F)
)

private fun temperatureColor(celsius: Float): Color {
    val first = TemperatureStops.first()
    if (celsius <= first.first) return first.second
    TemperatureStops.zipWithNext().forEach { (a, b) ->
        if (celsius <= b.first) {
            return lerp(a.second, b.second, (celsius - a.first) / (b.first - a.first))
        }
    }
    return TemperatureStops.last().second
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

    TrailSenseInputField(
        value = locationText,
        onValueChange = {
            locationText = it
            onLocationChange(Coordinate.parse(it))
        },
        label = stringResource(R.string.location),
        modifier = modifier.testTag("utm"),
        position = position,
        keyboardType = KeyboardType.Text,
        isError = locationText.isNotBlank() && parsedCoordinate == null,
        errorText = stringResource(R.string.coordinate_input_invalid_location),
        enabled = !isLoadingLocation,
        leadingIcon = {
            if (isLoadingLocation) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("gps_loading"),
                    strokeWidth = 3.dp
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
                    Icon(imageVector = TrailIcons.Location, contentDescription = stringResource(R.string.gps))
                }
            }
        },
        trailingIcon = {
            IconButton(
                onClick = { showHelp = true },
                modifier = Modifier.testTag("coordinate_input_help_btn")
            ) {
                Icon(
                    imageVector = TrailIcons.Help,
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

    TrailSenseInputField(
        value = elevationText,
        onValueChange = {
            elevationText = it
            publish(text = it)
        },
        label = stringResource(R.string.elevation),
        modifier = modifier.testTag("elevation_input"),
        position = position,
        enabled = !isLoadingElevation,
        unit = formatter.getDistanceUnitName(unit, true),
        onUnitClick = { isUnitOpen = true },
        leadingIcon = {
            if (isLoadingElevation) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("elevation_loading"),
                    strokeWidth = 3.dp
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
                    Icon(imageVector = TrailIcons.Altitude, contentDescription = stringResource(R.string.elevation))
                }
            }
        }
    )

    if (isUnitOpen) {
        TrailSenseOptionsDialog(
            title = stringResource(R.string.elevation),
            options = units.map { formatter.getDistanceUnitName(it, false) to it },
            selected = unit,
            onSelect = { option ->
                isUnitOpen = false
                val converted = parseElevation(elevationText, unit)?.convertTo(option)
                unit = option
                elevationText = formatElevationAmount(converted, option)
                publish(text = elevationText, newUnit = option)
            },
            onDismiss = { isUnitOpen = false }
        )
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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun ClimateScreenPreview() {
    val lows = listOf(-6f, -5f, -1f, 4f, 10f, 15f, 18f, 17f, 13f, 6f, 1f, -3f)
    val highs = listOf(1f, 3f, 8f, 15f, 21f, 26f, 28f, 27f, 23f, 16f, 9f, 3f)
    val rain = listOf(80f, 70f, 90f, 95f, 100f, 105f, 110f, 100f, 95f, 105f, 100f, 90f)
    TrailSenseTheme {
        ClimateScreen(
            title = "Climate",
            subtitle = "Continental climate · 30-year average",
            place = "42.0000°, -72.0000° · 300 m",
            months = Month.entries.map {
                ClimateMonth(
                    it,
                    lows[it.ordinal],
                    highs[it.ordinal],
                    Distance.from(rain[it.ordinal], DistanceUnits.Millimeters)
                )
            },
            selectedMonth = Month.NOVEMBER,
            currentMonth = Month.NOVEMBER,
            verdict = "Cool, wet: 100 mm of rain",
            formatTemperature = { "${it.toInt()}°" },
            formatPrecipitation = { "${it.value.toInt()} mm" },
            onMonthSelected = {},
            location = null,
            elevation = null,
            onLocationChange = {},
            onElevationChange = {},
            locationAutofillRequest = 0,
            onAutoLocationClick = {},
            onLocationBeaconSelected = {},
            elevationAutofillRequest = 0,
            onAutoElevationClick = {}
        )
    }
}
