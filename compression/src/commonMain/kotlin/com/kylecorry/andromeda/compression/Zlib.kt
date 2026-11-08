package com.kylecorry.andromeda.compression

import korlibs.io.compression.deflate.ZLib
import korlibs.io.compression.uncompress

object Zlib {
    fun decompress(bytes: ByteArray): ByteArray = bytes.uncompress(ZLib)
}
