package com.kylecorry.andromeda.pdf

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.ksensor.core.context.KSensorContext
import com.kylecorry.andromeda.bitmaps.PixelImage
import java.io.File

actual class PdfPages actual constructor(bytes: ByteArray) : AutoCloseable {
    private val lock = Any()
    private val file = File.createTempFile("andromeda-pdf-", ".pdf", KSensorContext.get().cacheDir)
    private val renderer: PdfRenderer
    private var closed = false

    init {
        var descriptor: ParcelFileDescriptor? = null
        try {
            file.writeBytes(bytes)
            descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(descriptor)
        } catch (error: Throwable) {
            descriptor?.close()
            file.delete()
            throw error
        }
    }

    actual val pageCount: Int get() = synchronized(lock) { check(!closed); renderer.pageCount }
    actual fun pageSize(page: Int): PdfPageSize = synchronized(lock) {
        check(!closed)
        renderer.openPage(page).use { PdfPageSize(it.width.toFloat(), it.height.toFloat()) }
    }
    actual fun render(page: Int, width: Int, height: Int, background: Int): PixelImage = synchronized(lock) {
        check(!closed)
        requireRenderSize(width, height)
        renderer.openPage(page).use { source ->
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            try {
                bitmap.eraseColor(background)
                val matrix = Matrix().apply { setScale(width.toFloat() / source.width, height.toFloat() / source.height) }
                source.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                val pixels = IntArray(width * height)
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                PixelImage(width, height, pixels)
            } finally { bitmap.recycle() }
        }
    }
    actual override fun close() = synchronized(lock) {
        if (!closed) {
            closed = true
            try { renderer.close() } finally { file.delete() }
        }
    }
}
