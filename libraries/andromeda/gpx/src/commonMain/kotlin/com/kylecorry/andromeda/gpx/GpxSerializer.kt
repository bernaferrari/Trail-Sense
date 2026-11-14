package com.kylecorry.andromeda.gpx

public class GpxSerializer(private val creator: String) {
    public fun serialize(value: GPXData): ByteArray = GPXParser.toGPX(value, creator).encodeToByteArray()
    public fun deserialize(bytes: ByteArray): GPXData = GPXParser.parse(bytes.decodeToString())
}
