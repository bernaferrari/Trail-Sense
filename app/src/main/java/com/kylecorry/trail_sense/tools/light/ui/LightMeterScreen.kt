@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.light.ui

import android.graphics.Paint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
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
    val distanceLabel = stringResource(R.string.distance_from_flashlight)
    val previewLabel = stringResource(R.string.preview)

    // The live illuminance is the hero; once a distance is entered the flashlight's strength and reach follow it
    // as a metric strip, then the beam preview, then the one input that drives them.
    TrailSensePage(
        title = stringResource(R.string.tool_light_meter_title),
        modifier = modifier
    ) {
        item("hero") {
            LightHero(
                luxText = luxText,
                illuminance = illuminance,
                peakText = if (maxLux > 0f) stringResource(R.string.light_meter_peak, formatter.formatLux(maxLux)) else null,
                onResetPeak = { maxLux = 0f },
                modifier = Modifier.testTag("light_title")
            )
        }

        item("metrics") {
            AnimatedVisibility(
                visible = beamDistance != null,
                enter = expandVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                    fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                    fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec())
            ) {
                TrailSenseMetricRow(
                    modifier = Modifier.padding(top = TrailSenseSpacing.sm),
                    items = listOf(
                        TrailSenseMetricItem(
                            label = stringResource(R.string.light_meter_intensity),
                            value = formatter.formatCandela(candela)
                        ),
                        TrailSenseMetricItem(
                            label = stringResource(R.string.light_meter_reach),
                            value = beamDistance?.let {
                                formatter.formatDistance(Optics.lightBeamDistance(candela).convertTo(it.units))
                            } ?: "--"
                        )
                    )
                )
            }
        }

        trailSenseSection("distance", title = distanceLabel) {
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
        }

        trailSenseSection("preview", title = previewLabel) {
            row { position ->
                Box {
                    LightBeamChart(
                        candela = candela,
                        units = beamDistanceUnit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(168.dp)
                            .testTag("light_chart")
                    )
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LightHero(
    luxText: String,
    illuminance: Float,
    peakText: String?,
    onResetPeak: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    // Light spans eight orders of magnitude, so the meter reads it on a log scale: a candle to the midday sun.
    val level = (log10(illuminance.coerceAtLeast(0f) + 1f) / 5f).coerceIn(0f, 1f)
    val animatedLevel by animateFloatAsState(level, MaterialTheme.motionScheme.slowSpatialSpec(), label = "lux")
    val description = stringResource(
        when {
            illuminance < 1f -> R.string.light_level_dark
            illuminance < 50f -> R.string.light_level_dim
            illuminance < 1000f -> R.string.light_level_indoor
            illuminance < 10_000f -> R.string.light_level_overcast
            illuminance < 50_000f -> R.string.light_level_daylight
            else -> R.string.light_level_sun
        }
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = scheme.primaryContainer,
        contentColor = scheme.onPrimaryContainer
    ) {
        Column(
            modifier = Modifier.padding(TrailSenseSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.light_meter_illuminance),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onPrimaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f)
                )
                Text(description, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            Text(
                text = luxText,
                style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold
            )
            LinearWavyProgressIndicator(
                progress = { animatedLevel },
                modifier = Modifier.fillMaxWidth(),
                color = scheme.primary,
                trackColor = scheme.onPrimaryContainer.copy(alpha = 0.14f)
            )
            AnimatedVisibility(
                visible = peakText != null,
                enter = expandVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                    fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                    fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        peakText.orEmpty(),
                        style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalButton(
                        onClick = onResetPeak,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.testTag("reset_btn"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = scheme.surfaceContainerLowest.copy(alpha = 0.7f),
                            contentColor = scheme.onSurface
                        )
                    ) {
                        Text(stringResource(R.string.light_meter_reset_peak))
                    }
                }
            }
        }
    }
}

@Composable
private fun LightBeamChart(
    candela: Float,
    units: DistanceUnits,
    modifier: Modifier = Modifier
) {
    // A night scene: the flashlight shines from the left edge across a row of trees, each lit by the light that
    // actually reaches it.
    val labelColor = Color.White.copy(alpha = 0.72f)
    val glow = MaterialTheme.colorScheme.primaryFixedDim
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.verticalGradient(listOf(Color(0xFF0B1424), Color(0xFF16233A))))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = if (units == DistanceUnits.Feet) 3 else 1
            val fullMoon = android.hardware.SensorManager.LIGHT_FULLMOON
            val sunrise = android.hardware.SensorManager.LIGHT_SUNRISE
            val minLux = ln(fullMoon - 0.05f)
            val maxLuxLn = ln(sunrise)
            fun brightness(index: Int): Float {
                if (candela <= 0f) return 0f
                val lux = Optics.luxAtDistance(candela, Distance.from(((index + 1) * step).toFloat(), units))
                return ((ln(lux) - minLux) / (maxLuxLn - minLux)).coerceIn(0f, 1f)
            }
            val groundTop = size.height - 20.dp.toPx()
            // The beam: a soft cone whose glow fades the way the light does.
            val columns = 100
            val colWidth = size.width / columns
            for (i in 0 until columns) {
                val b = brightness(i)
                if (b <= 0f) continue
                val spread = (i + 1) / columns.toFloat()
                val halfHeight = (groundTop * 0.18f) + groundTop * 0.45f * spread
                val center = groundTop * 0.55f
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.Transparent, glow.copy(alpha = b * 0.55f), Color.Transparent),
                        startY = center - halfHeight,
                        endY = center + halfHeight
                    ),
                    topLeft = Offset(i * colWidth, center - halfHeight),
                    size = Size(colWidth + 1f, halfHeight * 2)
                )
                drawRect(
                    color = glow.copy(alpha = b * 0.8f),
                    topLeft = Offset(i * colWidth, groundTop),
                    size = Size(colWidth + 1f, 3.dp.toPx())
                )
            }
            drawRect(
                color = Color.White.copy(alpha = 0.06f),
                topLeft = Offset(0f, groundTop),
                size = Size(size.width, size.height - groundTop)
            )
            val paint = Paint().apply {
                color = android.graphics.Color.argb((labelColor.alpha * 255).toInt(), 255, 255, 255)
                textAlign = Paint.Align.CENTER
                textSize = 11.dp.toPx()
                isAntiAlias = true
            }
            listOf(9, 24, 49, 74).forEach { index ->
                val center = (index + 0.5f) * colWidth
                val b = brightness(index)
                val tree = lerp(Color.White.copy(alpha = 0.12f), Color.White, b)
                drawTree(this, center, groundTop, 34.dp.toPx(), tree)
                drawIntoCanvas {
                    it.nativeCanvas.drawText(((index + 1) * step).toString(), center, size.height - 6.dp.toPx(), paint)
                }
            }
        }
        if (candela <= 0f) {
            Text(
                text = stringResource(R.string.light_meter_preview_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = labelColor,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = TrailSenseSpacing.xl, vertical = TrailSenseSpacing.lg)
            )
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
