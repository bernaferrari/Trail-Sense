package com.kylecorry.andromeda.tensorflow

import com.kylecorry.andromeda.bitmaps.PixelImage

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImageTensorsTest {
    @Test fun convertsRgbAndQuantizedTensors() {
        val image = PixelImage(1, 1, intArrayOf(0xff0080ff.toInt()))
        assertContentEquals(byteArrayOf(0, -128, -1), image.rgbUint8())
        assertEquals(listOf(-1f, 0f, 127f / 128f), decodeFloat32Tensor(image.rgbFloat32(128f, 128f)))
        val quantized = image.rgbQuantized(scale = 1f, zeroPoint = -128, signed = true)
        assertContentEquals(byteArrayOf(-128, 0, 127), quantized)
        assertEquals(listOf(0f, 128f, 255f), decodeQuantizedTensor(quantized, 1f, -128, signed = true))
        val probabilities = listOf(1000f, 1000f).softmax()
        assertTrue(probabilities.all { it == 0.5f })
    }
}
