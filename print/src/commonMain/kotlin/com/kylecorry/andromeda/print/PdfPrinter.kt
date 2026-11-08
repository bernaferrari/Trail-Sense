package com.kylecorry.andromeda.print

/** Opens the system print dialog for PDF data. Invoke on the UI thread. */
fun interface PdfPrinter {
    fun print(pdf: ByteArray, jobName: String)
}
