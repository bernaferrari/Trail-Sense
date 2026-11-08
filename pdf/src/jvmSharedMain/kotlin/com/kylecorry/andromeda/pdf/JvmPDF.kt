package com.kylecorry.andromeda.pdf

actual fun parsePDF(bytes: ByteArray, ignoreStreams: Boolean): List<PDFValue.PDFObject> =
    bytes.inputStream().use { PDFParser().parse(it, ignoreStreams) }
