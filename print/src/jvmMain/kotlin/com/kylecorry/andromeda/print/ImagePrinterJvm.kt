package com.kylecorry.andromeda.print

import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.awt.print.Printable
import java.awt.print.PrinterJob
import kotlin.math.min

fun imagePrinter(): ImagePrinter = ImagePrinter { image, jobName ->
    val bitmap = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)
    bitmap.setRGB(0, 0, image.width, image.height, image.pixels, 0, image.width)
    val job = PrinterJob.getPrinterJob()
    job.jobName = jobName
    job.setPrintable { graphics, page, index ->
        if (index != 0) Printable.NO_SUCH_PAGE
        else {
            val target = graphics.create() as Graphics2D
            try {
                val scale = min(page.imageableWidth / image.width, page.imageableHeight / image.height)
                target.translate(page.imageableX + (page.imageableWidth - image.width * scale) / 2,
                    page.imageableY + (page.imageableHeight - image.height * scale) / 2)
                target.scale(scale, scale)
                target.drawImage(bitmap, 0, 0, null)
            } finally { target.dispose() }
            Printable.PAGE_EXISTS
        }
    }
    if (job.printDialog()) job.print()
}
