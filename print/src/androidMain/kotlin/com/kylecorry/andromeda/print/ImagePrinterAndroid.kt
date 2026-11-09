package com.kylecorry.andromeda.print

import android.app.Activity
import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import com.kylecorry.andromeda.bitmaps.toBitmap
import java.io.FileOutputStream

public fun imagePrinter(context: Context): ImagePrinter = ImagePrinter { image, jobName, options ->
    require(context is Activity) { "Printing requires an Activity context" }
    val bitmap = image.toBitmap()
    val landscape = when (options.orientation) {
        PrintOrientation.Auto -> image.width > image.height
        PrintOrientation.Portrait -> false
        PrintOrientation.Landscape -> true
    }
    val attributes = PrintAttributes.Builder()
        .setMediaSize(if (landscape) PrintAttributes.MediaSize.UNKNOWN_LANDSCAPE else PrintAttributes.MediaSize.UNKNOWN_PORTRAIT)
        .setColorMode(if (options.color == PrintColorMode.Monochrome) PrintAttributes.COLOR_MODE_MONOCHROME else PrintAttributes.COLOR_MODE_COLOR)
        .build()
    val adapter = object : PrintDocumentAdapter() {
        private var currentAttributes = attributes

        override fun onLayout(oldAttributes: PrintAttributes?, newAttributes: PrintAttributes, cancellationSignal: CancellationSignal?, callback: LayoutResultCallback, extras: Bundle?) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }
            currentAttributes = newAttributes
            val info = PrintDocumentInfo.Builder(jobName).setContentType(PrintDocumentInfo.CONTENT_TYPE_PHOTO).setPageCount(1).build()
            callback.onLayoutFinished(info, oldAttributes != newAttributes)
        }

        override fun onWrite(pages: Array<out PageRange>, destination: ParcelFileDescriptor, cancellationSignal: CancellationSignal?, callback: WriteResultCallback) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onWriteCancelled()
                return
            }
            if (pages.none { it.start <= 0 && it.end >= 0 }) {
                callback.onWriteFinished(emptyArray())
                return
            }
            try {
                val document = PrintedPdfDocument(context, currentAttributes)
                try {
                    val page = document.startPage(0)
                    val bounds = page.info.contentRect
                    val layout = imagePrintLayout(image.width, image.height, bounds.width().toDouble(), bounds.height().toDouble(), options.scale)
                    val target = RectF(
                        (bounds.left + layout.x).toFloat(), (bounds.top + layout.y).toFloat(),
                        (bounds.left + layout.x + layout.width).toFloat(), (bounds.top + layout.y + layout.height).toFloat(),
                    )
                    val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                        if (currentAttributes.colorMode == PrintAttributes.COLOR_MODE_MONOCHROME) {
                            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                        }
                    }
                    page.canvas.save()
                    page.canvas.clipRect(bounds)
                    page.canvas.drawBitmap(bitmap, null, target, paint)
                    page.canvas.restore()
                    document.finishPage(page)
                    FileOutputStream(destination.fileDescriptor).use { document.writeTo(it) }
                } finally { document.close() }
                if (cancellationSignal?.isCanceled == true) callback.onWriteCancelled()
                else callback.onWriteFinished(arrayOf(PageRange(0, 0)))
            } catch (error: Exception) {
                callback.onWriteFailed(error.message)
            }
        }

        override fun onFinish() { bitmap.recycle() }
    }
    try {
        (context.getSystemService(Context.PRINT_SERVICE) as PrintManager).print(jobName, adapter, attributes)
    } catch (error: Throwable) {
        bitmap.recycle()
        throw error
    }
}
