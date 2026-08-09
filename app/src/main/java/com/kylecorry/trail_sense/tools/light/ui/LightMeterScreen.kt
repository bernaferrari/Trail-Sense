@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.light.ui

import android.graphics.Paint
import android.graphics.Canvas as AndroidCanvas
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAnimation
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChip
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max

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

    val beamDistanceText = beamDistance?.let {
        val beamDist = Optics.lightBeamDistance(candela).convertTo(it.units)
        stringResource(R.string.beam_distance, formatter.formatDistance(beamDist))
    }.orEmpty()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(TrailSenseSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xl)
        ) {
            // Hero: live illuminance reading in a bold tonal card.
            LightHeroCard(
                lux = illuminance,
                luxText = formatter.formatLux(illuminance),
                candelaText = if (beamDistance == null) "" else formatter.formatCandela(candela),
                beamDistanceText = beamDistanceText
            )

            // Beam visualization grouped in a surface card.
            LightBeamCard(
                candela = candela,
                units = beamDistanceUnit
            )

            // Controls: distance input + unit selection + reset.
            LightControls(
                distanceUnits = distanceUnits,
                beamDistanceUnit = beamDistanceUnit,
                beamDistanceAmount = beamDistanceAmount,
                formatter = formatter,
                onAmountChange = {
                    beamDistanceAmount = it
                    maxLux = 0f
                },
                onUnitSelected = {
                    beamDistanceUnit = it
                    maxLux = 0f
                }
            )

            TrailSenseButton(
                text = stringResource(R.string.reset),
                onClick = { maxLux = 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reset_btn")
            )
        }
    }
}

@Composable
private fun LightHeroCard(
    lux: Float,
    luxText: String,
    candelaText: String,
    beamDistanceText: String
) {
    val animatedLux by animateFloatAsState(
        targetValue = lux,
        animationSpec = TrailSenseAnimation.standardSpring,
        label = "luxValue"
    )
    // Drive a subtle brightness ramp on the hero based on the live reading so the
    // card visibly reacts to light. Purely cosmetic; the displayed text is exact.
    val brightness = (ln(animatedLux + 1f) / ln(10001f)).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("light_title"),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.padding(TrailSenseSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
        ) {
            // Sun-like indicator whose glow tracks the measured illuminance.
            Box(
                modifier = Modifier.height(8.dp).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(TrailSenseSpacing.xs),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f + 0.75f * brightness),
                    modifier = Modifier
                        .fillMaxWidth(0.2f + 0.6f * brightness)
                        .height(6.dp)
                ) {}
            }
            Text(
                text = luxText,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
            if (candelaText.isNotEmpty()) {
                Text(
                    text = candelaText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
            if (beamDistanceText.isNotEmpty()) {
                Text(
                    text = beamDistanceText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("beam_distance_text"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            } else {
                // Preserve the test target even when empty (matches original behavior).
                Text(
                    text = beamDistanceText,
                    modifier = Modifier.testTag("beam_distance_text"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun LightBeamCard(
    candela: Float,
    units: DistanceUnits
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(TrailSenseSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
        ) {
            Text(
                text = stringResource(R.string.preview),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            LightBeamChart(
                candela = candela,
                units = units,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp)
                    .testTag("light_chart")
            )
        }
    }
}

@Composable
private fun LightControls(
    distanceUnits: List<DistanceUnits>,
    beamDistanceUnit: DistanceUnits,
    beamDistanceAmount: String,
    formatter: FormatService,
    onAmountChange: (String) -> Unit,
    onUnitSelected: (DistanceUnits) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(TrailSenseSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
        ) {
            Text(
                text = stringResource(R.string.distance_from_flashlight),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            OutlinedTextField(
                value = beamDistanceAmount,
                onValueChange = onAmountChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("beam_distance"),
                label = { Text(stringResource(R.string.distance)) },
                suffix = { Text(formatter.getDistanceUnitName(beamDistanceUnit, true)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = MaterialTheme.shapes.medium,
                singleLine = true
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
            ) {
                distanceUnits.forEach { unit ->
                    TrailSenseChip(
                        text = formatter.getDistanceUnitName(unit, true),
                        selected = unit == beamDistanceUnit,
                        onClick = { onUnitSelected(unit) }
                    )
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
    val shape = MaterialTheme.shapes.medium
    Surface(
        modifier = modifier,
        shape = shape,
        color = Color.Black
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (candela <= 0f) {
                return@Canvas
            }

            val fullMoon = android.hardware.SensorManager.LIGHT_FULLMOON
            val sunrise = android.hardware.SensorManager.LIGHT_SUNRISE
            val minLux = ln(fullMoon - 0.05f)
            val maxLux = ln(sunrise)
            val gradients = (1..100).map {
                val distance = if (units == DistanceUnits.Feet) it * 3 else it
                val lux = Optics.luxAtDistance(candela, Distance.from(distance.toFloat(), units))
                val pct = ((ln(lux) - minLux) / (maxLux - minLux)).coerceIn(0f, 1f)
                floor(pct * 255).toInt()
            }
            val gradWidth = size.width / gradients.size.toFloat()
            val treeIndices = setOf(9, 24, 49, 74)
            val paint = Paint().apply {
                color = android.graphics.Color.WHITE
                textAlign = Paint.Align.CENTER
                textSize = 10.dp.toPx()
            }

            gradients.forEachIndexed { index, alpha ->
                val start = index * gradWidth
                val color = Color.White.copy(alpha = alpha / 255f)
                if (index in treeIndices) {
                    val center = start + gradWidth / 2f
                    drawIntoCanvas { canvas ->
                        drawTree(canvas.nativeCanvas, center, size.height - 18.dp.toPx(), color)
                        val distance = ((index + 1) * if (units == DistanceUnits.Feet) 3 else 1).toString()
                        canvas.nativeCanvas.drawText(
                            distance,
                            center,
                            size.height / 2f,
                            paint
                        )
                    }
                }
                drawRect(
                    color = color,
                    topLeft = Offset(start, size.height - 22.dp.toPx()),
                    size = Size(gradWidth, 22.dp.toPx())
                )
            }
        }
    }
}

private fun drawTree(
    canvas: AndroidCanvas,
    centerX: Float,
    bottom: Float,
    color: Color
) {
    val paint = Paint().apply {
        this.color = android.graphics.Color.argb(
            (color.alpha * 255).toInt(),
            255,
            255,
            255
        )
        style = Paint.Style.FILL
    }
    val trunkWidth = 5f
    canvas.drawRect(centerX - trunkWidth / 2f, bottom - 18f, centerX + trunkWidth / 2f, bottom, paint)
    val path = android.graphics.Path().apply {
        moveTo(centerX, bottom - 58f)
        lineTo(centerX - 28f, bottom - 12f)
        lineTo(centerX + 28f, bottom - 12f)
        close()
    }
    canvas.drawPath(path, paint)
}
