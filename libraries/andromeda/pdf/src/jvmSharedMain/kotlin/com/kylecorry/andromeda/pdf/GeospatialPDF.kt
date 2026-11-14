package com.kylecorry.andromeda.pdf

public actual fun parseGeospatialPDF(bytes: ByteArray): GeospatialPDFMetadata? =
    bytes.inputStream().use { GeospatialPDFParser().parse(it) }
