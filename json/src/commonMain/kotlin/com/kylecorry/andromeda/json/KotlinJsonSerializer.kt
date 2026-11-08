package com.kylecorry.andromeda.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

class KotlinJsonSerializer<T>(private val serializer: KSerializer<T>, private val json: Json = JsonConvert.format) {
    fun serialize(value: T): ByteArray = json.encodeToString(serializer, value).encodeToByteArray()
    fun deserialize(bytes: ByteArray): T = json.decodeFromString(serializer, bytes.decodeToString())
}
