package com.kylecorry.andromeda.gpx

class GpxSerializer(private val creator: String) {
    fun serialize(value: GPXData): ByteArray = GPXParser.toGPX(value, creator).encodeToByteArray()
    fun deserialize(bytes: ByteArray): GPXData = GPXParser.parse(bytes.decodeToString())
}
