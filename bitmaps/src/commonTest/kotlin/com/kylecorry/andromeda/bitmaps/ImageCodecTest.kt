package com.kylecorry.andromeda.bitmaps

import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ImageCodecTest {
    @Test
    fun readsAndWritesPngInTopToBottomOrder() {
        val png = Base64.decode("iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAYAAABytg0kAAAAE0lEQVR4nGP4z8DwHwyBNIhgAAA/0gX7yxemzgAAAABJRU5ErkJggg==")
        val expected = intArrayOf(0xffff0000.toInt(), 0xff00ff00.toInt(), 0xff0000ff.toInt(), 0)
        val image = decodeImage(png)
        assertEquals(2, image.width)
        assertEquals(2, image.height)
        assertContentEquals(expected, image.pixels)
        assertContentEquals(expected, decodeImage(image.encodePng()).pixels)
        assertEquals(2, decodeImage(image.encodeJpeg(90)).width)
        assertFailsWith<IllegalArgumentException> { image.encodeJpeg(101) }
    }
}
