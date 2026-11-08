package com.kylecorry.andromeda.pdf

actual fun parseGeospatialPDF(bytes: ByteArray): GeospatialPDFMetadata? =
    bytes.inputStream().use { GeospatialPDFParser().parse(it) }
