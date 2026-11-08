package com.kylecorry.andromeda.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.InputStream
import java.io.OutputStream


object PdfConvert {

    private val generator = PDFGenerator()
    private val parser = PDFParser()

    fun toPDF(objects: List<PDFValue.PDFObject>): String {
        return generator.toPDF(objects)
    }

    fun toPDF(objects: List<PDFValue.PDFObject>, out: OutputStream) {
        generator.toPDF(objects, out)
    }

    fun fromPDF(input: InputStream, ignoreStreams: Boolean = false): List<PDFValue.PDFObject> {
        return parser.parse(input, ignoreStreams)
    }

    fun toPdf(
        context: Context,
        text: CharSequence,
        out: OutputStream,
        textSize: Float = 6f,
        margins: Int = 50
    ) {
        require(margins >= 0 && margins * 2 < 595 && margins * 2 < 842)
        require(textSize > 0)
        val document = PdfDocument()
        val paint = TextPaint().apply {
            color = Color.BLACK
            this.textSize = textSize * context.resources.displayMetrics.scaledDensity
        }
        val width = 595 - margins * 2
        val height = 842 - margins * 2
        try {
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()
            var firstLine = 0
            var pageNumber = 1
            do {
                var endLine = firstLine
                val top = layout.getLineTop(firstLine)
                while (endLine < layout.lineCount &&
                    (endLine == firstLine || layout.getLineBottom(endLine) - top <= height)) {
                    endLine++
                }
                val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber++).create())
                page.canvas.save()
                page.canvas.clipRect(margins, margins, 595 - margins, 842 - margins)
                page.canvas.translate(margins.toFloat(), (margins - top).toFloat())
                layout.draw(page.canvas)
                page.canvas.restore()
                document.finishPage(page)
                firstLine = endLine
            } while (firstLine < layout.lineCount)
            document.writeTo(out)
        } finally {
            document.close()
        }
    }
}
