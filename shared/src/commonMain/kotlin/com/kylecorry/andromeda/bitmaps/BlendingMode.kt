package com.kylecorry.andromeda.bitmaps

/**
 * Determines how a source buffer is blended into a destination buffer.
 * In the descriptions below, ".a" represents the alpha channel.
 */
public enum class BlendingMode(public val value: Int) {
    /**
     * dest = 0
     *
     * The destination is cleared, i.e. each pixel is set to (0, 0, 0, 0)
     */
    CLEAR(0),

    /**
     * dest = src
     *
     * Sets each pixel of the destination to the corresponding one in the source.
     */
    SRC(1),

    /**
     * dest = dest
     *
     * Leaves the destination untouched. This is a no-op.
     */
    DST(2),

    /**
     * dest = src + dest * (1.0 - src.a)
     */
    SRC_OVER(3),

    /**
     * dest = dest + src * (1.0 - dest.a)
     */
    DST_OVER(4),

    /**
     * dest = src * dest.a
     */
    SRC_IN(5),

    /**
     * dest = dest * src.a
     */
    DST_IN(6),

    /**
     * dest = src * (1.0 - dest.a)
     */
    SRC_OUT(7),

    /**
     * dest = dest * (1.0 - src.a)
     */
    DST_OUT(8),

    /**
     * dest.rgb = src.rgb * dest.a + (1.0 - src.a) * dest.rgb, dest.a = dest.a
     */
    SRC_ATOP(9),

    /**
     * dest = dest.rgb * src.a + (1.0 - dest.a) * src.rgb, dest.a = src.a
     */
    DST_ATOP(10),

    /**
     * dest = {src.r ^ dest.r, src.g ^ dest.g, src.b ^ dest.b, src.a ^ dest.a}
     *
     * Note: this is NOT the Porter/Duff XOR mode; this is a bitwise xor.
     */
    XOR(11),

    /**
     * dest = src * dest
     */
    MULTIPLY(12),

    /**
     * dest = min(src + dest, 1.0)
     */
    ADD(13),

    /**
     * dest = max(dest - src, 0.0)
     */
    SUBTRACT(14)
}

/**
 * A translation table used by the lut method. For each potential red, green, blue, and alpha
 * value, specifies it's replacement value.
 *
 * The fields are initialized to be a no-op operation, i.e. replace 1 by 1, 2 by 2, etc.
 * You can modify just the values you're interested in having a translation.
 */
public class LookupTable {
    public var red: ByteArray = ByteArray(256) { it.toByte() }
    public var green: ByteArray = ByteArray(256) { it.toByte() }
    public var blue: ByteArray = ByteArray(256) { it.toByte() }
    public var alpha: ByteArray = ByteArray(256) { it.toByte() }
}

