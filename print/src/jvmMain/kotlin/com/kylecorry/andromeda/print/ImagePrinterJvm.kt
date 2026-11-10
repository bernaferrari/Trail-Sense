package com.kylecorry.andromeda.print

import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.awt.print.Printable
import java.awt.print.PageFormat
import java.awt.print.PrinterJob

public fun imagePrinter(): ImagePrinter = ImagePrinter { image, jobName, options ->
    val source = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)
    source.setRGB(0, 0, image.width, image.height, image.pixels, 0, image.width)
    val bitmap = BufferedImage(image.width, image.height, if (options.color == PrintColorMode.Monochrome) BufferedImage.TYPE_BYTE_GRAY else BufferedImage.TYPE_INT_RGB)
    bitmap.createGraphics().let { graphics ->
        try {
            graphics.color = Color.WHITE
            graphics.fillRect(0, 0, image.width, image.height)
            graphics.drawImage(source, 0, 0, null)
        } finally { graphics.dispose() }
    }
    val job = PrinterJob.getPrinterJob()
    job.jobName = jobName
    val format = job.defaultPage().apply {
        orientation = when (options.orientation) {
            PrintOrientation.Auto -> if (image.width > image.height) PageFormat.LANDSCAPE else PageFormat.PORTRAIT
            PrintOrientation.Portrait -> PageFormat.PORTRAIT
            PrintOrientation.Landscape -> PageFormat.LANDSCAPE
        }
    }
    job.setPrintable({ graphics, page, index ->
        if (index != 0) Printable.NO_SUCH_PAGE
        else {
            val target = graphics.create() as Graphics2D
            try {
                val layout = imagePrintLayout(image.width, image.height, page.imageableWidth, page.imageableHeight, options.scale)
                target.clip(java.awt.geom.Rectangle2D.Double(page.imageableX, page.imageableY, page.imageableWidth, page.imageableHeight))
                target.translate(page.imageableX + layout.x, page.imageableY + layout.y)
                target.scale(layout.width / image.width, layout.height / image.height)
                target.drawImage(bitmap, 0, 0, null)
            } finally { target.dispose() }
            Printable.PAGE_EXISTS
        }
    }, format)
    if (job.printDialog()) job.print()
}
