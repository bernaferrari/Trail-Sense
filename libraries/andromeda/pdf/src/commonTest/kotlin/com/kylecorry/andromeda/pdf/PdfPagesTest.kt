package com.kylecorry.andromeda.pdf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PdfPagesTest {
    @Test fun rendersPagesAndCloses() {
        val pdf = createPDF(listOf(catalog(1, 2), pages(2, listOf(3)), page(3, 2, 100, 100, listOf(4)),
            stream(4, "1 0 0 rg 0 0 50 100 re f")))
        val document = PdfPages(pdf)
        assertEquals(1, document.pageCount)
        assertEquals(PdfPageSize(100f, 100f), document.pageSize(0))
        val image = document.render(0, 40, 40)
        assertTrue((image[10, 20] and 0x00ff0000) > 0x00f00000)
        assertEquals(-1, image[30, 20])
        document.close()
        assertFailsWith<IllegalStateException> { document.pageCount }
    }
}
