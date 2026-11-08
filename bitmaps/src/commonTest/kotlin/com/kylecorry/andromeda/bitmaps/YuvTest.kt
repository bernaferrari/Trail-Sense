package com.kylecorry.andromeda.bitmaps

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class YuvTest {
    @Test
    fun convertsInterleavedAndAlignedPlanarFrames() {
        val nv21 = byteArrayOf(82, 82, 82, 82, 240.toByte(), 90)
        val yv12 = ByteArray(64).apply {
            this[0] = 82; this[1] = 82; this[16] = 82; this[17] = 82
            this[32] = 240.toByte(); this[48] = 90
        }
        val expected = IntArray(4) { 0xffff0100.toInt() }
        assertContentEquals(expected, nv21.yuvToPixelImage(2, 2, YuvFormat.NV21).pixels)
        assertContentEquals(expected, yv12.yuvToPixelImage(2, 2, YuvFormat.YV12).pixels)
        assertFailsWith<IllegalArgumentException> { nv21.yuvToPixelImage(4, 2, YuvFormat.NV21) }
    }
}
