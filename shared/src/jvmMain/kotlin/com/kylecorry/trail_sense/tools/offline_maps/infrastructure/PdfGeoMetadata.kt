package com.kylecorry.trail_sense.tools.offline_maps.infrastructure

import org.apache.pdfbox.Loader
import org.apache.pdfbox.cos.COSArray
import org.apache.pdfbox.cos.COSDictionary
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.cos.COSNumber

internal actual class PdfGeoMetadata actual constructor(private val bytes: ByteArray) {
    actual val pageCount = Loader.loadPDF(bytes).use { it.numberOfPages }

    actual fun readPage(index: Int): PdfGeoPage = Loader.loadPDF(bytes).use { document ->
        val page = document.getPage(index)
        val box = page.cropBox
        val candidates = page.cosObject.getDictionaryObject(COSName.getPDFName("VP")) as? COSArray
        val viewports = (0 until (candidates?.size() ?: 0)).mapNotNull { i ->
            val viewport = candidates?.getObject(i) as? COSDictionary ?: return@mapNotNull null
            val measure = viewport.getDictionaryObject(COSName.getPDFName("Measure")) as? COSDictionary
                ?: return@mapNotNull null
            if (!measure.getNameAsString(COSName.SUBTYPE).equals("GEO", ignoreCase = true)) return@mapNotNull null
            val gcs = measure.getDictionaryObject(COSName.getPDFName("GCS")) as? COSDictionary
            PdfGeoViewport(
                viewport.numbers("BBox"), measure.numbers("GPTS"), measure.numbers("LPTS"),
                gcs?.getString(COSName.getPDFName("WKT"))
            )
        }
        PdfGeoPage(
            box.lowerLeftX.toDouble(), box.lowerLeftY.toDouble(),
            box.upperRightX.toDouble(), box.upperRightY.toDouble(), page.rotation, viewports
        )
    }

    private fun COSDictionary.numbers(key: String): List<Double> {
        val array = getDictionaryObject(COSName.getPDFName(key)) as? COSArray ?: return emptyList()
        return (0 until array.size()).map {
            (array.getObject(it) as? COSNumber)?.floatValue()?.toDouble() ?: return emptyList()
        }
    }
}
