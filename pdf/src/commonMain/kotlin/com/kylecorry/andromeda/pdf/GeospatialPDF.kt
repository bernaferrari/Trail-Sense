package com.kylecorry.andromeda.pdf

/** Reads geospatial metadata from a PDF document. */
public expect fun parseGeospatialPDF(bytes: ByteArray): GeospatialPDFMetadata?
