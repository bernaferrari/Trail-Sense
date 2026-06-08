package com.kylecorry.trail_sense.tools.navigation.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.AttributeSet
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils.getCardinalDirectionColor
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.camera.AugmentedRealityUtils
import com.kylecorry.trail_sense.shared.extensions.getValuesBetween
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme

class LinearCompassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseCompassView(context, attrs, defStyleAttr) {

    private val formatService = FormatService.getInstance(context)
    private val north by lazy { formatService.formatDirection(CompassDirection.North) }
    private val south by lazy { formatService.formatDirection(CompassDirection.South) }
    private val east by lazy { formatService.formatDirection(CompassDirection.East) }
    private val west by lazy { formatService.formatDirection(CompassDirection.West) }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private var canvas: Canvas? = null
    private var canvasWidth = 0f
    private var canvasHeight = 0f
    private var iconSize = Resources.dp(context, 25f).toInt()

    var range = 180f
        set(value) {
            field = value
            requestRedraw()
        }

    private val rawMinimum: Float
        get() = azimuth - range / 2

    private val rawMaximum: Float
        get() = azimuth + range / 2

    @Composable
    override fun Content() {
        TrailSenseTheme {
            Canvas(modifier = Modifier.fillMaxSize()) {
                renderVersion
                if (!isVisible) {
                    return@Canvas
                }
                canvas = drawContext.canvas.nativeCanvas
                canvasWidth = size.width
                canvasHeight = size.height
                drawAzimuth()
                drawCompass()
                drawCompassLayers()
                canvas = null
            }
        }
    }

    private fun drawAzimuth() {
        val nativeCanvas = canvas ?: return
        val bitmap = getBitmap(R.drawable.ic_arrow_target, iconSize)
        paint.reset()
        paint.isAntiAlias = true
        paint.isFilterBitmap = true
        paint.colorFilter = PorterDuffColorFilter(
            Resources.androidTextColorPrimary(context),
            PorterDuff.Mode.SRC_IN
        )
        nativeCanvas.drawBitmap(bitmap, canvasWidth / 2f - iconSize / 2f, 0f, paint)
        paint.colorFilter = null
    }

    private fun drawCompass() {
        val nativeCanvas = canvas ?: return
        val values = getValuesBetween(rawMinimum, rawMaximum, 5f).map { it.toInt() }
        val primaryColor = Resources.getCardinalDirectionColor(context)
        val secondaryColor = Resources.androidTextColorPrimary(context)

        values.forEach {
            val x = toPixel(it.toFloat())
            val isCardinalTick = it % 45 == 0
            paint.reset()
            paint.isAntiAlias = true
            paint.color = if (isCardinalTick) primaryColor else secondaryColor
            paint.strokeWidth = 8f
            paint.style = Paint.Style.STROKE

            val lineHeight = when {
                it % 90 == 0 -> 0.5f * canvasHeight
                it % 15 == 0 -> 0.75f * canvasHeight
                else -> 10 / 12f * canvasHeight
            }
            nativeCanvas.drawLine(x, canvasHeight, x, lineHeight, paint)

            if (it % 90 == 0) {
                val coord = when (it) {
                    -90, 270 -> west
                    0, 360 -> north
                    90, 450 -> east
                    -180, 180 -> south
                    else -> ""
                }
                paint.reset()
                paint.isAntiAlias = true
                paint.color = secondaryColor
                paint.textAlign = Paint.Align.CENTER
                paint.textSize = sp(15f)
                nativeCanvas.drawText(coord, x, 5 / 12f * canvasHeight, paint)
            }
        }
    }

    override fun draw(reference: IMappableReferencePoint, size: Int?) {
        val nativeCanvas = canvas ?: return
        val sizeDp = size?.let { dp(it.toFloat()).toInt() } ?: iconSize
        val x = toPixel(reference.bearing).coerceIn(0f, canvasWidth)
        val bitmap = getBitmap(reference.drawableId, sizeDp)
        paint.reset()
        paint.isAntiAlias = true
        paint.isFilterBitmap = true
        paint.alpha = (255 * reference.opacity).toInt()
        paint.colorFilter = reference.tint?.let { PorterDuffColorFilter(it, PorterDuff.Mode.SRC_IN) }
        nativeCanvas.save()
        nativeCanvas.translate(x - sizeDp / 2f, (iconSize - sizeDp) * 0.6f)
        nativeCanvas.rotate(reference.rotation, bitmap.width / 2f, bitmap.height / 2f)
        nativeCanvas.drawBitmap(bitmap, 0f, 0f, paint)
        nativeCanvas.restore()
        paint.alpha = 255
        paint.colorFilter = null
    }

    override fun draw(bearing: IMappableBearing, stopAt: Coordinate?) {
        val nativeCanvas = canvas ?: return
        val x = toPixel(bearing.bearing) - canvasWidth / 2f
        paint.reset()
        paint.isAntiAlias = true
        paint.color = bearing.color
        paint.alpha = 100
        paint.style = Paint.Style.FILL
        nativeCanvas.drawRect(
            canvasWidth / 2f,
            canvasHeight - 0.5f * canvasHeight,
            canvasWidth / 2f + x,
            canvasHeight,
            paint
        )
        paint.alpha = 255
    }

    private fun toPixel(bearing: Float): Float {
        return AugmentedRealityUtils.getPixelLinear(
            bearing,
            azimuth,
            0f,
            0f,
            Size(canvasWidth, canvasHeight),
            Size(range, 0f)
        ).x
    }
}
