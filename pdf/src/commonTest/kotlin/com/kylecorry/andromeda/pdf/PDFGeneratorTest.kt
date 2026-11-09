package com.kylecorry.andromeda.pdf

import kotlin.test.Test
import kotlin.test.assertTrue

class PDFGeneratorTest {
    @Test fun generatesPortableTextAndImageStreams() {
        val jpeg = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 1, 0xff.toByte(), 0xd9.toByte())
        val objects = listOf(catalog(1, 2), pages(2, listOf(3)), page(3, 2, 100, 100, listOf(4, 5)),
            text(4, "A(B)\\C"), jpg(5, jpeg, 1, 1))
        val pdf = createPDF(objects)
        assertTrue(pdf.decodeToString().contains("A\\(B\\)\\\\C"))
        val parsed = parsePDF(pdf)
        assertTrue(parsed.any { it.id == 5 })
        assertTrue(pdf.toList().windowed(jpeg.size).any { it == jpeg.toList() })
    }
}
