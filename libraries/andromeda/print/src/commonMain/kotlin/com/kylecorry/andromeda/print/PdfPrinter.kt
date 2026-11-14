package com.kylecorry.andromeda.print

/** Opens the system print dialog for PDF data. Invoke on the UI thread. */
public fun interface PdfPrinter {
    public fun print(pdf: ByteArray, jobName: String)
}
