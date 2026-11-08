package com.kylecorry.andromeda.pdf

import io.github.yuroyami.kitepdf.PdfDocument
import io.github.yuroyami.kitepdf.core.KiteRawApi
import io.github.yuroyami.kitepdf.core.parser.*
import io.github.yuroyami.kitepdf.parser.XrefEntry

@OptIn(KiteRawApi::class)
actual fun parsePDF(bytes: ByteArray, ignoreStreams: Boolean): List<PDFValue.PDFObject> {
    val document = PdfDocument.open(bytes)
    fun convert(value: PdfObject): PDFValue = when (value) {
        is PdfBoolean -> PDFValue.PDFBoolean(value.value)
        is PdfInt -> PDFValue.PDFNumber(value.value)
        is PdfReal -> PDFValue.PDFNumber(value.value)
        is PdfString -> PDFValue.PDFString(value.asText())
        is PdfName -> PDFValue.PDFName(value.value)
        is PdfArray -> PDFValue.PDFArray(value.items.map(::convert))
        is PdfDictionary -> PDFValue.PDFDictionary(value.map.mapKeys { PDFValue.PDFName(it.key) }.mapValues { convert(it.value) })
        is PdfReference -> PDFValue.PDFIndirectObject(value.objectNumber.toInt(), value.generation)
        is PdfStream -> PDFValue.PDFStream(value.rawBytes)
        PdfNull -> PDFValue.PDFNull()
    }
    return document.xref.values.mapNotNull { entry ->
        if (entry is XrefEntry.Free || entry.objectNumber > Int.MAX_VALUE) return@mapNotNull null
        val generation = (entry as? XrefEntry.InUse)?.generation ?: 0
        val value = document.resolve(PdfReference(entry.objectNumber, generation)) ?: return@mapNotNull null
        val content = if (value is PdfStream) {
            listOf(convert(value.dict)) + if (ignoreStreams) emptyList() else listOf(convert(value))
        } else listOf(convert(value))
        PDFValue.PDFObject(entry.objectNumber.toInt(), generation, content)
    }.sortedBy { it.id }
}
