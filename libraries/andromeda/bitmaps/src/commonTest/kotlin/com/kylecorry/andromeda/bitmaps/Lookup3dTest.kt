package com.kylecorry.andromeda.bitmaps

import kotlin.test.Test
import kotlin.test.assertContentEquals

class Lookup3dTest {
    @Test
    fun identityCubePreservesRgbAndInputAlpha() {
        val cube = Rgba3dArray(ByteArray(32), 2, 2, 2)
        for (z in 0..1) for (y in 0..1) for (x in 0..1) {
            cube[x, y, z] = byteArrayOf((x * 255).toByte(), (y * 255).toByte(), (z * 255).toByte(), 0)
        }
        val image = PixelImage(3, 1, intArrayOf(0xff123456.toInt(), 0x80ffffff.toInt(), 0x00000000))
        assertContentEquals(image.pixels, image.lookup3d(cube).pixels)
    }
}
