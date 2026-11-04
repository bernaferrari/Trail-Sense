@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.light.ui

import android.graphics.Paint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseStat
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseStatGrid
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGroupShape
import com.kylecorry.trail_sense.shared.views.compose.trailSenseToneColors
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpotlight
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpotlightAction
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import kotlin.math.log10
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.sol.science.optics.Optics
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.TextButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseMetricItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseMetricRow
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseOptionsDialog
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseReading
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAnimation
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LightMeterScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formatter = remember { FormatService.getInstance(context) }
    val lightSensor = remember { SensorService(context).getLightSensor() }
    val distanceUnits = remember {
        formatter.sortDistanceUnits(DistanceUtils.hikingDistanceUnits)
    }
    val defaultUnit = remember { distanceUnits.firstOrNull() ?: DistanceUnits.Meters }
    var illuminance by remember { mutableStateOf(lightSensor.illuminance) }
    var maxLux by remember { mutableStateOf(0f) }
    var beamDistanceUnit by remember { mutableStateOf(defaultUnit) }
    var beamDistanceAmount by remember { mutableStateOf("") }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, lightSensor) {
        fun onReading(): Boolean {
            illuminance = lightSensor.illuminance
            maxLux = max(maxLux, illuminance)
            return true
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> lightSensor.start(::onReading)
                Lifecycle.Event.ON_PAUSE -> lightSensor.stop(::onReading)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        lightSensor.start(::onReading)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            lightSensor.stop(::onReading)
        }
    }

    val beamDistance = beamDistanceAmount.toFloatOrNull()?.let {
        Distance.from(it, beamDistanceUnit)
    }
    val candela = beamDistance?.let {
        Optics.luxToCandela(maxLux, it)
    } ?: 0f


    var showUnitPicker by remember { mutableStateOf(false) }
    val luxText = formatter.formatLux(illuminance)
    // One question: how bright is it here?
    // One live object, a ring that fills with the light, the reading inside it and the verdict under it. Testing a
    // flashlight is its own section under it, in the order you do it: the distance, the brightest reading, then what
    // that means (intensity, reach) and the scene it lights.
    TrailSensePage(
        title = stringResource(R.string.tool_light_meter_title),
        modifier = modifier
    ) {
        item("hero") {
            LightHero(
                luxText = luxText,
                illuminance = illuminance,
                modifier = Modifier.testTag("light_title")
            )
        }

        trailSenseSection(
            "flashlight",
            title = context.getString(R.string.light_meter_flashlight_title),
            footer = context.getString(R.string.light_meter_preview_hint)
        ) {
            row { position ->
                TrailSenseInputField(
                    value = beamDistanceAmount,
                    onValueChange = {
                        beamDistanceAmount = it
                        maxLux = 0f
                    },
                    label = stringResource(R.string.distance),
                    modifier = Modifier.testTag("beam_distance"),
                    position = position,
                    unit = formatter.getDistanceUnitName(beamDistanceUnit, true),
                    onUnitClick = { showUnitPicker = true }
                )
            }
            if (maxLux > 0f) {
                row { position ->
                    LightValueRow(
                        label = stringResource(R.string.light_meter_peak_label),
                        value = formatter.formatLux(maxLux),
                        supportingText = stringResource(R.string.light_meter_tap_to_reset),
                        position = position,
                        onClick = { maxLux = 0f },
                        modifier = Modifier.testTag("light_peak")
                    )
                }
            }
            if (beamDistance != null) {
                row { position ->
                    LightValueRow(
                        label = stringResource(R.string.light_meter_intensity),
                        value = formatter.formatCandela(candela),
                        position = position
                    )
                }
                row { position ->
                    LightValueRow(
                        label = stringResource(R.string.light_meter_reach),
                        value = formatter.formatDistance(Optics.lightBeamDistance(candela).convertTo(beamDistance.units)),
                        position = position
                    )
                }
                row { position ->
                    Surface(
                        shape = trailSenseGroupShape(position),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        LightBeamChart(
                            candela = candela,
                            units = beamDistanceUnit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(152.dp)
                                .testTag("light_chart")
                        )
                    }
                }
            }
        }
    }

    if (showUnitPicker) {
        TrailSenseOptionsDialog(
            title = stringResource(R.string.distance),
            options = distanceUnits.map { formatter.getDistanceUnitName(it, false) to it },
            selected = beamDistanceUnit,
            onSelect = {
                beamDistanceUnit = it
                maxLux = 0f
                showUnitPicker = false
            },
            onDismiss = { showUnitPicker = false }
        )
    }
}

/** Where a reading sits on the worded scale: Dark, Dim, Indoor, Daylight as four equal bands, log inside each. */
private fun lightScalePosition(lux: Float): Float {
    val bounds = floatArrayOf(0f, 1f, 50f, 1000f, 100_000f)
    val value = lux.coerceIn(0f, 100_000f)
    for (i in 0 until 4) {
        if (value < bounds[i + 1] || i == 3) {
            val lo = log10(bounds[i] + 1f)
            val hi = log10(bounds[i + 1] + 1f)
            val frac = ((log10(value + 1f) - lo) / (hi - lo)).coerceIn(0f, 1f)
            return (i + frac) / 4f
        }
    }
    return 1f
}

/**
 * The instrument: a thick ring that fills along the worded light scale (dark, dim, indoor, daylight), with the
 * reading inside it and the verdict as the headline under it.
 */
@Composable
private fun LightHero(
    luxText: String,
    illuminance: Float,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val position = lightScalePosition(illuminance)
    val animatedPosition by animateFloatAsState(position, MaterialTheme.motionScheme.slowSpatialSpec(), label = "lux")
    val verdict = stringResource(
        when {
            illuminance < 1f -> R.string.light_verdict_dark
            illuminance < 50f -> R.string.light_verdict_dim
            illuminance < 1000f -> R.string.light_verdict_indoor
            illuminance < 10_000f -> R.string.light_verdict_overcast
            illuminance < 50_000f -> R.string.light_verdict_daylight
            else -> R.string.light_verdict_sun
        }
    )
    // "Pitch dark · your eyes need..." reads as a headline and its explanation.
    val headline = verdict.substringBefore(" · ")
    val detail = verdict.substringAfter(" · ", "").replaceFirstChar { it.uppercase() }
    val track = scheme.surfaceContainerHighest
    val fill = scheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = TrailSenseSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
    ) {
        // The arc is open at the bottom, so its box is shorter than the circle: the verdict sits right under it.
        Box(
            modifier = Modifier
                .width(260.dp)
                .height(228.dp),
            contentAlignment = Alignment.TopCenter
        ) {
          Box(
            modifier = Modifier
                .requiredSize(260.dp),
            contentAlignment = Alignment.Center
          ) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 22.dp.toPx()
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(
                    color = track,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
                // At rest the fill is a short primary stub, so the instrument never reads as empty.
                drawArc(
                    color = fill,
                    startAngle = 135f,
                    sweepAngle = (270f * animatedPosition).coerceAtLeast(10f),
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
            Text(
                text = luxText,
                style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
          }
        }
        Text(
            text = headline,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        if (detail.isNotEmpty()) {
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** A quiet value row: its label on the left, the value on the right. */
@Composable
private fun LightValueRow(
    label: String,
    value: String,
    position: TrailSenseGroupPosition,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    onClick: (() -> Unit)? = null
) {
    com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem(
        title = label,
        subtitle = supportingText,
        position = position,
        onClick = onClick,
        modifier = modifier,
        trailing = {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold
            )
        }
    )
}

@Composable
private fun LightBeamChart(
    candela: Float,
    units: DistanceUnits,
    modifier: Modifier = Modifier
) {
    // A thin strip in the app's own colours, on the group row itself (no panel of its own): the flashlight shines
    // from the left edge across a row of trees, each lit by the light that actually reaches it.
    val scheme = MaterialTheme.colorScheme
    val animatedCandela by animateFloatAsState(candela, MaterialTheme.motionScheme.slowSpatialSpec(), label = "beam")
    val glow = scheme.tertiary
    val treeDark = scheme.primary.copy(alpha = 0.35f)
    val treeLit = scheme.tertiary
    val labelColor = scheme.onSurfaceVariant
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = if (units == DistanceUnits.Feet) 3 else 1
            val fullMoon = android.hardware.SensorManager.LIGHT_FULLMOON
            val sunrise = android.hardware.SensorManager.LIGHT_SUNRISE
            val minLux = ln(fullMoon - 0.05f)
            val maxLuxLn = ln(sunrise)
            fun brightness(index: Int): Float {
                if (animatedCandela <= 0.01f) return 0f
                val lux = Optics.luxAtDistance(animatedCandela, Distance.from(((index + 1) * step).toFloat(), units))
                return ((ln(lux) - minLux) / (maxLuxLn - minLux)).coerceIn(0f, 1f)
            }
            val groundTop = size.height - 24.dp.toPx()
            val columns = 100
            val colWidth = size.width / columns
            for (i in 0 until columns) {
                val b = brightness(i)
                if (b <= 0f) continue
                val spread = (i + 1) / columns.toFloat()
                val halfHeight = (groundTop * 0.16f) + groundTop * 0.42f * spread
                val center = groundTop * 0.6f
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.Transparent, glow.copy(alpha = b * 0.45f), Color.Transparent),
                        startY = center - halfHeight,
                        endY = center + halfHeight
                    ),
                    topLeft = Offset(i * colWidth, center - halfHeight),
                    size = Size(colWidth + 1f, halfHeight * 2)
                )
                drawRect(
                    color = glow.copy(alpha = b * 0.8f),
                    topLeft = Offset(i * colWidth, groundTop),
                    size = Size(colWidth + 1f, 4.dp.toPx())
                )
            }
            drawRect(
                color = scheme.primary.copy(alpha = 0.12f),
                topLeft = Offset(0f, groundTop),
                size = Size(size.width, size.height - groundTop)
            )
            val paint = Paint().apply {
                color = labelColor.toArgb()
                textAlign = Paint.Align.CENTER
                textSize = 13.dp.toPx()
                isFakeBoldText = true
                isAntiAlias = true
            }
            listOf(9, 24, 49, 74).forEach { index ->
                val center = (index + 0.5f) * colWidth
                val b = brightness(index)
                val tree = lerp(treeDark, treeLit, b)
                drawTree(this, center, groundTop, 44.dp.toPx(), tree)
                drawIntoCanvas {
                    it.nativeCanvas.drawText(((index + 1) * step).toString(), center, size.height - 7.dp.toPx(), paint)
                }
            }
        }
    }
}

private fun drawTree(
    scope: androidx.compose.ui.graphics.drawscope.DrawScope,
    centerX: Float,
    bottom: Float,
    height: Float,
    color: Color
) = with(scope) {
    val trunk = height * 0.12f
    drawRect(color, Offset(centerX - trunk / 2f, bottom - height * 0.22f), Size(trunk, height * 0.22f))
    listOf(0f to 1f, 0.28f to 0.8f, 0.52f to 0.6f).forEach { (lift, width) ->
        val base = bottom - height * 0.18f - height * lift
        val path = Path().apply {
            moveTo(centerX, base - height * 0.5f)
            lineTo(centerX - height * 0.34f * width, base)
            lineTo(centerX + height * 0.34f * width, base)
            close()
        }
        drawPath(path, color)
    }
}
