package com.kylecorry.trail_sense.shared.map_layers.tiles

import androidx.collection.LruCache
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/** An LRU tile cache that can transfer ownership without disposing a moved tile. */
open class SharedTileCache<T : SharedImageTile<*>>(
    val source: String,
    maxSize: Int,
    private val sizeBytesOf: (T) -> Long,
    private val removalListener: (T) -> Unit
) : LruCache<String, T>(maxSize) {
    private val lock = SynchronizedObject()
    private val entries = ConcurrentTileMap<T>()
    private val transferring = ConcurrentTileMap<T>()

    override fun entryRemoved(evicted: Boolean, key: String, oldValue: T, newValue: T?) {
        super.entryRemoved(evicted, key, oldValue, newValue)
        entries.removeIfSame(key, oldValue)
        if (!transferring.removeIfSame(key, oldValue)) removalListener(oldValue)
    }

    operator fun get(tile: Tile): T? = get(getKey(tile))
    fun peek(key: String): T? = entries[key]

    fun getOrPut(key: String, provider: () -> T): T = synchronized(lock) {
        get(key) ?: provider().also {
            put(key, it)
            entries.put(key, it)
        }
    }

    fun store(key: String, tile: T) = synchronized(lock) {
        put(key, tile)
        entries.put(key, tile)
    }

    fun evictOwner(owner: String) = synchronized(lock) {
        snapshot().filterValues { it.owner == owner }.keys.forEach { remove(it) }
    }

    fun invalidateOwner(owner: String) {
        snapshot().values.filter { it.owner == owner }.forEach { it.invalidate() }
    }

    fun sizeBytes(): Long = snapshot().values.sumOf(sizeBytesOf)

    fun take(key: String): T? = synchronized(lock) {
        val value = get(key) ?: return null
        transferring.put(key, value)
        remove(key)
        value
    }

    fun takeAll(): List<T> = synchronized(lock) {
        snapshot().keys.mapNotNull { key ->
            val value = get(key) ?: return@mapNotNull null
            transferring.put(key, value)
            remove(key)
            value
        }
    }

    fun removeIfSame(key: String, tile: T): Boolean = synchronized(lock) {
        if (entries[key] !== tile) return false
        remove(key)
        true
    }

    fun getKey(tile: Tile): String = "${source}_${tile.x}_${tile.y}_${tile.z}"
}
