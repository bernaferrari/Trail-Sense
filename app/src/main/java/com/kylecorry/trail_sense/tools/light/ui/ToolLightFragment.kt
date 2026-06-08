package com.kylecorry.trail_sense.tools.light.ui

import android.graphics.Paint
import android.graphics.Canvas as AndroidCanvas
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max

class ToolLightFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        LightMeterScreen()
    }
}

@Composable
private fun LightMeterScreen(
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
            modifier = Modifier.fillMaxSize()
        ) {
            LightHeader(
                title = formatter.formatLux(illuminance),
                subtitle = if (beamDistance == null) "" else formatter.formatCandela(candela)
            )
            Text(
                text = beamDistanceText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag("beam_distance_text"),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            LightBeamChart(
                candela = candela,
                units = beamDistanceUnit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp)
                    .padding(top = 8.dp)
                    .testTag("light_chart")
            )
            Text(
                text = stringResource(R.string.distance_from_flashlight),
                modifier = Modifier.padding(start = 16.dp, top = 32.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            OutlinedTextField(
                value = beamDistanceAmount,
                onValueChange = {
                    beamDistanceAmount = it
                    maxLux = 0f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp)
                    .testTag("beam_distance"),
                label = { Text(stringResource(R.string.distance)) },
                suffix = { Text(formatter.getDistanceUnitName(beamDistanceUnit, true)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            UnitSelector(
                units = distanceUnits,
                selected = beamDistanceUnit,
                formatter = formatter,
                onSelected = {
                    beamDistanceUnit = it
                    maxLux = 0f
                }
            )
            Button(
                onClick = { maxLux = 0f },
                modifier = Modifier
                    .padding(start = 16.dp, top = 16.dp)
                    .testTag("reset_btn")
            ) {
                Text(stringResource(R.string.reset))
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun LightHeader(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("light_title")
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun UnitSelector(
    units: List<DistanceUnits>,
    selected: DistanceUnits,
    formatter: FormatService,
    onSelected: (DistanceUnits) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp)
    ) {
        units.forEach { unit ->
            Button(
                onClick = { onSelected(unit) },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = formatter.getDistanceUnitName(unit, true),
                    color = if (unit == selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    }
                )
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
    Canvas(modifier = modifier) {
        drawRect(Color.Black)
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
