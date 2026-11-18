package com.kylecorry.trail_sense.tools.offline_maps.infrastructure

import io.github.yuroyami.kitepdf.PdfDocument
import io.github.yuroyami.kitepdf.core.KiteRawApi
import io.github.yuroyami.kitepdf.core.parser.PdfArray
import io.github.yuroyami.kitepdf.core.parser.PdfDictionary
import io.github.yuroyami.kitepdf.core.parser.PdfInt
import io.github.yuroyami.kitepdf.core.parser.PdfName
import io.github.yuroyami.kitepdf.core.parser.PdfObject
import io.github.yuroyami.kitepdf.core.parser.PdfReal
import io.github.yuroyami.kitepdf.core.parser.PdfReference
import io.github.yuroyami.kitepdf.core.parser.PdfString

@OptIn(KiteRawApi::class)
internal actual class PdfGeoMetadata actual constructor(bytes: ByteArray) {
    private val document = PdfDocument.open(bytes)
    actual val pageCount: Int get() = document.pages.size

    actual fun readPage(index: Int): PdfGeoPage {
        val page = document.pages[index]
        val box = page.displayBox
        val pageDictionary = dictionary(page.reference)
        val candidates = resolve(pageDictionary?.get("VP")) as? PdfArray
        val viewports = candidates.orEmpty().mapNotNull { rawViewport ->
            val viewport = dictionary(rawViewport) ?: return@mapNotNull null
            val measure = dictionary(viewport["Measure"]) ?: return@mapNotNull null
            val subtype = (resolve(measure["Subtype"]) as? PdfName)?.value
            if (!subtype.equals("GEO", ignoreCase = true)) return@mapNotNull null
            PdfGeoViewport(
                numbers(viewport["BBox"]), numbers(measure["GPTS"]), numbers(measure["LPTS"]),
                (resolve(dictionary(measure["GCS"])?.get("WKT")) as? PdfString)?.asText()
            )
        }
        return PdfGeoPage(box.left, box.bottom, box.right, box.top, page.rotationNormalized, viewports)
    }

    private fun resolve(value: PdfObject?): PdfObject? =
        if (value is PdfReference) document.resolve(value) else value

    private fun dictionary(value: PdfObject?) = resolve(value) as? PdfDictionary

    private fun numbers(value: PdfObject?): List<Double> {
        val array = resolve(value) as? PdfArray ?: return emptyList()
        return array.map { raw ->
            when (val number = resolve(raw)) {
                is PdfInt -> number.value.toDouble()
                is PdfReal -> number.value
                else -> return emptyList()
            }
        }
    }
}
