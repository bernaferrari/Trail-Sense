package com.kylecorry.trail_sense.shared.canvas.tiles

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.net.Uri
import android.util.Size
import androidx.core.graphics.toRectF
import com.kylecorry.andromeda.core.math.MathUtils
import com.kylecorry.andromeda.pdf.PDFRenderer2
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap

class PdfImageRegionDecoder(private val bitmapConfig: Bitmap.Config? = null) : RegionDecoder {

    private lateinit var renderer: PDFRenderer2
    private var pageIndex = 0

    fun init(
        context: Context?,
        uri: Uri,
        pageIndex: Int = 0
    ): Point {
        this.pageIndex = pageIndex
        val ctx = requireNotNull(context)
        val config = bitmapConfig ?: Bitmap.Config.RGB_565
        val size = PDFRenderer2(ctx, uri, 1f, config).use { it.getSize(pageIndex) }
        val scaledSize = MathUtils.scaleToBounds(
            size,
            Size(PhotoMap.DESIRED_PDF_SIZE, PhotoMap.DESIRED_PDF_SIZE)
        )
        val scale = scaledSize.width.toFloat() / size.width.toFloat()
        renderer = PDFRenderer2(ctx, uri, scale, config)
        return Point(scaledSize.width, scaledSize.height)
    }

    private fun decodeRegion(
        sRect: Rect,
        sampleSize: Int
    ): Bitmap {
        val scaledSize = Size(sRect.width() / sampleSize, sRect.height() / sampleSize)
        return renderer.toBitmap(
            scaledSize,
            page = pageIndex,
            srcRegion = sRect.toRectF()
        )!!
    }

    override suspend fun decodeRegionSuspend(
        sRect: Rect,
        sampleSize: Int
    ): Bitmap = onIO {
        decodeRegion(sRect, sampleSize)
    }

    override suspend fun recycleSuspend() = onIO {
        renderer.close()
    }

}
