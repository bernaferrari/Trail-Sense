package com.kylecorry.andromeda.compression

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ByteArrayExtensionsTest {
    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun readsBigEndianInt() {
        assertEquals(0x12345678, byteArrayOf(0x12, 0x34, 0x56, 0x78).toInt())
        assertEquals(-1, ByteArray(4) { -1 }.toInt())
    }

    @Test
    fun readsAcrossByteBoundariesAndRejectsInvalidOffsets() {
        val bytes = byteArrayOf(0x35, 0x60)
        assertEquals(0x56, bytes.bitsToInt(8, 4))
        assertNull(bytes.bitsToInt(9, 8))
        assertThrows(IllegalArgumentException::class.java) { bytes.bitsToInt(1, -1) }
    }
}
