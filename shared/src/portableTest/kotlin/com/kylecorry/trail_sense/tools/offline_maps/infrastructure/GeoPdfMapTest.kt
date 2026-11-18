package com.kylecorry.trail_sense.tools.offline_maps.infrastructure

import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PercentCoordinate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeoPdfMapTest {
    @Test
    fun calibrationComesFromTheSelectedPageAndUsesItsCropAndRotation() {
        val document = GeoPdfMap("survey", makePdf(listOf(
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R 4 0 R] /Count 2 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 100 200] >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 100 200] /CropBox [10 20 90 180] /Rotate 90 /VP [5 0 R] >>",
            "<< /Type /Viewport /BBox [10 20 90 180] /Measure 6 0 R >>",
            "<< /Type /Measure /Subtype /GEO /GPTS [10 20 11 21] /LPTS [0 0 1 1] >>"
        )))

        assertEquals(2, document.pageCount)
        assertTrue(document.georeference(0, Size(100f, 200f)).calibrationPoints.isEmpty())
        val points = document.georeference(1, Size(160f, 80f)).calibrationPoints
        assertEquals(listOf(Coordinate(10.0, 20.0), Coordinate(11.0, 21.0)), points.map { it.location })
        assertEquals(listOf(PercentCoordinate(0f, 0f), PercentCoordinate(1f, 1f)), points.map { it.imageLocation })
    }

    private fun makePdf(objects: List<String>): ByteArray {
        val pdf = StringBuilder("%PDF-1.4\n")
        val offsets = objects.mapIndexed { index, value ->
            pdf.length.also { pdf.append("${index + 1} 0 obj\n$value\nendobj\n") }
        }
        val xref = pdf.length
        pdf.append("xref\n0 ${objects.size + 1}\n0000000000 65535 f \n")
        offsets.forEach { pdf.append("${it.toString().padStart(10, '0')} 00000 n \n") }
        pdf.append("trailer\n<< /Size ${objects.size + 1} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF")
        return pdf.toString().encodeToByteArray()
    }
}
