package com.kylecorry.andromeda.json

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

public object JsonConvert {
    public val format: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    public inline fun <reified T> toJson(value: T): String = format.encodeToString(value)
    public inline fun <reified T> fromJson(json: String): T? = runCatching { format.decodeFromString<T>(json) }.getOrNull()
}

public inline fun <reified T> T.toJson(): String = JsonConvert.toJson(this)
public inline fun <reified T> T.toJsonBytes(): ByteArray = toJson().encodeToByteArray()
public inline fun <reified T> ByteArray.fromJson(): T? = JsonConvert.fromJson(decodeToString())
