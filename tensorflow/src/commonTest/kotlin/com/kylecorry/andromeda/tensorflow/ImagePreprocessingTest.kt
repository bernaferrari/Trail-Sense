package com.kylecorry.andromeda.tensorflow

import com.kylecorry.andromeda.bitmaps.ImageInterpolation
import com.kylecorry.andromeda.bitmaps.PixelImage
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ImagePreprocessingTest {
    @Test fun cropsAndPadsWithoutDistortingAspectRatio() {
        val source = PixelImage(4, 2, intArrayOf(1, 2, 3, 4, 5, 6, 7, 8))
        val pixels = source
        assertContentEquals(intArrayOf(2, 3, 6, 7), pixels.resizeForModel(2, 2, ImageResizeMode.CenterCrop, ImageInterpolation.Nearest).pixels)
        val fitted = pixels.resizeForModel(4, 4, ImageResizeMode.Fit, ImageInterpolation.Nearest, background = -1)
        assertEquals(4, fitted.width)
        assertEquals(4, fitted.height)
        assertContentEquals(intArrayOf(-1, -1, -1, -1, 1, 2, 3, 4, 5, 6, 7, 8, -1, -1, -1, -1), fitted.pixels)
    }
}
