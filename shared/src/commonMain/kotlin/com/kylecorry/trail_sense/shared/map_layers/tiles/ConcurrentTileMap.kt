package com.kylecorry.trail_sense.shared.map_layers.tiles

/** A concurrent side index for cache lookups that must not change LRU order. */
internal expect class ConcurrentTileMap<T : Any>() {
    operator fun get(key: String): T?
    fun put(key: String, value: T)
    fun removeIfSame(key: String, value: T): Boolean
}
