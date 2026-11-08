package com.kylecorry.andromeda.gpx

import java.io.InputStream
import java.io.OutputStream

fun GPXParser.parse(stream: InputStream): GPXData = parse(stream.bufferedReader().readText())
fun GPXParser.toGPX(data: GPXData, creator: String, stream: OutputStream) {
    stream.write(toGPX(data, creator).toByteArray())
}
