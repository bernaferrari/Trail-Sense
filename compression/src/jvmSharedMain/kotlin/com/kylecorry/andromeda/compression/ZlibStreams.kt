package com.kylecorry.andromeda.compression

import java.io.InputStream
import java.io.OutputStream
import java.util.zip.InflaterInputStream

fun Zlib.decompress(input: InputStream, output: OutputStream, closeOutput: Boolean = false) {
    try { InflaterInputStream(input).use { it.copyTo(output) } } finally { if (closeOutput) output.close() }
}
fun Zlib.decompress(input: InputStream): ByteArray = InflaterInputStream(input).use { it.readBytes() }
