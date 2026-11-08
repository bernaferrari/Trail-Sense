package com.kylecorry.andromeda.json

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object JsonConvert {
    val format = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    inline fun <reified T> toJson(value: T): String = format.encodeToString(value)
    inline fun <reified T> fromJson(json: String): T? = runCatching { format.decodeFromString<T>(json) }.getOrNull()
}

inline fun <reified T> T.toJson(): String = JsonConvert.toJson(this)
inline fun <reified T> T.toJsonBytes(): ByteArray = toJson().encodeToByteArray()
inline fun <reified T> ByteArray.fromJson(): T? = JsonConvert.fromJson(decodeToString())
