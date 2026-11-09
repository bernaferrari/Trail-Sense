package com.kylecorry.andromeda.pdf

import com.kylecorry.andromeda.bitmaps.PixelImage
import java.awt.Color
import java.awt.image.BufferedImage
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer

actual class PdfPages actual constructor(bytes: ByteArray) : AutoCloseable {
    private val lock = Any()
    private val document = Loader.loadPDF(bytes)
    private val renderer = PDFRenderer(document)
    private var closed = false
    actual val pageCount: Int get() = synchronized(lock) { check(!closed); document.numberOfPages }
    actual fun pageSize(page: Int): PdfPageSize = synchronized(lock) {
        check(!closed)
        val source = document.getPage(page)
        val box = source.cropBox
        if (source.rotation % 180 == 0) PdfPageSize(box.width, box.height) else PdfPageSize(box.height, box.width)
    }
    actual fun render(page: Int, width: Int, height: Int, background: Int): PixelImage = synchronized(lock) {
        check(!closed)
        requireRenderSize(width, height)
        val size = pageSize(page)
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try {
            graphics.background = Color(background, true)
            graphics.color = graphics.background
            graphics.fillRect(0, 0, width, height)
            renderer.renderPageToGraphics(page, graphics, width / size.widthPoints, height / size.heightPoints)
        } finally { graphics.dispose() }
        PixelImage(width, height, image.getRGB(0, 0, width, height, null, 0, width))
    }
    actual override fun close() = synchronized(lock) {
        if (!closed) { closed = true; document.close() }
    }
}
