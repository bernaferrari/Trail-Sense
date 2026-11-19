package com.kylecorry.andromeda.pdf

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.ksensor.core.context.KSensorContext
import com.kylecorry.andromeda.bitmaps.PixelImage
import java.io.File

public actual class PdfPages actual constructor(bytes: ByteArray) : AutoCloseable {
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

    public actual val pageCount: Int get() = synchronized(lock) { check(!closed); renderer.pageCount }
    public actual fun pageSize(page: Int): PdfPageSize = synchronized(lock) {
        check(!closed)
        renderer.openPage(page).use { PdfPageSize(it.width.toFloat(), it.height.toFloat()) }
    }
    public actual fun render(page: Int, width: Int, height: Int, background: Int): PixelImage =
        renderRegion(page, width, height, 0.0, 0.0, 1.0, 1.0, background)

    public actual fun renderRegion(
        page: Int, width: Int, height: Int,
        left: Double, top: Double, right: Double, bottom: Double, background: Int
    ): PixelImage = synchronized(lock) {
        check(!closed)
        requirePageRegion(left, top, right, bottom)
        val pageWidth = width / (right - left)
        val pageHeight = height / (bottom - top)
        requireRenderSize(width, height)
        renderer.openPage(page).use { source ->
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            try {
                bitmap.eraseColor(background)
                val matrix = Matrix().apply {
                    setScale((pageWidth / source.width).toFloat(), (pageHeight / source.height).toFloat())
                    postTranslate((-left * pageWidth).toFloat(), (-top * pageHeight).toFloat())
                }
                source.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                val pixels = IntArray(width * height)
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                PixelImage(width, height, pixels)
            } finally { bitmap.recycle() }
        }
    }
    actual override fun close(): Unit = synchronized(lock) {
        if (!closed) {
            closed = true
            try { renderer.close() } finally { file.delete() }
        }
    }
}
