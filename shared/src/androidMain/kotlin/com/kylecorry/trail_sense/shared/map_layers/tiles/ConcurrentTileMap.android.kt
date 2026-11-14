package com.kylecorry.trail_sense.shared.map_layers.tiles

import java.util.concurrent.ConcurrentHashMap

internal actual class ConcurrentTileMap<T : Any> actual constructor() {
    private val values = ConcurrentHashMap<String, T>()
    actual operator fun get(key: String): T? = values[key]
    actual fun put(key: String, value: T) { values[key] = value }
    actual fun removeIfSame(key: String, value: T): Boolean = values.remove(key, value)
}
