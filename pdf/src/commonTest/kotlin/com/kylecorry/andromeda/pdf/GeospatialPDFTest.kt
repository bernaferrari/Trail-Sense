package com.kylecorry.andromeda.pdf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GeospatialPDFTest {
    @Test fun readsViewportCoordinates() {
        val objects = listOf(
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 100 200] /VP [4 0 R] >>",
            "<< /Type /Viewport /BBox [0 0 100 200] /Measure 5 0 R >>",
            "<< /Type /Measure /Subtype /GEO /GPTS [10 20 30 40] /LPTS [0 0 1 1] >>"
        )
        val pdf = StringBuilder("%PDF-1.4\n")
        val offsets = objects.mapIndexed { index, value ->
            val offset = pdf.length
            pdf.append("${index + 1} 0 obj\n$value\nendobj\n")
            offset
        }
        val xref = pdf.length
        pdf.append("xref\n0 ${objects.size + 1}\n0000000000 65535 f \n")
        offsets.forEach { pdf.append("${it.toString().padStart(10, '0')} 00000 n \n") }
        pdf.append("trailer\n<< /Size ${objects.size + 1} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF")
        val metadata = assertNotNull(parseGeospatialPDF(pdf.toString().encodeToByteArray()))
        assertEquals(2, metadata.points.size)
        assertEquals(10.0, metadata.points[0].second.latitude)
        assertEquals(40.0, metadata.points[1].second.longitude)
    }
}
