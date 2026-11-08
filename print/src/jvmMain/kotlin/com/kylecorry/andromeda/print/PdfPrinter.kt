package com.kylecorry.andromeda.print

import java.awt.print.PrinterJob
import org.apache.pdfbox.Loader
import org.apache.pdfbox.printing.PDFPageable

fun pdfPrinter(): PdfPrinter = PdfPrinter { pdf, jobName ->
    Loader.loadPDF(pdf).use { document ->
        val job = PrinterJob.getPrinterJob()
        job.jobName = jobName
        job.setPageable(PDFPageable(document))
        if (job.printDialog()) job.print()
    }
}
