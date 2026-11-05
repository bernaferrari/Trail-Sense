package com.kylecorry.trail_sense.shared.views.subscaleview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.shared.canvas.tiles.ImageRegionDecoder
import com.kylecorry.trail_sense.shared.canvas.tiles.PdfImageRegionDecoder
import com.kylecorry.trail_sense.shared.canvas.tiles.RegionDecoder

/**
 * A large image to show in tiles: where it is and whether it is a PDF (rendered page regions) or a bitmap image
 * (decoded regions).
 */
data class ImageTileSourceSpec(val uri: Uri, val isPdf: Boolean)

/** An opened image: its full size in pixels (as stored, before any orientation) and a decoder for its regions. */
internal class ImageTileSource(
    val width: Int,
    val height: Int,
    private val decoder: RegionDecoder
) {
    suspend fun decode(region: Rect, sampleSize: Int): Bitmap? {
        return decoder.decodeRegionSuspend(region, sampleSize)
    }

    suspend fun close() {
        decoder.recycleSuspend()
    }

    companion object {
        private val bitmapConfig = Bitmap.Config.RGB_565

        suspend fun open(context: Context, spec: ImageTileSourceSpec): ImageTileSource = onIO {
            if (spec.isPdf) {
                val decoder = PdfImageRegionDecoder(bitmapConfig)
                val size = decoder.init(context, spec.uri)
                ImageTileSource(size.x, size.y, decoder)
            } else {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(spec.uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }
                val decoder = ImageRegionDecoder(context, bitmapConfig)
                decoder.init(spec.uri)
                ImageTileSource(options.outWidth, options.outHeight, decoder)
            }
        }
    }
}
