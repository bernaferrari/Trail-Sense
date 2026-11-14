package com.kylecorry.trail_sense.shared.map_layers.tiles

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/** Coordinates ordinary cache access with whole-layer clear and invalidation. */
interface TileCacheLifecycleLock {
    fun <T> read(block: () -> T): T
    fun <T> write(block: () -> T): T
}

/** A portable lock for hosts that do not provide a read/write implementation. */
class SynchronizedTileCacheLock : TileCacheLifecycleLock {
    private val lock = SynchronizedObject()
    override fun <T> read(block: () -> T): T = synchronized(lock, block)
    override fun <T> write(block: () -> T): T = synchronized(lock, block)
}

/** A layer-local cache that transfers reusable tiles to a host-owned shared cache. */
open class SharedLayerTileCache<T : SharedImageTile<*>>(
    val source: String,
    maxSize: Int,
    private val sharedCache: SharedTileCache<T>,
    createCache: (String, Int, (T) -> Unit) -> SharedTileCache<T>,
    private val recycle: (T) -> Unit,
    private val lifecycleLock: TileCacheLifecycleLock = SynchronizedTileCacheLock()
) : TileLayerCache<T> {
    private val layerCache = createCache(source, maxSize) { tile ->
        if (isCacheable(tile)) sharedCache.store(tile.key, tile) else recycle(tile)
    }

    operator fun get(tile: Tile): T? = layerCache[tile]

    fun peek(tile: Tile): T? {
        val key = layerCache.getKey(tile)
        return layerCache.peek(key) ?: sharedCache.peek(key)
    }

    override fun getOrPut(tiles: List<Tile>, provider: (Tile, String) -> T): List<T> =
        lifecycleLock.read {
            tiles.map { tile ->
                val key = layerCache.getKey(tile)
                layerCache.getOrPut(key) { takeShared(key) ?: provider(tile, key) }
            }
        }

    fun onLoadComplete(tile: T) {
        if (!isCacheable(tile)) sharedCache.removeIfSame(tile.key, tile)
    }

    fun clear() = lifecycleLock.write {
        layerCache.takeAll().forEach(recycle)
        sharedCache.evictOwner(source)
    }

    fun invalidate() = lifecycleLock.write {
        layerCache.snapshot().values.forEach { it.invalidate() }
        sharedCache.invalidateOwner(source)
    }

    override fun resize(maxSize: Int) = lifecycleLock.read { layerCache.resize(maxSize) }
    override fun maxSize(): Int = layerCache.maxSize()
    fun sizeBytes(): Long = layerCache.sizeBytes()
    fun sharedSizeBytes(): Long = sharedCache.sizeBytes()

    private fun takeShared(key: String): T? {
        val tile = sharedCache.take(key) ?: return null
        if (isCacheable(tile)) return tile
        recycle(tile)
        return null
    }

    private fun isCacheable(tile: T): Boolean =
        tile.hasImage() || tile.state == TileState.Loading || tile.state == TileState.Empty
}
