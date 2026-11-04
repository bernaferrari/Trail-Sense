@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.ruler.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.system.Screen
import com.kylecorry.sol.math.arithmetic.Arithmetic
import com.kylecorry.sol.science.geography.Geography
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseConnectedChoice
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpotlight
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInstrumentScaffold
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseOptionsDialog
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseReading
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.safeRoundToInt
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RulerScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formatService = remember { FormatService.getInstance(context) }
    val prefs = remember { UserPreferences(context) }
    val yDpi = remember { Screen.ydpi(context) }
    val rulerScale = remember { prefs.ruler.rulerScale }
    val isRulerFlipped = remember { prefs.ruler.rulerFlipped }
    var rulerUnits by remember {
        mutableStateOf(
            if (prefs.distanceUnits == UserPreferences.DistanceUnits.Meters) {
                DistanceUnits.Centimeters
            } else {
                DistanceUnits.Inches
            }
        )
    }
    var selectedDistance by remember { mutableStateOf<Distance?>(null) }
    var scaleMode by remember { mutableStateOf(MapScaleMode.Fractional) }
    var fractionalFrom by remember { mutableStateOf("1") }
    var fractionalTo by remember { mutableStateOf("") }
    var verbalFrom by remember { mutableStateOf<Distance?>(null) }
    var verbalTo by remember { mutableStateOf<Distance?>(null) }

    val currentDistance = selectedDistance?.convertTo(DistanceUnits.Centimeters)
    val measurement = currentDistance?.convertTo(rulerUnits)?.let {
        formatService.formatDistance(it, PRECISION, false)
    }.orEmpty()
    val mapDistance = remember(
        currentDistance,
        rulerUnits,
        scaleMode,
        fractionalFrom,
        fractionalTo,
        verbalFrom,
        verbalTo
    ) {
        calculateMapDistance(
            currentDistance,
            rulerUnits,
            scaleMode,
            fractionalFrom,
            fractionalTo,
            verbalFrom,
            verbalTo,
            formatService
        )?.let {
            context.getString(R.string.map_distance, it)
        }.orEmpty()
    }

    val tapHint = stringResource(R.string.ruler_tap_hint)
    val cmLabel = getUnitText(DistanceUnits.Centimeters)
    val inLabel = getUnitText(DistanceUnits.Inches)
    val ratioLabel = stringResource(R.string.map_scale_ratio)
    val verbalLabel = stringResource(R.string.map_scale_verbal)

    // The ruler runs down the screen's edge; the measurement is the hero beside it, then the units, then the map
    // scale that turns the measurement into a distance on the ground.
    TrailSenseInstrumentScaffold(
        title = stringResource(R.string.tool_ruler_title),
        modifier = modifier
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            RulerCanvas(
                rulerUnits = rulerUnits,
                highlight = selectedDistance?.convertTo(rulerUnits),
                yDpi = yDpi,
                scale = rulerScale,
                flipped = isRulerFlipped,
                onDistanceSelected = {
                    selectedDistance = it.convertTo(DistanceUnits.Centimeters)
                },
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxHeight()
                    .testTag("ruler")
                    .semantics { contentDescription = measurement }
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = TrailSenseSize.maximumContentWidth)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = TrailSenseSpacing.lg,
                        end = TrailSenseSpacing.lg,
                        top = TrailSenseSpacing.lg,
                        bottom = TrailSenseSpacing.xxxl
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xl)
            ) {
                TrailSenseSpotlight(
                    eyebrow = stringResource(R.string.ruler_length),
                    title = measurement.ifEmpty { tapHint },
                    titleStyle = if (measurement.isEmpty()) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.displaySmall
                    },
                    supportingText = mapDistance.takeIf { it.isNotEmpty() },

                    modifier = Modifier.testTag("measurement")
                )

                TrailSenseConnectedChoice(
                    options = listOf(
                        TrailSenseChoice(DistanceUnits.Centimeters, cmLabel),
                        TrailSenseChoice(DistanceUnits.Inches, inLabel)
                    ),
                    selected = rulerUnits,
                    onSelect = { rulerUnits = it },
                    modifier = Modifier.testTag("ruler_unit_btn")
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    TrailSenseGroupHeader(
                        stringResource(R.string.map_scale_title),
                        modifier = Modifier.testTag("map_scale_title")
                    )
                    TrailSenseConnectedChoice(
                        options = listOf(
                            TrailSenseChoice(MapScaleMode.Fractional, ratioLabel),
                            TrailSenseChoice(MapScaleMode.Relational, verbalLabel)
                        ),
                        selected = scaleMode,
                        onSelect = { scaleMode = it },
                        modifier = Modifier
                            .padding(bottom = TrailSenseSpacing.md)
                            .testTag(if (scaleMode == MapScaleMode.Fractional) "map_ratio_btn" else "map_verbal_btn")
                    )
                    val enterSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
                    val exitSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
                    AnimatedContent(
                        targetState = scaleMode,
                        transitionSpec = { fadeIn(enterSpec) togetherWith fadeOut(exitSpec) },
                        label = "mapScaleMode"
                    ) { mode ->
                        if (mode == MapScaleMode.Fractional) {
                            FractionalScaleInput(
                                from = fractionalFrom,
                                to = fractionalTo,
                                onFromChanged = { fractionalFrom = it },
                                onToChanged = { fractionalTo = it },
                                modifier = Modifier.testTag("fractional_map_scale")
                            )
                        } else {
                            VerbalScaleInput(
                                from = verbalFrom,
                                to = verbalTo,
                                onFromChanged = { verbalFrom = it },
                                onToChanged = { verbalTo = it },
                                formatService = formatService,
                                modifier = Modifier.testTag("verbal_map_scale")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun RulerCanvas(
    rulerUnits: DistanceUnits,
    highlight: Distance?,
    yDpi: Float,
    scale: Float,
    flipped: Boolean,
    onDistanceSelected: (Distance) -> Unit,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.onPrimaryContainer
    val highlightColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.primaryContainer
    Canvas(
        modifier = modifier
            .background(surfaceColor)
            .pointerInput(rulerUnits, yDpi, scale, flipped) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    onDistanceSelected(
                        getRulerDistance(
                            down.position.y,
                            size.height.toFloat(),
                            rulerUnits,
                            yDpi,
                            scale,
                            flipped,
                            8.dp.toPx()
                        )
                    )
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) {
                            break
                        }
                        if (change.positionChange() != Offset.Zero) {
                            onDistanceSelected(
                                getRulerDistance(
                                    change.position.y,
                                    size.height.toFloat(),
                                    rulerUnits,
                                    yDpi,
                                    scale,
                                    flipped,
                                    8.dp.toPx()
                                )
                            )
                            change.consume()
                        }
                    }
                }
            }
    ) {
        val offset = 8.dp.toPx()
        val rulerHeight = getRulerHeight(size.height, rulerUnits, yDpi, scale, offset)
        val lineThickness = 1.dp.toPx()
        val highlightLineThickness = 2.dp.toPx()
        val wholeSize = 40.dp.toPx()
        val halfSize = 24.dp.toPx()
        val quarterSize = 12.dp.toPx()
        val eighthSize = 6.dp.toPx()
        val tenthSize = 12.dp.toPx()

        highlight?.let {
            val zeroPosition = getRulerPosition(
                Distance.from(0f, rulerHeight.units),
                size.height,
                rulerHeight,
                flipped,
                offset
            )
            val highlightPosition = getRulerPosition(it, size.height, rulerHeight, flipped, offset)
            val top = minOf(zeroPosition, highlightPosition)
            val bottom = maxOf(zeroPosition, highlightPosition)
            drawRect(
                color = highlightColor.copy(alpha = 0.2f),
                topLeft = Offset(0f, top),
                size = Size(size.width, bottom - top)
            )
        }

        if (rulerUnits.isMetric) {
            drawRulerLines(
                rulerHeight = rulerHeight,
                spacing = Distance.from(0.1f, rulerHeight.units),
                lineColor = lineColor,
                lineThickness = lineThickness,
                flipped = flipped,
                offset = offset,
                widthFn = {
                    if (approxDivisibleBy(it.value, 1f)) {
                        wholeSize
                    } else if (approxDivisibleBy(it.value, 0.5f)) {
                        halfSize
                    } else {
                        tenthSize
                    }
                }
            )
        } else {
            drawRulerLines(
                rulerHeight = rulerHeight,
                spacing = Distance.from(0.125f, rulerHeight.units),
                lineColor = lineColor,
                lineThickness = lineThickness,
                flipped = flipped,
                offset = offset,
                widthFn = {
                    when {
                        it.value % 1f == 0f -> wholeSize
                        it.value % 0.5f == 0f -> halfSize
                        it.value % 0.25f == 0f -> quarterSize
                        it.value % 0.125f == 0f -> eighthSize
                        else -> tenthSize
                    }
                }
            )
        }

        drawRulerLabels(
            rulerHeight = rulerHeight,
            spacing = Distance.from(1f, rulerHeight.units),
            lineColor = lineColor,
            flipped = flipped,
            offset = offset,
            lineWidth = wholeSize
        )

        highlight?.let {
            val y = getRulerPosition(it, size.height, rulerHeight, flipped, offset)
            drawLine(
                color = highlightColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = highlightLineThickness,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRulerLines(
    rulerHeight: Distance,
    spacing: Distance,
    lineColor: Color,
    lineThickness: Float,
    flipped: Boolean,
    offset: Float,
    widthFn: (Distance) -> Float
) {
    val convertedSpacing = spacing.convertTo(rulerHeight.units)
    var position = Distance.from(0f, rulerHeight.units)
    while (position < rulerHeight) {
        val y = getRulerPosition(position, size.height, rulerHeight, flipped, offset)
        drawLine(
            color = lineColor,
            start = Offset(0f, y),
            end = Offset(widthFn(position), y),
            strokeWidth = lineThickness
        )
        position = Distance.from(position.value + convertedSpacing.value, position.units)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRulerLabels(
    rulerHeight: Distance,
    spacing: Distance,
    lineColor: Color,
    flipped: Boolean,
    offset: Float,
    lineWidth: Float
) {
    val convertedSpacing = spacing.convertTo(rulerHeight.units)
    val paint = Paint().apply {
        color = android.graphics.Color.argb(
            (lineColor.alpha * 255).toInt(),
            (lineColor.red * 255).toInt(),
            (lineColor.green * 255).toInt(),
            (lineColor.blue * 255).toInt()
        )
        textSize = 12.dp.toPx()
    }
    var position = Distance.from(0f, rulerHeight.units)
    while (position < rulerHeight) {
        val label = position.value.toInt().toString()
        val y = getRulerPosition(position, size.height, rulerHeight, flipped, offset)
        drawIntoCanvas {
            it.nativeCanvas.drawText(label, lineWidth + 8.dp.toPx(), y + paint.textSize / 2f, paint)
        }
        position = Distance.from(position.value + convertedSpacing.value, position.units)
    }
}

/** "1 : 24000" as one group: the two numbers of the ratio, the separator between them in the label. */
@Composable
private fun FractionalScaleInput(
    from: String,
    to: String,
    onFromChanged: (String) -> Unit,
    onToChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // "1 : 25000" on one line, the way a map prints its scale.
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
    ) {
        TrailSenseInputField(
            value = from,
            onValueChange = onFromChanged,
            label = stringResource(R.string.distance_from),
            position = TrailSenseGroupPosition.Single,
            modifier = Modifier
                .weight(1f)
                .testTag("fractional_map_from")
        )
        Text(
            text = stringResource(R.string.map_fractional_separator),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(horizontal = TrailSenseSpacing.xs)
                .testTag("textView12")
        )
        TrailSenseInputField(
            value = to,
            onValueChange = onToChanged,
            label = stringResource(R.string.distance_to),
            position = TrailSenseGroupPosition.Single,
            modifier = Modifier
                .weight(2f)
                .testTag("fractional_map_to")
        )
    }
}

/** "1 cm = 1 km" as one group: each side a number with its unit picked from the field. */
@Composable
private fun VerbalScaleInput(
    from: Distance?,
    to: Distance?,
    onFromChanged: (Distance?) -> Unit,
    onToChanged: (Distance?) -> Unit,
    formatService: FormatService,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
    ) {
        DistanceAmountInput(
            label = stringResource(R.string.distance_from),
            value = from,
            units = formatService.sortDistanceUnits(DistanceUtils.rulerDistanceUnits),
            defaultUnit = DistanceUnits.Centimeters,
            onValueChange = onFromChanged,
            position = TrailSenseGroupPosition(isFirst = true, isLast = false),
            modifier = Modifier.testTag("verbal_map_scale_from")
        )
        DistanceAmountInput(
            label = stringResource(R.string.distance_to),
            value = to,
            units = formatService.sortDistanceUnits(DistanceUtils.hikingDistanceUnits),
            defaultUnit = DistanceUnits.Meters,
            onValueChange = onToChanged,
            position = TrailSenseGroupPosition(isFirst = false, isLast = true),
            modifier = Modifier.testTag("verbal_map_scale_to")
        )
    }
}

@Composable
private fun DistanceAmountInput(
    label: String,
    value: Distance?,
    units: List<DistanceUnits>,
    defaultUnit: DistanceUnits,
    onValueChange: (Distance?) -> Unit,
    position: TrailSenseGroupPosition,
    modifier: Modifier = Modifier
) {
    val formatService = FormatService.getInstance(LocalContext.current)
    var amount by remember {
        mutableStateOf(value?.let { DecimalFormatter.format(it.value, 5, false) }.orEmpty())
    }
    var unit by remember { mutableStateOf(value?.units ?: defaultUnit) }
    var isPickingUnit by remember { mutableStateOf(false) }

    TrailSenseInputField(
        value = amount,
        onValueChange = {
            amount = it
            onValueChange(it.toFloatOrNull()?.let { amountValue -> Distance.from(amountValue, unit) })
        },
        label = label,
        position = position,
        unit = formatService.getDistanceUnitName(unit, true),
        onUnitClick = { isPickingUnit = true },
        modifier = modifier
    )

    if (isPickingUnit) {
        TrailSenseOptionsDialog(
            title = stringResource(R.string.units),
            options = units.map { formatService.getDistanceUnitName(it) to it },
            selected = unit,
            onSelect = {
                unit = it
                isPickingUnit = false
                onValueChange(amount.toFloatOrNull()?.let { amountValue -> Distance.from(amountValue, it) })
            },
            onDismiss = { isPickingUnit = false }
        )
    }
}

private fun calculateMapDistance(
    currentDistance: Distance?,
    rulerUnits: DistanceUnits,
    scaleMode: MapScaleMode,
    fractionalFrom: String,
    fractionalTo: String,
    verbalFrom: Distance?,
    verbalTo: Distance?,
    formatService: FormatService
): String? {
    currentDistance ?: return null
    return when (scaleMode) {
        MapScaleMode.Relational -> {
            if (verbalFrom == null || verbalTo == null) {
                null
            } else {
                val mapDistance = Geography.getMapDistance(currentDistance, verbalFrom, verbalTo)
                formatService.formatDistance(
                    Distance.from(mapDistance.value, verbalTo.units),
                    MAP_PRECISION,
                    false
                )
            }
        }

        MapScaleMode.Fractional -> {
            val ratioFrom = fractionalFrom.toFloatOrNull()
            val ratioTo = fractionalTo.toFloatOrNull()
            if (ratioFrom == null || ratioTo == null) {
                null
            } else {
                val mapDistance = Geography.getMapDistance(currentDistance, ratioFrom, ratioTo)
                formatService.formatDistance(
                    mapDistance.convertTo(rulerUnits).toRelativeDistance(),
                    MAP_PRECISION,
                    false
                )
            }
        }
    }
}

private fun getRulerHeight(
    height: Float,
    units: DistanceUnits,
    yDpi: Float,
    scale: Float,
    offset: Float
): Distance {
    val actualHeight = height - offset
    val heightIn = scale * actualHeight / yDpi
    return Distance.from(heightIn, DistanceUnits.Inches)
        .convertTo(if (units.isMetric) DistanceUnits.Centimeters else DistanceUnits.Inches)
}

private fun getRulerPosition(
    distance: Distance,
    height: Float,
    rulerHeight: Distance,
    flipped: Boolean,
    offset: Float
): Float {
    val usableHeight = height - offset
    val d = distance.convertTo(rulerHeight.units).value
    val position = d / rulerHeight.value * usableHeight
    return if (flipped) {
        height - offset - position
    } else {
        offset + position
    }
}

private fun getRulerDistance(
    y: Float,
    height: Float,
    units: DistanceUnits,
    yDpi: Float,
    scale: Float,
    flipped: Boolean,
    offset: Float
): Distance {
    val rulerHeight = getRulerHeight(height, units, yDpi, scale, offset)
    val usableHeight = height - offset
    val distance = if (flipped) {
        (height - offset - y) / usableHeight * rulerHeight.value
    } else {
        (y - offset) / usableHeight * rulerHeight.value
    }.coerceIn(0f, rulerHeight.value)
    return Distance.from(distance, rulerHeight.units)
}

@Composable
private fun getUnitText(units: DistanceUnits): String {
    return stringResource(
        if (units == DistanceUnits.Centimeters) {
            R.string.unit_centimeters_abbreviation
        } else {
            R.string.unit_inches_abbreviation
        }
    )
}

private fun approxDivisibleBy(value: Float, divisor: Float): Boolean {
    val rounded = (value / divisor).safeRoundToInt() * divisor
    return Arithmetic.isCloseTo(rounded, value, 0.05f)
}

private enum class MapScaleMode {
    Fractional,
    Relational
}

private const val PRECISION = 4
private const val MAP_PRECISION = 2
