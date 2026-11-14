package com.kylecorry.andromeda.compression

import korlibs.io.compression.deflate.ZLib
import korlibs.io.compression.uncompress

public object Zlib {
    public fun decompress(bytes: ByteArray): ByteArray = bytes.uncompress(ZLib)
}
