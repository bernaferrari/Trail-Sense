package com.kylecorry.andromeda.print

import kotlin.test.Test
import kotlin.test.assertEquals

class ImagePrintLayoutTest {
    @Test fun centersFitAndFillLayouts() {
        assertEquals(ImagePrintLayout(0.0, 25.0, 100.0, 50.0), imagePrintLayout(200, 100, 100.0, 100.0, PrintScaleMode.Fit))
        assertEquals(ImagePrintLayout(-50.0, 0.0, 200.0, 100.0), imagePrintLayout(200, 100, 100.0, 100.0, PrintScaleMode.Fill))
    }
}
