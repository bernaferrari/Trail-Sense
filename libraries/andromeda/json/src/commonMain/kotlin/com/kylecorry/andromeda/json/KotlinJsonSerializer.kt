package com.kylecorry.andromeda.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

public class KotlinJsonSerializer<T>(private val serializer: KSerializer<T>, private val json: Json = JsonConvert.format) {
    public fun serialize(value: T): ByteArray = json.encodeToString(serializer, value).encodeToByteArray()
    public fun deserialize(bytes: ByteArray): T = json.decodeFromString(serializer, bytes.decodeToString())
}
