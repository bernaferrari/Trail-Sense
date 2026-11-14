package com.kylecorry.trail_sense.shared.map_layers.tiles

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/** Android's shared bitmap cache and asynchronous recycling. */
class LayerTileCache(source: String, maxSize: Int) : SharedLayerTileCache<ImageTile>(
    source = source,
    maxSize = maxSize,
    sharedCache = sharedCache,
    createCache = { cacheSource, size, onRemove -> TileCache(cacheSource, size, onRemove) },
    recycle = { recycle(it) },
    lifecycleLock = AndroidTileCacheLifecycleLock()
) {
    companion object {
        private val recycleScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private val sharedCache = TileCache("", SHARED_CACHE_SIZE) { recycle(it) }
        private const val SHARED_CACHE_SIZE = 256

        private fun recycle(tile: ImageTile) {
            recycleScope.launch { tile.recycle() }
        }
    }
}

private class AndroidTileCacheLifecycleLock : TileCacheLifecycleLock {
    private val lock = ReentrantReadWriteLock()
    override fun <T> read(block: () -> T): T = lock.read(block)
    override fun <T> write(block: () -> T): T = lock.write(block)
}
