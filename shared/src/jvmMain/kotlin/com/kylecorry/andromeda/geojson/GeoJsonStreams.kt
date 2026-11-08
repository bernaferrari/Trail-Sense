package com.kylecorry.andromeda.geojson

import java.io.InputStream
import java.io.OutputStream

fun GeoJsonConvert.fromJson(stream: InputStream): GeoJsonObject? = fromJson(stream.bufferedReader().readText())
fun GeoJsonConvert.toJson(obj: GeoJsonObject, stream: OutputStream) {
    stream.write(toJson(obj).toByteArray())
    stream.flush()
}
