package com.kylecorry.andromeda.views.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kylecorry.sol.math.Vector2
import kotlin.math.abs
import kotlin.math.roundToInt

/** Immutable layers are redrawn whenever the caller supplies new data. */
sealed interface PlotLayer {
    val points: List<Vector2> get() = emptyList()
    val onPointClick: ((Vector2) -> Unit)? get() = null

    data class Line(override val points: List<Vector2>, val color: Color, val width: Float = 2f, override val onPointClick: ((Vector2) -> Unit)? = null) : PlotLayer
    data class Scatter(override val points: List<Vector2>, val color: Color, val radius: Float = 6f, override val onPointClick: ((Vector2) -> Unit)? = null) : PlotLayer
    data class Bars(override val points: List<Vector2>, val color: Color, val width: Float, val baseline: Float = 0f, override val onPointClick: ((Vector2) -> Unit)? = null) : PlotLayer
    data class Area(override val points: List<Vector2>, val color: Color, val baseline: Float = 0f, val lineColor: Color = Color.Transparent, val lineWidth: Float = 2f, override val onPointClick: ((Vector2) -> Unit)? = null) : PlotLayer
    data class Bounds(val upper: List<Vector2>, val lower: List<Vector2>, val color: Color) : PlotLayer { override val points get() = upper + lower }
    data class Band(val top: Float, val bottom: Float, val color: Color) : PlotLayer
    data class HorizontalLine(val value: Float, val color: Color, val width: Float = 2f) : PlotLayer
    data class Markers(override val points: List<Vector2>, val image: ImageBitmap, val size: Float = 12f, val rotation: Float = 0f, val tint: Color? = null, override val onPointClick: ((Vector2) -> Unit)? = null) : PlotLayer
    data class Label(val position: Vector2, val text: String, val color: Color, val size: Float = 12f) : PlotLayer
    data class Overlay(val draw: DrawScope.(PlotCoordinates) -> Unit) : PlotLayer
}

class PlotCoordinates internal constructor(
    val xRange: ClosedFloatingPointRange<Float>,
    val yRange: ClosedFloatingPointRange<Float>,
    val size: Size,
) {
    fun toPixel(point: Vector2): Offset = Offset(
        (point.x - xRange.start) / (xRange.endInclusive - xRange.start) * size.width,
        size.height - (point.y - yRange.start) / (yRange.endInclusive - yRange.start) * size.height,
    )

    fun toData(point: Offset): Vector2 = Vector2(
        xRange.start + point.x / size.width * (xRange.endInclusive - xRange.start),
        yRange.start + (size.height - point.y) / size.height * (yRange.endInclusive - yRange.start),
    )
}

/** A composable plot with ordered layers and optional point and background click handling. */
@Composable
fun Plot(
    layers: List<PlotLayer>,
    xRange: ClosedFloatingPointRange<Float>,
    yRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    onClick: ((Vector2) -> Unit)? = null,
    xAxis: PlotAxis? = null,
    yAxis: PlotAxis? = null,
    emptyText: String? = null,
) {
    require(xRange.start < xRange.endInclusive && yRange.start < yRange.endInclusive)
    val measurer = rememberTextMeasurer()
    val clickModifier = Modifier.pointerInput(layers, xRange, yRange, onClick) {
        detectTapGestures { position ->
            val coordinates = PlotCoordinates(xRange, yRange, Size(size.width.toFloat(), size.height.toFloat()))
            val radius = 12.dp.toPx()
            val hit = layers.asReversed().firstNotNullOfOrNull { layer ->
                val handler = layer.onPointClick ?: return@firstNotNullOfOrNull null
                val point = if (layer is PlotLayer.Bars) {
                    layer.points.firstOrNull { point ->
                        val a = coordinates.toPixel(Vector2(point.x - layer.width / 2, point.y))
                        val b = coordinates.toPixel(Vector2(point.x + layer.width / 2, layer.baseline))
                        position.x in minOf(a.x, b.x)..maxOf(a.x, b.x) && position.y in minOf(a.y, b.y)..maxOf(a.y, b.y)
                    }
                } else {
                    layer.points.minByOrNull { (coordinates.toPixel(it) - position).getDistance() }
                        ?.takeIf { (coordinates.toPixel(it) - position).getDistance() <= radius }
                }
                point?.let { handler to it }
            }
            if (hit != null) hit.first(hit.second) else onClick?.invoke(coordinates.toData(position))
        }
    }
    Canvas(modifier.then(clickModifier)) {
        val coordinates = PlotCoordinates(xRange, yRange, size)
        fun polygon(points: List<Vector2>, close: Boolean): Path = Path().apply {
            points.forEachIndexed { index, point ->
                val p = coordinates.toPixel(point)
                if (index == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            if (close && points.isNotEmpty()) close()
        }
        fun tick(index: Int, count: Int, range: ClosedFloatingPointRange<Float>): Float =
            range.start + (range.endInclusive - range.start) * (if (count == 1) 0f else index.toFloat() / (count - 1))
        xAxis?.let { axis ->
            repeat(axis.count.coerceAtLeast(0)) { i ->
                val value = tick(i, axis.count, xRange)
                val x = coordinates.toPixel(Vector2(value, yRange.start)).x
                if (axis.grid) drawLine(axis.gridColor, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                val text = measurer.measure(axis.format(value), TextStyle(color = axis.labelColor, fontSize = axis.labelSize.sp))
                drawText(text, topLeft = Offset((x - text.size.width / 2).coerceIn(0f, (size.width - text.size.width).coerceAtLeast(0f)), size.height - text.size.height))
            }
        }
        yAxis?.let { axis ->
            repeat(axis.count.coerceAtLeast(0)) { i ->
                val value = tick(i, axis.count, yRange)
                val y = coordinates.toPixel(Vector2(xRange.start, value)).y
                if (axis.grid) drawLine(axis.gridColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                val text = measurer.measure(axis.format(value), TextStyle(color = axis.labelColor, fontSize = axis.labelSize.sp))
                drawText(text, topLeft = Offset(0f, (y - text.size.height / 2).coerceIn(0f, (size.height - text.size.height).coerceAtLeast(0f))))
            }
        }
        layers.forEach { layer ->
            when (layer) {
                is PlotLayer.Line -> drawPath(polygon(layer.points, false), layer.color, style = Stroke(layer.width.dp.toPx()))
                is PlotLayer.Scatter -> layer.points.forEach { drawCircle(layer.color, layer.radius.dp.toPx(), coordinates.toPixel(it)) }
                is PlotLayer.Bars -> layer.points.forEach { point ->
                    val a = coordinates.toPixel(Vector2(point.x - layer.width / 2, point.y))
                    val b = coordinates.toPixel(Vector2(point.x + layer.width / 2, layer.baseline))
                    drawRect(layer.color, Offset(minOf(a.x, b.x), minOf(a.y, b.y)), Size(abs(a.x - b.x), abs(a.y - b.y)))
                }
                is PlotLayer.Area -> if (layer.points.isNotEmpty()) {
                    val area = layer.points + listOf(layer.points.last().copy(y = layer.baseline), layer.points.first().copy(y = layer.baseline))
                    drawPath(polygon(area, true), layer.color)
                    if (layer.lineColor != Color.Transparent) drawPath(polygon(layer.points, false), layer.lineColor, style = Stroke(layer.lineWidth.dp.toPx()))
                }
                is PlotLayer.Bounds -> drawPath(polygon(layer.upper + layer.lower.asReversed(), true), layer.color)
                is PlotLayer.Band -> {
                    val y1 = coordinates.toPixel(Vector2(xRange.start, layer.top)).y
                    val y2 = coordinates.toPixel(Vector2(xRange.start, layer.bottom)).y
                    drawRect(layer.color, Offset(0f, minOf(y1, y2)), Size(size.width, abs(y2 - y1)))
                }
                is PlotLayer.HorizontalLine -> {
                    val y = coordinates.toPixel(Vector2(xRange.start, layer.value)).y
                    drawLine(layer.color, Offset(0f, y), Offset(size.width, y), layer.width.dp.toPx())
                }
                is PlotLayer.Markers -> layer.points.forEach { point ->
                    val p = coordinates.toPixel(point)
                    val dimension = layer.size.dp.toPx().roundToInt()
                    rotate(layer.rotation, p) { drawImage(layer.image, dstOffset = IntOffset((p.x - dimension / 2).roundToInt(), (p.y - dimension / 2).roundToInt()), dstSize = IntSize(dimension, dimension), colorFilter = layer.tint?.let(ColorFilter::tint)) }
                }
                is PlotLayer.Label -> {
                    val text = measurer.measure(layer.text, TextStyle(color = layer.color, fontSize = layer.size.sp))
                    val p = coordinates.toPixel(layer.position)
                    drawText(text, topLeft = Offset(p.x - text.size.width / 2, p.y - text.size.height / 2))
                }
                is PlotLayer.Overlay -> layer.draw(this, coordinates)
            }
        }
        if (emptyText != null && layers.all { it.points.isEmpty() }) {
            val text = measurer.measure(emptyText)
            drawText(text, topLeft = Offset((size.width - text.size.width) / 2, (size.height - text.size.height) / 2))
        }
    }
}

/** Axis labels and grid can be configured independently for each dimension. */
data class PlotAxis(
    val count: Int = 3,
    val grid: Boolean = true,
    val gridColor: Color = Color.LightGray,
    val labelColor: Color = Color.DarkGray,
    val labelSize: Float = 10f,
    val format: (Float) -> String = { it.toString() },
)

fun plotRange(values: Iterable<Float>, margin: Float = 0f, minimumRange: Float = 1f): ClosedFloatingPointRange<Float> {
    require(margin >= 0 && minimumRange > 0)
    val list = values.toList()
    val min = list.minOrNull() ?: 0f
    val max = list.maxOrNull() ?: 0f
    val middle = (min + max) / 2f
    return minOf(min - margin, middle - minimumRange / 2)..maxOf(max + margin, middle + minimumRange / 2)
}

fun indexedPlotData(values: List<Float>): List<Vector2> = values.mapIndexed { index, value -> Vector2(index.toFloat(), value) }

/** Maps readings to hours since [startTime] and a caller-selected value. */
fun <T> plotReadings(
    readings: List<com.kylecorry.sol.units.Reading<T>>,
    startTime: kotlin.time.Instant? = null,
    value: (T) -> Float,
): List<Vector2> {
    val start = startTime ?: readings.firstOrNull()?.time ?: return emptyList()
    return readings.map { Vector2((it.time - start).inWholeMilliseconds / 3_600_000f, value(it.value)) }
}
