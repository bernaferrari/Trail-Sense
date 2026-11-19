package com.kylecorry.trail_sense.tools.offline_maps.infrastructure

import com.kylecorry.andromeda.pdf.PdfPages
import com.kylecorry.andromeda.pdf.catalog
import com.kylecorry.andromeda.pdf.createPDF
import com.kylecorry.andromeda.pdf.page
import com.kylecorry.andromeda.pdf.pages
import com.kylecorry.andromeda.pdf.stream
import kotlin.test.Test
import kotlin.test.assertTrue

class PdfRegionRenderTest {
    @Test
    fun rendersOnlyTheRequestedPartOfThePage() {
        val pdf = createPDF(listOf(
            catalog(1, 2),
            pages(2, listOf(3)),
            page(3, 2, 200, 100, listOf(4)),
            stream(4, """
                1 0 0 rg 0 0 200 100 re f
                0 1 0 rg 100 50 100 50 re f
            """.trimIndent())
        ))
        PdfPages(pdf).use { document ->
            val green = 0xff00ff00.toInt()
            val red = 0xffff0000.toInt()
            val topRight = document.renderRegion(0, 64, 32, 0.6, 0.1, 0.9, 0.4)
            val bottomRight = document.renderRegion(0, 64, 32, 0.6, 0.6, 0.9, 0.9)
            assertTrue(topRight.pixels.all { it == green })
            assertTrue(bottomRight.pixels.all { it == red })
        }
    }
}
