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
        modifier = modifier,
        actions = {
            TextButton(
                onClick = { maxLux = 0f },
                modifier = Modifier.testTag("reset_btn")
            ) {
                Text(stringResource(R.string.reset))
            }
        }
    ) {
        item("hero") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = TrailSenseSpacing.lg)
                    .testTag("light_title"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
            ) {
                TrailSenseReading(value = luxText)
                AnimatedVisibility(
                    visible = beamDistance != null,
                    enter = expandVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                        fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
                    exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                        fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec())
                ) {
                    TrailSenseMetricRow(
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
                TrailSenseGroupCard(position = position) {
                    LightBeamChart(
                        candela = candela,
                        units = beamDistanceUnit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(125.dp)
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

@Composable
private fun LightBeamChart(
    candela: Float,
    units: DistanceUnits,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(TrailSenseShape.groupInner * 2),
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
