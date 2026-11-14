package com.kylecorry.trail_sense.shared.map_layers.tiles

import com.kylecorry.luna.concurrency.Parallel
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapViewProjection
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.math.log

interface QueuedTile {
    val key: String
    val tile: Tile
    val state: TileState
    val retryAfterMillis: Long
    fun isLoadable(): Boolean
    suspend fun load()
}

enum class TileState { Idle, Loading, Loaded, Error, Empty, Stale }

class SharedTileQueue<T : QueuedTile> {
    private var changeListener: (tile: T) -> Unit = {}
    private val loadingKeys = mutableSetOf<String>()
    private val queuedKeys = mutableSetOf<String>()
    private val loadingCount = atomic(0)

    private val comparator = compareBy<T> { getPriority(it) }

    private val queue = LazyPriorityQueue(16, comparator)
    private val delayed = LazyPriorityQueue<T>(16, compareBy { it.retryAfterMillis })
    private val loadingLock = SynchronizedObject()
    private val queuedLock = SynchronizedObject()
    private val delayedLock = SynchronizedObject()

    private val mapProjection = atomic<IMapViewProjection?>(null)
    private val desiredTiles = atomic<Set<Tile>?>(null)

    fun setMapProjection(projection: IMapViewProjection) {
        mapProjection.value = projection
        queue.recalculatePriorities()
    }

    fun setDesiredTiles(tiles: List<Tile>) {
        desiredTiles.value = tiles.toSet()
    }

    fun enqueue(tile: T) {
        if (!tile.isLoadable() && tile.state != TileState.Error) {
            return
        }
        synchronized(loadingLock) {
            if (loadingKeys.contains(tile.key)) {
                return
            }
        }

        val shouldEnqueue = synchronized(queuedLock) {
            queuedKeys.add(tile.key)
        }

        if (shouldEnqueue) {
            if (tile.isLoadable()) {
                queue.enqueue(tile)
            } else {
                synchronized(delayedLock) { delayed.enqueue(tile) }
            }
        }
    }

    fun clear() {
        queue.clear()
        synchronized(delayedLock) { delayed.clear() }
        synchronized(queuedLock) {
            queuedKeys.clear()
        }
        synchronized(loadingLock) {
            loadingKeys.clear()
            loadingCount.value = 0
        }
    }

    fun count(): Int {
        return queue.count() + synchronized(delayedLock) { delayed.count() }
    }

    private fun dequeue(): T? {
        val tile = queue.dequeue().firstOrNull()
        if (tile != null) {
            synchronized(queuedLock) {
                queuedKeys.remove(tile.key)
            }
        }
        return tile
    }

    suspend fun load(maxTotalLoads: Int, maxNewLoads: Int = maxTotalLoads) {
        val jobs = mutableListOf<T>()
        val tiles = desiredTiles.value ?: return
        releaseReadyTiles()
        while (loadingCount.value < maxTotalLoads && jobs.size < maxNewLoads) {
            val tile = dequeue() ?: break
            if (tile.tile !in tiles) {
                // This tile is no longer wanted
                continue
            }

            val key = tile.key
            if (tile.isLoadable()) {
                val shouldLoad = synchronized(loadingLock) {
                    loadingKeys.add(key).also {
                        if (it) {
                            loadingCount.incrementAndGet()
                        }
                    }
                }
                if (shouldLoad) {
                    jobs.add(tile)
                }
            }
        }
        // TODO: Allow cancellation of a job - likely means storing the actual jobs somewhere
        try {
            Parallel.forEach(jobs) {
                it.load()
                onStateChange(it)
            }
        } finally {
            synchronized(loadingLock) {
                jobs.forEach {
                    if (loadingKeys.remove(it.key)) {
                        loadingCount.decrementAndGet()
                    }
                }
            }
        }
    }

    private fun onStateChange(tile: T) {
        synchronized(loadingLock) {
            if (loadingKeys.remove(tile.key)) {
                loadingCount.decrementAndGet()
            }
        }
        if (tile.state == TileState.Error) {
            enqueue(tile)
        }
        changeListener(tile)
    }

    private fun releaseReadyTiles() {
        val ready = synchronized(delayedLock) {
            buildList {
                while (delayed.peek()?.isLoadable() == true) {
                    add(delayed.dequeue().first())
                }
            }
        }
        ready.forEach { queue.enqueue(it) }
    }

    fun getLoadingCount(): Int {
        return loadingCount.value
    }

    fun isEmpty(): Boolean {
        return count() == 0 && getLoadingCount() == 0
    }

    private fun getPriority(tile: T): Double {
        val projection = mapProjection.value ?: return 0.0
        val center = projection.toPixels(projection.center)
        val tileCenter = projection.toPixels(tile.tile.getCenter())
        val distance = center.distanceTo(tileCenter)
        val resolution = tile.tile.getResolution()
        // https://github.com/openlayers/openlayers/blob/main/src/ol/TileQueue.js
        return 65536 * log(resolution, 10.0) + distance / resolution
    }

    fun setChangeListener(listener: (tile: T) -> Unit) {
        changeListener = listener
    }

}
