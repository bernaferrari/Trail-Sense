package com.kylecorry.andromeda.pdf

/** Reads geospatial metadata from a PDF document. */
expect fun parseGeospatialPDF(bytes: ByteArray): GeospatialPDFMetadata?
