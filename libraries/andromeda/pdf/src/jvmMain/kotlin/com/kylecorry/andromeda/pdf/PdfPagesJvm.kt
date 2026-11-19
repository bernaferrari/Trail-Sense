package com.kylecorry.andromeda.pdf

import com.kylecorry.andromeda.bitmaps.PixelImage
import java.awt.Color
import java.awt.image.BufferedImage
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer

public actual class PdfPages actual constructor(bytes: ByteArray) : AutoCloseable {
    private val lock = Any()
    private val document = Loader.loadPDF(bytes)
    private val renderer = PDFRenderer(document)
    private var closed = false
    public actual val pageCount: Int get() = synchronized(lock) { check(!closed); document.numberOfPages }
    public actual fun pageSize(page: Int): PdfPageSize = synchronized(lock) {
        check(!closed)
        val source = document.getPage(page)
        val box = source.cropBox
        if (source.rotation % 180 == 0) PdfPageSize(box.width, box.height) else PdfPageSize(box.height, box.width)
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
        val size = pageSize(page)
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try {
            graphics.background = Color(background, true)
            graphics.color = graphics.background
            graphics.fillRect(0, 0, width, height)
            graphics.translate(-left * pageWidth, -top * pageHeight)
            renderer.renderPageToGraphics(page, graphics, (pageWidth / size.widthPoints).toFloat(), (pageHeight / size.heightPoints).toFloat())
        } finally { graphics.dispose() }
        PixelImage(width, height, image.getRGB(0, 0, width, height, null, 0, width))
    }
    actual override fun close(): Unit = synchronized(lock) {
        if (!closed) { closed = true; document.close() }
    }
}
