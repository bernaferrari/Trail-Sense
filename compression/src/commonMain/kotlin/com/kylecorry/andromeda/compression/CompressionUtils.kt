package com.kylecorry.andromeda.compression

public object CompressionUtils {
    public fun getBytes(bytes: ByteArray, line: Int, bytesPerLine: Int, lineCount: Int = 1): ByteArray? {
        val start = line.toLong() * bytesPerLine
        val count = bytesPerLine.toLong() * lineCount.coerceAtLeast(1)
        if (start < 0 || count < 0 || start + count > bytes.size) return null
        return bytes.copyOfRange(start.toInt(), (start + count).toInt())
    }
    @OptIn(ExperimentalUnsignedTypes::class)
    public fun getShort(bytes: ByteArray, line: Int): Short? = getBytes(bytes, line, Short.SIZE_BYTES)?.toShort()
    @OptIn(ExperimentalUnsignedTypes::class)
    public fun getInt(bytes: ByteArray, line: Int): Int? = getBytes(bytes, line, Int.SIZE_BYTES)?.toInt()
    public fun getByte(bytes: ByteArray, line: Int, numBits: Int = Byte.SIZE_BITS): Byte? =
        bytes.bitsToByte(numBits, line * numBits)
}
