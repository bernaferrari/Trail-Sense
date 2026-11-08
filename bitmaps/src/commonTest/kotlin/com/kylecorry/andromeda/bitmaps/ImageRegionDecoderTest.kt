package com.kylecorry.andromeda.bitmaps

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ImageRegionDecoderTest {
    @Test fun decodesMultipleRegionsAndEnforcesBounds() {
        val pixels = intArrayOf(0xff112233.toInt(), 0xff445566.toInt(), 0xff778899.toInt(),
            0xffaabbcc.toInt(), 0xffddeeff.toInt(), 0xff102030.toInt())
        val decoder = ImageRegionDecoder(PixelImage(3, 2, pixels).encodePng())
        assertEquals(3, decoder.width)
        assertEquals(2, decoder.height)
        assertContentEquals(intArrayOf(pixels[1], pixels[2], pixels[4], pixels[5]), decoder.decode(1, 0, 2, 2).pixels)
        assertContentEquals(intArrayOf(pixels[3], pixels[4]), decoder.decode(0, 1, 2, 1).pixels)
        assertContentEquals(intArrayOf(pixels[2], pixels[0], pixels[1], pixels[2], pixels[0]),
            decoder.decodeWrappedRegion(-1, 0, 5, 1, wrapHorizontally = true).pixels)
        assertContentEquals(intArrayOf(0, 0, pixels[0], pixels[1]),
            decoder.decodeWrappedRegion(-2, 0, 4, 1).pixels)
        assertFailsWith<IllegalArgumentException> { decoder.decode(2, 0, 2, 1) }
        decoder.close()
        assertFailsWith<IllegalStateException> { decoder.decode(0, 0, 1, 1) }
    }
}
