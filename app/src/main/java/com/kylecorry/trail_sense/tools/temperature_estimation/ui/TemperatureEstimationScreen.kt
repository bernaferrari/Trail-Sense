@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.temperature_estimation.ui

import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.zIndex
import com.kylecorry.trail_sense.shared.icons.trail.ElevationGain
import com.kylecorry.trail_sense.shared.icons.trail.ElevationLoss
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.withStyle
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import androidx.compose.animation.animateContentSize
import com.kylecorry.trail_sense.shared.views.compose.trailSenseToneColors
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.icons.trail.Thermometer
import com.kylecorry.trail_sense.shared.icons.trail.CategoryWater
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.sense.readAll
import com.kylecorry.sol.math.MathExtensions.roundPlaces
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.Temperature
import com.kylecorry.sol.units.TemperatureUnits
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.safeRoundToInt
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseOptionsDialog
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.rememberTrailSenseBack
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import kotlinx.coroutines.launch
import java.time.Duration

private enum class WindSpeedUnit(val distance: DistanceUnits, val time: TimeUnits) {
    KilometersPerHour(DistanceUnits.Kilometers, TimeUnits.Hours),
    MilesPerHour(DistanceUnits.Miles, TimeUnits.Hours),
    MetersPerSecond(DistanceUnits.Meters, TimeUnits.Seconds)
}

@Composable
fun TemperatureEstimationToolScreen() {
    val context = LocalContext.current
    val sensorService = remember { SensorService(context) }
    val thermometer = remember { sensorService.getThermometer() }
    val prefs = remember { UserPreferences(context) }
    val location = remember { LocationSubsystem.getInstance(context) }
    val formatService = remember { FormatService.getInstance(context) }

    TemperatureEstimationScreen(
        prefs = prefs,
        formatService = formatService,
        initialElevation = location.elevation.convertTo(prefs.baseDistanceUnits),
        sensorTemperature = { thermometer.temperature },
        hasThermometerReading = {
            thermometer.hasValidReading || thermometer.temperature != 0f
        },
        readThermometer = {
            readAll(
                listOf(thermometer),
                Duration.ofSeconds(10),
                forceStopOnCompletion = true
            )
        }
    )
}

/**
 * The estimate leads; under it the weather where you are (the temperature field fills itself from the
 * thermometer with the button at its end), then the two elevations as their own group.
 */
@Composable
private fun TemperatureEstimationScreen(
    prefs: UserPreferences,
    formatService: FormatService,
    initialElevation: Distance,
    sensorTemperature: () -> Float,
    hasThermometerReading: () -> Boolean,
    readThermometer: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    val temperatureUnits = prefs.temperatureUnits
    val temperatureUnitOptions = remember {
        if (temperatureUnits == TemperatureUnits.Celsius) {
            listOf(TemperatureUnits.Celsius, TemperatureUnits.Fahrenheit)
        } else {
            listOf(TemperatureUnits.Fahrenheit, TemperatureUnits.Celsius)
        }
    }
    val elevationUnits = remember { DistanceUtils.elevationDistanceUnits }
    val speedUnits = remember {
        if (prefs.distanceUnits == UserPreferences.DistanceUnits.Meters) {
            listOf(
                WindSpeedUnit.KilometersPerHour,
                WindSpeedUnit.MetersPerSecond,
                WindSpeedUnit.MilesPerHour
            )
        } else {
            listOf(
                WindSpeedUnit.MilesPerHour,
                WindSpeedUnit.KilometersPerHour,
                WindSpeedUnit.MetersPerSecond
            )
        }
    }

    val roundedElevation = initialElevation.value.roundPlaces(
        Units.getDecimalPlaces(initialElevation.units)
    )
    // 877, not 877.0: a whole number reads without its trailing zero.
    var baseElevationText by remember {
        mutableStateOf(
            if (roundedElevation % 1f == 0f) roundedElevation.toInt().toString() else roundedElevation.toString()
        )
    }
    var baseElevationUnit by remember { mutableStateOf(initialElevation.units) }
    var destElevationText by remember { mutableStateOf("") }
    var destElevationUnit by remember { mutableStateOf(initialElevation.units) }
    var baseTemperatureText by remember { mutableStateOf("") }
    var baseTemperatureUnit by remember { mutableStateOf(temperatureUnits) }
    var humidityText by remember { mutableStateOf("") }
    var windSpeedText by remember { mutableStateOf("") }
    var windSpeedUnit by remember { mutableStateOf(speedUnits.first()) }
    var isLoading by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf<Picker?>(null) }
    val destinationFocus = remember { FocusRequester() }

    fun setThermometerFieldFromSensor() {
        val sensorTemp = sensorTemperature()
        if (hasThermometerReading() && !sensorTemp.isNaN()) {
            val temp = Temperature.celsius(sensorTemp).convertTo(temperatureUnits)
            baseTemperatureText = temp.value.safeRoundToInt().toString()
            baseTemperatureUnit = temperatureUnits
        }
    }

    LaunchedEffect(Unit) {
        setThermometerFieldFromSensor()
    }

    val estimated = estimateTemperature(
        baseTemperatureText = baseTemperatureText,
        baseTemperatureUnit = baseTemperatureUnit,
        baseElevationText = baseElevationText,
        baseElevationUnit = baseElevationUnit,
        destElevationText = destElevationText,
        destElevationUnit = destElevationUnit,
        humidityText = humidityText,
        windSpeedText = windSpeedText,
        windSpeedUnit = windSpeedUnit,
        outputUnits = temperatureUnits
    )
    val title = estimated?.takeIf { !it.value.isNaN() }?.let {
        formatService.formatTemperature(it, 0, false)
    } ?: stringResource(R.string.dash)
    val humidity = humidityText.toFloatOrNull()
    val humidityError = humidityText.isNotEmpty() && (humidity == null || humidity !in 0f..100f)

    val hereTitle = stringResource(R.string.sky_temperature_here)
    val thereTitle = stringResource(R.string.sky_temperature_there)
    val conditionsTitle = stringResource(R.string.sky_temperature_conditions)
    val conditionsFooter = stringResource(R.string.sky_temperature_conditions_footer)
    val readThermometerLabel = stringResource(R.string.sky_temperature_read_thermometer)
    val humidityErrorText = stringResource(R.string.humidity_must_be_between_0_and_100)
    val labels = TemperatureLabels(
        baseElevation = stringResource(R.string.base_elevation),
        destinationElevation = stringResource(R.string.destination_elevation),
        currentTemperature = stringResource(R.string.current_temperature),
        humidity = stringResource(R.string.humidity),
        windSpeed = stringResource(R.string.wind_speed)
    )
    val elevationLabel = stringResource(R.string.elevation)

    fun autofill() {
        scope.launch {
            isLoading = true
            readThermometer()
            setThermometerFieldFromSensor()
            isLoading = false
        }
    }

    TrailSensePage(
        title = stringResource(R.string.tool_temperature_estimation_title),
        onBack = rememberTrailSenseBack(),
        backLabel = stringResource(R.string.back)
    ) {
        // How warm will it be where I'm going? Here and there, each with its temperature, joined by the climb
        // between them; the inputs sit below in the order you think of them.
        item("estimate") {
            val valid = estimated?.takeIf { !it.value.isNaN() }
            val base = baseTemperatureText.toFloatOrNull()?.let {
                Temperature.from(it, baseTemperatureUnit).convertTo(temperatureUnits)
            }
            val baseElevation = baseElevationText.toFloatOrNull()?.let { Distance.from(it, baseElevationUnit) }
            val destElevation = destElevationText.toFloatOrNull()?.let { Distance.from(it, destElevationUnit) }
            val climb = if (baseElevation != null && destElevation != null) {
                Distance.from(destElevation.meters().value - baseElevation.meters().value, DistanceUnits.Meters)
                    .convertTo(baseElevation.units)
            } else {
                null
            }
            TemperatureTrip(
                hereTemperature = base?.let { formatService.formatTemperature(it, 0, false) },
                hereElevation = baseElevation?.let { formatService.formatDistance(it, Units.getDecimalPlaces(it.units), false) },
                thereTemperature = valid?.let { title },
                thereElevation = destElevation?.let { formatService.formatDistance(it, Units.getDecimalPlaces(it.units), false) },
                climb = climb,
                formatClimb = {
                    formatService.formatDistance(
                        Distance.from(kotlin.math.abs(it.value), it.units),
                        Units.getDecimalPlaces(it.units),
                        false
                    )
                },
                change = if (valid != null && base != null) valid.value - base.value else null,
                formatChange = { formatService.formatDegrees(kotlin.math.abs(it)) },
                estimateCelsius = valid?.celsius()?.value,
                hasBase = base != null,
                onAddDestination = { runCatching { destinationFocus.requestFocus() } }
            )
        }

        trailSenseSection(key = "here", title = hereTitle) {
            row { position ->
                TrailSenseInputField(
                    value = baseTemperatureText,
                    onValueChange = { baseTemperatureText = it },
                    label = labels.currentTemperature,
                    position = position,
                    enabled = !isLoading,
                    unit = formatService.getTemperatureUnitName(baseTemperatureUnit, true),
                    onUnitClick = { picker = Picker.Temperature },
                    modifier = Modifier.testTag("temp_est_base_temperature"),
                    // The phone's thermometer fills this field, so its button sits in front of it.
                    leadingIcon = {
                        TemperatureWell(
                            isLoading = isLoading,
                            contentDescription = readThermometerLabel,
                            onClick = ::autofill,
                            modifier = Modifier.testTag("temp_est_autofill")
                        )
                    }
                )
            }
            row { position ->
                TrailSenseInputField(
                    value = baseElevationText,
                    onValueChange = { baseElevationText = it },
                    label = elevationLabel,
                    position = position,
                    enabled = !isLoading,
                    unit = formatService.getDistanceUnitName(baseElevationUnit, true),
                    onUnitClick = { picker = Picker.BaseElevation },
                    modifier = Modifier.testTag("temp_est_base_elevation")
                )
            }
        }

        trailSenseSection(key = "there", title = thereTitle) {
            row { position ->
                TrailSenseInputField(
                    value = destElevationText,
                    onValueChange = { destElevationText = it },
                    label = elevationLabel,
                    position = position,
                    enabled = !isLoading,
                    unit = formatService.getDistanceUnitName(destElevationUnit, true),
                    onUnitClick = { picker = Picker.DestinationElevation },
                    modifier = Modifier
                        .focusRequester(destinationFocus)
                        .testTag("temp_est_dest_elevation")
                )
            }
        }

        trailSenseSection(key = "conditions", title = conditionsTitle, footer = conditionsFooter) {
            row { position ->
                TrailSenseInputField(
                    value = humidityText,
                    onValueChange = { humidityText = it },
                    label = labels.humidity,
                    position = position,
                    isError = humidityError,
                    errorText = humidityErrorText,
                    unit = "%",
                    modifier = Modifier.testTag("temp_est_humidity")
                )
            }
            row { position ->
                TrailSenseInputField(
                    value = windSpeedText,
                    onValueChange = { windSpeedText = it },
                    label = labels.windSpeed,
                    position = position,
                    enabled = !isLoading,
                    unit = formatService.getSpeedUnitName(windSpeedUnit.distance, windSpeedUnit.time, true),
                    onUnitClick = { picker = Picker.WindSpeed },
                    modifier = Modifier.testTag("temp_est_wind_speed")
                )
            }
        }
    }

    when (picker) {
        Picker.BaseElevation -> UnitPickerDialog(
            title = labels.baseElevation,
            units = elevationUnits,
            selected = baseElevationUnit,
            getName = { formatService.getDistanceUnitName(it, true) },
            onSelect = {
                baseElevationUnit = it
                picker = null
            },
            onDismiss = { picker = null }
        )

        Picker.DestinationElevation -> UnitPickerDialog(
            title = labels.destinationElevation,
            units = elevationUnits,
            selected = destElevationUnit,
            getName = { formatService.getDistanceUnitName(it, true) },
            onSelect = {
                destElevationUnit = it
                picker = null
            },
            onDismiss = { picker = null }
        )

        Picker.Temperature -> UnitPickerDialog(
            title = labels.currentTemperature,
            units = temperatureUnitOptions,
            selected = baseTemperatureUnit,
            getName = { formatService.getTemperatureUnitName(it, true) },
            onSelect = {
                baseTemperatureUnit = it
                picker = null
            },
            onDismiss = { picker = null }
        )

        Picker.WindSpeed -> UnitPickerDialog(
            title = labels.windSpeed,
            units = speedUnits,
            selected = windSpeedUnit,
            getName = { formatService.getSpeedUnitName(it.distance, it.time, true) },
            onSelect = {
                windSpeedUnit = it
                picker = null
            },
            onDismiss = { picker = null }
        )

        null -> Unit
    }
}

/**
 * The object: a before and after. "Here" on top with the temperature where you are, "There" under it with the
 * estimate, and between them, sitting on the seam, the link: how much higher or lower you're going and how much
 * colder or warmer that makes it. The "There" card is the answer, in primary container, with one verdict line. Before
 * there is a temperature or a destination, the link and the verdict line say the one thing to do next.
 */
@Composable
private fun TemperatureTrip(
    hereTemperature: String?,
    hereElevation: String?,
    thereTemperature: String?,
    thereElevation: String?,
    climb: Distance?,
    formatClimb: (Distance) -> String,
    change: Float?,
    formatChange: (Float) -> String,
    estimateCelsius: Float?,
    hasBase: Boolean,
    onAddDestination: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val dash = stringResource(R.string.dash)
    val verdict = when {
        !hasBase -> stringResource(R.string.sky_temperature_start)
        estimateCelsius == null -> stringResource(R.string.sky_temperature_add_base_elevation)
        else -> stringResource(
            when {
                estimateCelsius < 0f -> R.string.sky_temperature_verdict_freezing
                estimateCelsius < 10f -> R.string.sky_temperature_verdict_cold
                estimateCelsius < 18f -> R.string.sky_temperature_verdict_cool
                estimateCelsius < 30f -> R.string.sky_temperature_verdict_warm
                else -> R.string.sky_temperature_verdict_hot
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = TrailSenseSpacing.sm)
    ) {
        TripCard(
            label = stringResource(R.string.sky_temperature_here_short),
            elevation = hereElevation ?: dash,
            temperature = hereTemperature ?: dash,
            container = scheme.surfaceContainerHigh,
            content = scheme.onSurface,
            quiet = scheme.onSurfaceVariant,
            shape = RoundedCornerShape(
                topStart = TrailSenseShape.hero,
                topEnd = TrailSenseShape.hero,
                bottomStart = TrailSenseShape.groupInner,
                bottomEnd = TrailSenseShape.groupInner
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = TrailSenseSpacing.xl,
                end = TrailSenseSpacing.xl,
                top = TrailSenseSpacing.xl,
                bottom = TrailSenseSpacing.xxl
            )
        )
        // The link sits on the seam between the two cards, drawn over both.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TrailSenseSpacing.xxs)
                .zIndex(1f),
            contentAlignment = Alignment.Center
        ) {
            TripLink(
                climb = climb,
                formatClimb = formatClimb,
                change = change,
                formatChange = formatChange,
                onAddDestination = onAddDestination,
                modifier = Modifier.wrapContentHeight(unbounded = true)
            )
        }
        TripCard(
            label = stringResource(R.string.sky_temperature_there_short),
            elevation = thereElevation ?: dash,
            temperature = thereTemperature ?: dash,
            container = scheme.primaryContainer,
            content = scheme.onPrimaryContainer,
            quiet = scheme.onPrimaryContainer.copy(alpha = 0.72f),
            shape = RoundedCornerShape(
                topStart = TrailSenseShape.groupInner,
                topEnd = TrailSenseShape.groupInner,
                bottomStart = TrailSenseShape.hero,
                bottomEnd = TrailSenseShape.hero
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = TrailSenseSpacing.xl,
                end = TrailSenseSpacing.xl,
                top = TrailSenseSpacing.xxl,
                bottom = TrailSenseSpacing.xl
            ),
            verdict = verdict
        )
    }
}

/** One end of the trip: where it is (a label and its elevation) on the left, its temperature as the display value. */
@Composable
private fun TripCard(
    label: String,
    elevation: String,
    temperature: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    quiet: androidx.compose.ui.graphics.Color,
    shape: androidx.compose.ui.graphics.Shape,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    verdict: String? = null
) {
    val description = listOfNotNull(label, elevation, temperature, verdict).joinToString(", ")
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = description },
        shape = shape,
        color = container,
        contentColor = content
    ) {
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .animateContentSize(MaterialTheme.motionScheme.fastSpatialSpec())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        color = content
                    )
                    Text(
                        text = elevation,
                        style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                        color = quiet,
                        maxLines = 1
                    )
                }
                Text(
                    text = temperature,
                    style = (
                        if (temperature.length > 6) {
                            MaterialTheme.typography.displaySmall
                        } else {
                            MaterialTheme.typography.displayMedium
                        }
                        ).copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    color = content,
                    maxLines = 1,
                    modifier = Modifier.padding(start = TrailSenseSpacing.md)
                )
            }
            if (verdict != null) {
                Text(
                    text = verdict,
                    style = MaterialTheme.typography.titleMedium,
                    color = content,
                    modifier = Modifier.padding(top = TrailSenseSpacing.md)
                )
            }
        }
    }
}

/**
 * The link between here and there: the climb ("1,200 m higher") and what it does to the temperature ("8° colder"),
 * in a small surface-coloured pill with a hill arrow. With no destination yet it asks for one, and tapping it goes
 * to that field.
 */
@Composable
private fun TripLink(
    climb: Distance?,
    formatClimb: (Distance) -> String,
    change: Float?,
    formatChange: (Float) -> String,
    onAddDestination: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val climbText = when {
        climb == null -> null
        kotlin.math.abs(climb.meters().value) < 1f -> stringResource(R.string.sky_temperature_same_elevation)
        climb.value > 0f -> stringResource(R.string.sky_temperature_higher, formatClimb(climb))
        else -> stringResource(R.string.sky_temperature_lower, formatClimb(climb))
    }
    val changeText = when {
        change == null || climb == null -> null
        kotlin.math.abs(change) < 0.5f -> null
        change < 0f -> stringResource(R.string.sky_temperature_colder, formatChange(change))
        else -> stringResource(R.string.sky_temperature_warmer, formatChange(change))
    }
    val text = when {
        climbText == null -> stringResource(R.string.sky_temperature_add_destination)
        changeText == null -> climbText
        else -> stringResource(R.string.sky_temperature_link, climbText, changeText)
    }
    val icon = when {
        climb == null -> TrailIcons.ElevationGain
        climb.value < 0f -> TrailIcons.ElevationLoss
        else -> TrailIcons.ElevationGain
    }
    Surface(
        onClick = onAddDestination,
        enabled = climb == null,
        modifier = modifier.heightIn(min = 40.dp),
        shape = androidx.compose.foundation.shape.CircleShape,
        color = scheme.surface,
        contentColor = scheme.onSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, scheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 40.dp)
                .padding(horizontal = TrailSenseSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
        ) {
            Icon(
                painter = rememberVectorPainter(icon),
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(18.dp)
            )
            val enter = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
            val exit = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
            androidx.compose.animation.AnimatedContent(
                targetState = text,
                transitionSpec = {
                    androidx.compose.animation.fadeIn(enter) togetherWith androidx.compose.animation.fadeOut(exit)
                },
                label = "trip_link"
            ) { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                    color = scheme.onSurface,
                    maxLines = 1
                )
            }
        }
    }
}

/** The family's 40 dp Cookie6 well in 12% primary, as the button that reads the phone's thermometer. */
@Composable
private fun TemperatureWell(
    isLoading: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowWell(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier
            .padding(start = TrailSenseSpacing.xs)
            .semantics { this.contentDescription = contentDescription }
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Icon(
                painter = rememberVectorPainter(TrailIcons.Thermometer),
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

private data class TemperatureLabels(
    val baseElevation: String,
    val destinationElevation: String,
    val currentTemperature: String,
    val humidity: String,
    val windSpeed: String
)

@Composable
private fun <T> UnitPickerDialog(
    title: String,
    units: List<T>,
    selected: T,
    getName: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    TrailSenseOptionsDialog(
        title = title,
        options = units.map { getName(it) to it },
        selected = selected,
        onSelect = onSelect,
        onDismiss = onDismiss
    )
}

private fun estimateTemperature(
    baseTemperatureText: String,
    baseTemperatureUnit: TemperatureUnits,
    baseElevationText: String,
    baseElevationUnit: DistanceUnits,
    destElevationText: String,
    destElevationUnit: DistanceUnits,
    humidityText: String,
    windSpeedText: String,
    windSpeedUnit: WindSpeedUnit,
    outputUnits: TemperatureUnits
): Temperature? {
    var temp = Temperature.from(
        baseTemperatureText.toFloatOrNull() ?: return null,
        baseTemperatureUnit
    ).convertTo(TemperatureUnits.Celsius)

    val destinationElevation = destElevationText.toFloatOrNull()?.let {
        Distance.from(it, destElevationUnit).meters()
    }
    val baseElevation = baseElevationText.toFloatOrNull()?.let {
        Distance.from(it, baseElevationUnit).meters()
    }
    val humidity = humidityText.toFloatOrNull()?.takeIf { it in 0f..100f }
    val windSpeed = windSpeedText.toFloatOrNull()?.let {
        Speed.from(it, windSpeedUnit.distance, windSpeedUnit.time)
    }

    if (destinationElevation != null) {
        if (baseElevation == null) {
            return null
        }
        temp = Meteorology.getTemperatureAtElevation(temp, baseElevation, destinationElevation)
    }

    if (humidity != null) {
        temp = Temperature.celsius(
            Meteorology.getHeatIndex(temp.celsius().value, humidity)
        ).convertTo(temp.units)
    }

    if (windSpeed != null) {
        temp = Meteorology.getWindChill(temp, windSpeed)
    }

    return temp.convertTo(outputUnits)
}

private enum class Picker {
    BaseElevation,
    DestinationElevation,
    Temperature,
    WindSpeed
}
