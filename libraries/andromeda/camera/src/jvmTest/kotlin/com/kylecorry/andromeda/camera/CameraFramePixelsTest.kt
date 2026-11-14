package com.kylecorry.andromeda.camera

import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals

class CameraFramePixelsTest {
    @Test
    fun extractsArgbPixelsFromCameraImage() {
        val image = BufferedImage(8, 6, BufferedImage.TYPE_4BYTE_ABGR)
        image.setRGB(2, 3, 0xff12ab34.toInt())
        val pixels = cameraFramePixels(image)
        assertEquals(48, pixels.size)
        assertEquals(0xff12ab34.toInt(), pixels[3 * 8 + 2])
    }
}
