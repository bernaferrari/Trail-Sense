@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.temperature_estimation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.kylecorry.trail_sense.shared.icons.trail.DropDownExpanded
import com.kylecorry.trail_sense.shared.icons.trail.Thermometer
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.icons.trail.Wind
import com.kylecorry.trail_sense.shared.safeRoundToInt
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseOptionsDialog
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowWell
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.rememberTrailSenseBack
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import kotlinx.coroutines.launch
import java.time.Duration
import kotlin.math.abs
import kotlin.math.roundToInt

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
        // The last elevation the GPS or altimeter knew; none at all (never recorded) leaves the field for the user.
        initialElevation = location.elevation.convertTo(prefs.baseDistanceUnits),
        isElevationKnown = location.elevationAge < Duration.ofDays(KNOWN_ELEVATION_DAYS),
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

private const val KNOWN_ELEVATION_DAYS = 3650L

/** How far up the slope a climb of this many metres (or more) reaches: the summit. */
private const val SUMMIT_CLIMB_METERS = 1500f

/**
 * How cold will it be where I'm going? A mountain is the hero: you stand at its foot with the temperature where you
 * are, the place you're going sits up the slope with its estimate, and the mountain cools toward the summit as the
 * climb gets colder (a snow cap shows below freezing). Under it, one line: the next thing to type, or the answer as
 * one display number and its verdict. Then the three inputs in the order you think them (the temperature here, where
 * you're going, your own elevation from the GPS), and humidity and wind behind a quiet "More conditions".
 */
@Composable
private fun TemperatureEstimationScreen(
    prefs: UserPreferences,
    formatService: FormatService,
    initialElevation: Distance,
    isElevationKnown: Boolean,
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

    // 877, not 877.0: a whole number reads without its trailing zero.
    val autoElevationText = remember {
        if (!isElevationKnown) {
            ""
        } else {
            val rounded = initialElevation.value.roundPlaces(Units.getDecimalPlaces(initialElevation.units))
            if (rounded % 1f == 0f) rounded.toInt().toString() else rounded.toString()
        }
    }
    var baseElevationText by remember { mutableStateOf(autoElevationText) }
    var baseElevationUnit by remember { mutableStateOf(initialElevation.units) }
    var destElevationText by remember { mutableStateOf("") }
    var destElevationUnit by remember { mutableStateOf(initialElevation.units) }
    var baseTemperatureText by remember { mutableStateOf("") }
    var baseTemperatureUnit by remember { mutableStateOf(temperatureUnits) }
    var humidityText by remember { mutableStateOf("") }
    var windSpeedText by remember { mutableStateOf("") }
    var windSpeedUnit by remember { mutableStateOf(speedUnits.first()) }
    var isLoading by remember { mutableStateOf(false) }
    var showConditions by remember { mutableStateOf(false) }
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

    fun autofill() {
        scope.launch {
            isLoading = true
            readThermometer()
            setThermometerFieldFromSensor()
            isLoading = false
        }
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
    )?.takeIf { !it.value.isNaN() }

    val base = baseTemperatureText.toFloatOrNull()?.let {
        Temperature.from(it, baseTemperatureUnit).convertTo(temperatureUnits)
    }
    val baseElevation = baseElevationText.toFloatOrNull()?.let { Distance.from(it, baseElevationUnit) }
    val destElevation = destElevationText.toFloatOrNull()?.let { Distance.from(it, destElevationUnit) }
    val humidity = humidityText.toFloatOrNull()
    val humidityError = humidityText.isNotEmpty() && (humidity == null || humidity !in 0f..100f)
    val hasConditions = (humidity != null && !humidityError) || windSpeedText.toFloatOrNull() != null
    // The estimate is an answer only when it says something new: a destination, or how it feels with humidity or wind.
    val answer = estimated?.takeIf { destElevation != null || hasConditions }

    fun formatElevation(distance: Distance): String =
        formatService.formatDistance(distance, Units.getDecimalPlaces(distance.units), false)

    val advice = answer?.let { stringResource(adviceFor(it.celsius().value)) }
    val place = when {
        answer == null -> null
        destElevation != null -> stringResource(R.string.sky_temperature_result_at, formatElevation(destElevation))
        else -> stringResource(R.string.sky_temperature_result_feels)
    }
    val instruction = when {
        base == null -> stringResource(R.string.sky_temperature_start)
        destElevation != null && baseElevation == null -> stringResource(R.string.sky_temperature_add_base_elevation)
        else -> stringResource(R.string.sky_temperature_next)
    }

    val labels = TemperatureLabels(
        baseElevation = stringResource(R.string.sky_temperature_field_base),
        destinationElevation = stringResource(R.string.sky_temperature_field_there),
        currentTemperature = stringResource(R.string.sky_temperature_field_here),
        humidity = stringResource(R.string.humidity),
        windSpeed = stringResource(R.string.wind_speed)
    )
    val readThermometerLabel = stringResource(R.string.sky_temperature_read_thermometer)
    val humidityErrorText = stringResource(R.string.humidity_must_be_between_0_and_100)
    val autoElevationNote = stringResource(R.string.sky_temperature_elevation_auto)
    val conditionsFooter = stringResource(R.string.sky_temperature_conditions_footer)

    TrailSensePage(
        title = stringResource(R.string.tool_temperature_estimation_title),
        onBack = rememberTrailSenseBack(),
        backLabel = stringResource(R.string.back)
    ) {
        item("mountain") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // The destination's temperature without humidity or wind: the mountain shows the air, not the feel.
                val destinationAir = if (destElevation != null && baseElevation != null && base != null) {
                    Meteorology.getTemperatureAtElevation(
                        base.celsius(),
                        baseElevation.meters(),
                        destElevation.meters()
                    ).convertTo(temperatureUnits)
                } else {
                    null
                }
                TemperatureMountain(
                    hereTemperature = base?.let { formatService.formatTemperature(it, 0, false) },
                    hereElevation = baseElevation?.let(::formatElevation),
                    thereTemperature = destinationAir?.let { formatService.formatTemperature(it, 0, false) },
                    thereElevation = destElevation?.let(::formatElevation),
                    climbMeters = if (baseElevation != null && destElevation != null) {
                        destElevation.meters().value - baseElevation.meters().value
                    } else {
                        null
                    },
                    changeCelsius = if (destinationAir != null && base != null) {
                        destinationAir.celsius().value - base.celsius().value
                    } else {
                        null
                    },
                    destinationCelsius = destinationAir?.celsius()?.value,
                    onAskDestination = { runCatching { destinationFocus.requestFocus() } }
                )

                // One line under the object: what to type next, or the answer and what it means.
                val answerIn = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
                val answerOut = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
                AnimatedContent(
                    targetState = answer?.let { formatService.formatTemperature(it, 0, false) },
                    transitionSpec = {
                        fadeIn(answerIn) togetherWith fadeOut(answerOut)
                    },
                    contentKey = { it == null },
                    label = "temperature_answer",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = TrailSenseSpacing.lg)
                ) { value ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (value == null) {
                            Text(
                                text = instruction,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = value,
                                style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = stringResource(R.string.sky_temperature_link, place ?: "", advice ?: ""),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // The inputs in the order you think them: it's this warm here, I'm going this high, and I'm at this elevation.
        trailSenseSection(key = "inputs") {
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
                    value = destElevationText,
                    onValueChange = { destElevationText = it },
                    label = labels.destinationElevation,
                    position = position,
                    enabled = !isLoading,
                    unit = formatService.getDistanceUnitName(destElevationUnit, true),
                    onUnitClick = { picker = Picker.DestinationElevation },
                    modifier = Modifier
                        .focusRequester(destinationFocus)
                        .testTag("temp_est_dest_elevation")
                )
            }
            row { position ->
                TrailSenseInputField(
                    value = baseElevationText,
                    onValueChange = { baseElevationText = it },
                    label = labels.baseElevation,
                    position = position,
                    enabled = !isLoading,
                    unit = formatService.getDistanceUnitName(baseElevationUnit, true),
                    onUnitClick = { picker = Picker.BaseElevation },
                    supportingText = autoElevationNote.takeIf {
                        baseElevationText.isNotEmpty() && baseElevationText == autoElevationText
                    },
                    modifier = Modifier.testTag("temp_est_base_elevation")
                )
            }
        }

        // Humidity and wind change how it feels, not the air itself, so they wait behind one quiet row.
        item("more_conditions") {
            Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
                val turn by animateFloatAsState(
                    targetValue = if (showConditions) 180f else 0f,
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                    label = "conditions_chevron"
                )
                TrailSenseListItem(
                    title = stringResource(R.string.sky_temperature_more_conditions),
                    subtitle = stringResource(R.string.sky_temperature_more_conditions_summary),
                    icon = rememberVectorPainter(TrailIcons.Wind),
                    position = if (showConditions) {
                        TrailSenseGroupPosition(isFirst = true, isLast = false)
                    } else {
                        TrailSenseGroupPosition.Single
                    },
                    onClick = { showConditions = !showConditions },
                    trailing = {
                        Icon(
                            painter = rememberVectorPainter(TrailIcons.DropDownExpanded),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.rotate(turn)
                        )
                    },
                    modifier = Modifier.testTag("temp_est_more_conditions")
                )
                AnimatedVisibility(
                    visible = showConditions,
                    enter = expandVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                        fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                    exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                        fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec())
                ) {
                    TrailSenseRowGroup(footer = conditionsFooter) {
                        row { position ->
                            TrailSenseInputField(
                                value = humidityText,
                                onValueChange = { humidityText = it },
                                label = labels.humidity,
                                position = position.copy(isFirst = false),
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
                                position = position.copy(isFirst = false),
                                enabled = !isLoading,
                                unit = formatService.getSpeedUnitName(windSpeedUnit.distance, windSpeedUnit.time, true),
                                onUnitClick = { picker = Picker.WindSpeed },
                                modifier = Modifier.testTag("temp_est_wind_speed")
                            )
                        }
                    }
                }
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

private fun adviceFor(celsius: Float): Int = when {
    celsius < 0f -> R.string.sky_temperature_advice_freezing
    celsius < 10f -> R.string.sky_temperature_advice_cold
    celsius < 18f -> R.string.sky_temperature_advice_cool
    celsius < 30f -> R.string.sky_temperature_advice_warm
    else -> R.string.sky_temperature_advice_hot
}

// The slope the two places sit on, as fractions of the object: from the foot (lower left) to the summit.
private const val FOOT_X = 0.10f
private const val FOOT_Y = 0.86f
private const val PEAK_X = 0.68f
private const val PEAK_Y = 0.16f
private const val LOW_STOP = 0.06f
private const val SUMMIT_STOP = 0.88f

private fun slopePoint(fraction: Float, width: Float, height: Float): Offset = Offset(
    x = width * (FOOT_X + (PEAK_X - FOOT_X) * fraction),
    y = height * (FOOT_Y + (PEAK_Y - FOOT_Y) * fraction)
)

/**
 * The object: a calm mountain on a soft sky. You stand at its foot, the place you're going sits up (or down) the
 * slope by how far the climb is, a dashed trail joins the two, and each has its temperature in a small label. The
 * mountain's upper slopes cool (toward primaryContainer) as the climb makes it colder, and a snow cap settles on the
 * summit when the destination is below freezing. Before there is a destination, a faint flag waits at the top with
 * "Where to?"; tapping it goes to that field. Everything moves on springs.
 */
@Composable
private fun TemperatureMountain(
    hereTemperature: String?,
    hereElevation: String?,
    thereTemperature: String?,
    thereElevation: String?,
    climbMeters: Float?,
    changeCelsius: Float?,
    destinationCelsius: Float?,
    onAskDestination: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val hasDestination = thereElevation != null
    val reach = climbMeters?.let { (abs(it) / SUMMIT_CLIMB_METERS).coerceIn(0.12f, 1f) }
    val span = SUMMIT_STOP - LOW_STOP
    val hereTarget = if (climbMeters != null && climbMeters < 0f) LOW_STOP + span * (reach ?: 1f) else LOW_STOP
    val thereTarget = when {
        !hasDestination -> SUMMIT_STOP
        climbMeters == null -> SUMMIT_STOP
        climbMeters < 0f -> LOW_STOP
        else -> LOW_STOP + span * (reach ?: 1f)
    }
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val hereF by animateFloatAsState(hereTarget, spatial, label = "here")
    val thereF by animateFloatAsState(thereTarget, spatial, label = "there")

    // How much colder the destination is sets how cool the upper slopes look; a calm middle when it's not known yet.
    val coolness = changeCelsius?.let { (0.3f + (-it / 10f) * 0.7f).coerceIn(0f, 1f) } ?: 0.55f
    val cool by animateFloatAsState(coolness, MaterialTheme.motionScheme.slowEffectsSpec(), label = "cool")
    val snow by animateFloatAsState(
        if (destinationCelsius != null && destinationCelsius < 0f) 1f else 0f,
        MaterialTheme.motionScheme.slowEffectsSpec(),
        label = "snow"
    )
    val sky by animateColorAsState(scheme.surfaceContainerLow, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "sky")
    val range = scheme.surfaceContainerHigh
    val warmSlope = scheme.tertiaryContainer
    val coolSlope = scheme.primaryContainer
    val trail = scheme.primary
    val snowColor = scheme.surfaceBright
    val ring = scheme.surface

    // A destination still to choose breathes gently: the one idle motion, and it means "add me".
    val waiting = rememberInfiniteTransition(label = "waiting")
    val breath by waiting.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "breath"
    )
    val flagAlpha = if (hasDestination) 1f else breath

    val description = listOfNotNull(
        stringResource(R.string.sky_temperature_here_short),
        hereTemperature,
        hereElevation,
        stringResource(R.string.sky_temperature_there_short).takeIf { hasDestination },
        thereTemperature,
        thereElevation
    ).joinToString(", ")

    Box(
        modifier = Modifier
            .padding(top = TrailSenseSpacing.sm)
            .fillMaxWidth()
            .height(264.dp)
            .clip(RoundedCornerShape(TrailSenseShape.hero))
            .semantics(mergeDescendants = true) { contentDescription = description }
            .testTag("temp_est_mountain")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(sky)

            // A softer range behind, for depth.
            val back = Path().apply {
                moveTo(0f, h)
                lineTo(0f, h * 0.52f)
                lineTo(w * 0.2f, h * 0.36f)
                lineTo(w * 0.4f, h * 0.54f)
                lineTo(w * 0.56f, h * 0.44f)
                lineTo(w * 0.86f, h * 0.30f)
                lineTo(w, h * 0.40f)
                lineTo(w, h)
                close()
            }
            drawPath(back, range)

            // The mountain: a straight climbing face (the slope the places sit on), a rounded summit, a shoulder.
            val foot = Offset(w * FOOT_X, h * FOOT_Y)
            val peak = Offset(w * PEAK_X, h * PEAK_Y)
            val shoulder = Offset(w * 0.82f, h * 0.34f)
            val beforePeak = Offset(lerpF(foot.x, peak.x, 0.94f), lerpF(foot.y, peak.y, 0.94f))
            val afterPeak = Offset(lerpF(peak.x, shoulder.x, 0.18f), lerpF(peak.y, shoulder.y, 0.18f))
            val mountain = Path().apply {
                moveTo(0f, h)
                lineTo(0f, foot.y + h * 0.04f)
                lineTo(foot.x, foot.y)
                lineTo(beforePeak.x, beforePeak.y)
                quadraticTo(peak.x, peak.y, afterPeak.x, afterPeak.y)
                lineTo(shoulder.x, shoulder.y)
                quadraticTo(w * 0.86f, h * 0.38f, w * 0.9f, h * 0.42f)
                lineTo(w, h * 0.54f)
                lineTo(w, h)
                close()
            }
            drawPath(
                mountain,
                Brush.verticalGradient(
                    0f to lerp(warmSlope, coolSlope, cool),
                    1f to lerp(warmSlope, coolSlope, cool * 0.25f),
                    startY = peak.y,
                    endY = h
                )
            )

            // The snow cap, only below freezing.
            if (snow > 0f) {
                clipPath(mountain) {
                    val capLeft = Offset(lerpF(foot.x, peak.x, 0.8f), lerpF(foot.y, peak.y, 0.8f))
                    val capRight = Offset(lerpF(peak.x, shoulder.x, 0.7f), lerpF(peak.y, shoulder.y, 0.7f))
                    val dip = h * 0.05f
                    val cap = Path().apply {
                        moveTo(capLeft.x, capLeft.y)
                        lineTo(peak.x, peak.y - h * 0.02f)
                        lineTo(capRight.x, capRight.y)
                        lineTo(lerpF(capLeft.x, capRight.x, 0.72f), capRight.y + dip * 0.4f)
                        lineTo(lerpF(capLeft.x, capRight.x, 0.5f), capLeft.y - dip * 0.2f)
                        lineTo(lerpF(capLeft.x, capRight.x, 0.28f), capLeft.y + dip * 0.5f)
                        close()
                    }
                    drawPath(cap, snowColor.copy(alpha = 0.9f * snow))
                }
            }

            // The trail between the two places.
            val here = slopePoint(hereF, w, h)
            val there = slopePoint(thereF, w, h)
            drawLine(
                color = trail.copy(alpha = if (hasDestination) 0.9f else 0.35f),
                start = here,
                end = there,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 9.dp.toPx()))
            )

            // You: a dot with a ring of the surface around it.
            drawCircle(ring, radius = 10.dp.toPx(), center = here)
            drawCircle(trail, radius = 7.dp.toPx(), center = here)

            // Where you're going: a small flag, faint and breathing until it's chosen.
            val poleTop = Offset(there.x, there.y - 30.dp.toPx())
            drawCircle(ring.copy(alpha = flagAlpha), radius = 6.dp.toPx(), center = there)
            drawCircle(trail.copy(alpha = flagAlpha), radius = 4.dp.toPx(), center = there)
            drawLine(
                trail.copy(alpha = flagAlpha),
                start = there,
                end = poleTop,
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
            val flag = Path().apply {
                moveTo(poleTop.x, poleTop.y)
                lineTo(poleTop.x + 18.dp.toPx(), poleTop.y + 6.dp.toPx())
                lineTo(poleTop.x, poleTop.y + 12.dp.toPx())
                close()
            }
            if (hasDestination) {
                drawPath(flag, trail)
            } else {
                drawPath(flag, trail.copy(alpha = flagAlpha), style = Stroke(width = 2.dp.toPx()))
            }
        }

        // The labels sit beside their places: the lower one above its dot, the higher one on the sky side.
        Layout(
            content = {
                PlaceLabel(
                    title = hereTemperature ?: stringResource(R.string.sky_temperature_here_short),
                    subtitle = hereElevation
                )
                if (hasDestination) {
                    PlaceLabel(
                        title = thereTemperature ?: thereElevation.orEmpty(),
                        subtitle = thereElevation.takeIf { thereTemperature != null },
                        emphasised = thereTemperature != null
                    )
                } else {
                    PlaceLabel(
                        title = stringResource(R.string.sky_temperature_where_to),
                        subtitle = null,
                        onClick = onAskDestination
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { measurables, constraints ->
            val loose = constraints.copy(minWidth = 0, minHeight = 0)
            val placeables = measurables.map { it.measure(loose) }
            layout(constraints.maxWidth, constraints.maxHeight) {
                val w = constraints.maxWidth.toFloat()
                val h = constraints.maxHeight.toFloat()
                val gap = 14.dp.roundToPx()
                val inset = 12.dp.roundToPx()
                fun clampX(x: Int, width: Int) = x.coerceIn(inset, (constraints.maxWidth - width - inset).coerceAtLeast(inset))
                fun clampY(y: Int, height: Int) = y.coerceIn(inset, (constraints.maxHeight - height - inset).coerceAtLeast(inset))
                val here = slopePoint(hereF, w, h)
                val there = slopePoint(thereF, w, h)
                val hereIsHigher = hereF > thereF
                listOf(here to placeables[0], there to placeables[1]).forEachIndexed { index, (point, p) ->
                    val isHigher = if (index == 0) hereIsHigher else !hereIsHigher
                    if (isHigher) {
                        // On the sky side, left of the place.
                        p.place(
                            clampX(point.x.roundToInt() - p.width - gap, p.width),
                            clampY(point.y.roundToInt() - p.height / 2 - gap, p.height)
                        )
                    } else {
                        p.place(
                            clampX(point.x.roundToInt() - p.width / 3, p.width),
                            clampY(point.y.roundToInt() - p.height - gap, p.height)
                        )
                    }
                }
            }
        }
    }
}

/** A place's label on the mountain: its temperature (or name) over its elevation, in a small surface pill. */
@Composable
private fun PlaceLabel(
    title: String,
    subtitle: String?,
    emphasised: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(horizontal = TrailSenseSpacing.md, vertical = TrailSenseSpacing.xs + 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
                color = if (emphasised) scheme.primary else scheme.onSurface,
                maxLines = 1
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                    color = scheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
    val shape = RoundedCornerShape(TrailSenseShape.iconContainer)
    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = scheme.surface,
            border = BorderStroke(1.dp, scheme.outlineVariant),
            content = content
        )
    } else {
        Surface(shape = shape, color = scheme.surface, content = content)
    }
}

private fun lerpF(a: Float, b: Float, t: Float): Float = a + (b - a) * t

/** The family's 40 dp Cookie6 well, as the button that reads the phone's thermometer. */
@Composable
private fun TemperatureWell(
    isLoading: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TrailSenseRowWell(
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
