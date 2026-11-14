package com.kylecorry.andromeda.pdf

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.wkt.CRSWellKnownTextConvert
import com.kylecorry.andromeda.wkt.WKTNumber
import com.kylecorry.andromeda.wkt.WKTString
import com.kylecorry.andromeda.wkt.getSection
import com.kylecorry.sol.units.Coordinate
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
import kotlin.math.absoluteValue

@OptIn(KiteRawApi::class)
public actual fun parseGeospatialPDF(bytes: ByteArray): GeospatialPDFMetadata? {
    val document = PdfDocument.open(bytes)
    fun resolve(value: PdfObject?): PdfObject? = if (value is PdfReference) document.resolve(value) else value
    fun dictionary(value: PdfObject?): PdfDictionary? = resolve(value) as? PdfDictionary
    fun numbers(value: PdfObject?): List<Double> = (resolve(value) as? PdfArray)?.mapNotNull {
        when (val number = resolve(it)) {
            is PdfInt -> number.value.toDouble()
            is PdfReal -> number.value
            else -> null
        }
    } ?: emptyList()
    fun projection(text: String): ProjectedCoordinateSystem? {
        val wkt = CRSWellKnownTextConvert.toWKT(text) ?: return null
        val name = wkt.getSection("projection")?.get<WKTString>(0)?.value ?: return null
        val datum = wkt.getSection("datum")?.get<WKTString>(0)?.value ?: "WGS 84"
        val spheroid = wkt.getSection("spheroid")
        val prime = wkt.getSection("primem")?.get<WKTNumber>(1)?.value ?: 0.0
        return ProjectedCoordinateSystem(GeographicCoordinateSystem(Datum(datum, Spheroid(
            spheroid?.get<WKTString>(0)?.value ?: "WGS 84",
            spheroid?.get<WKTNumber>(1)?.value?.toFloat() ?: 6378137f,
            spheroid?.get<WKTNumber>(2)?.value?.toFloat() ?: 298.2572229f
        )), prime), name)
    }
    for (page in document.pages) {
        val pageDict = dictionary(page.reference) ?: continue
        val pageTop = page.mediaBox.top
        val viewportArray = resolve(pageDict["VP"]) as? PdfArray ?: continue
        for (rawViewport in viewportArray) {
            val viewport = dictionary(rawViewport) ?: continue
            val measure = dictionary(viewport["Measure"]) ?: continue
            if (!(resolve(measure["Subtype"]) as? PdfName)?.value.equals("GEO", ignoreCase = true)) continue
            val bbox = numbers(viewport["BBox"])
            val gpts = numbers(measure["GPTS"])
            var lpts = numbers(measure["LPTS"])
            if (bbox.size < 4 || gpts.isEmpty()) continue
            if (lpts.size < gpts.size) lpts = listOf(0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0)
            if (lpts.size < gpts.size) continue
            val left = bbox[0]
            val top = pageTop - bbox[if (bbox[1] > bbox[3]) 1 else 3]
            val width = (bbox[2] - bbox[0]).absoluteValue
            val height = (bbox[3] - bbox[1]).absoluteValue
            val points = (1 until gpts.size step 2).map { i ->
                PixelCoordinate((lpts[i - 1] * width + left).toFloat(), (lpts[i] * height + top).toFloat()) to
                    Coordinate(gpts[i - 1], gpts[i])
            }
            val gcs = dictionary(measure["GCS"])
            val wkt = (resolve(gcs?.get("WKT")) as? PdfString)?.asText()
            return GeospatialPDFMetadata(points, wkt?.let(::projection))
        }
    }
    return null
}
