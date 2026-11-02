package com.kylecorry.trail_sense.shared.map_layers.tiles

import android.os.SystemClock
import com.kylecorry.luna.concurrency.Parallel
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.logging.Logger
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapViewProjection
import java.util.PriorityQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.log

class TileQueue {
    // Enable this to log what tiles are being loaded
    private val shouldLog = false
    private var changeListener: (tile: ImageTile) -> Unit = {}
    private val loadingKeys = mutableSetOf<String>()
    private val queuedKeys = mutableSetOf<String>()
    private val loadingCount = AtomicInteger(0)

    private val comparator = compareBy<ImageTile> { getPriority(it) }

    private val queue = LazyPriorityQueue(16, comparator)
    private val delayed = PriorityQueue<ImageTile>(compareBy { it.retryAfterMillis })

    @Volatile
    private var mapProjection: IMapViewProjection? = null

    @Volatile
    private var desiredTiles: Set<Tile>? = null

    fun setMapProjection(projection: IMapViewProjection) {
        mapProjection = projection
        queue.recalculatePriorities()
    }

    fun setDesiredTiles(tiles: List<Tile>) {
        desiredTiles = tiles.toSet()
    }

    fun enqueue(tile: ImageTile) {
        if (!tile.isLoadable() && tile.state != TileState.Error) {
            return
        }
        synchronized(loadingKeys) {
            if (loadingKeys.contains(tile.key)) {
                return
            }
        }

        val shouldEnqueue = synchronized(queuedKeys) {
            queuedKeys.add(tile.key)
        }

        if (shouldEnqueue) {
            if (tile.isLoadable()) {
                queue.enqueue(tile)
            } else {
                synchronized(delayed) { delayed.add(tile) }
            }
        }
    }

    fun clear() {
        queue.clear()
        synchronized(delayed) { delayed.clear() }
        synchronized(queuedKeys) {
            queuedKeys.clear()
        }
        synchronized(loadingKeys) {
            loadingKeys.clear()
            loadingCount.set(0)
        }
    }

    fun count(): Int {
        return queue.count() + synchronized(delayed) { delayed.size }
    }

    private fun dequeue(): ImageTile? {
        val tile = queue.dequeue().firstOrNull()
        if (tile != null) {
            synchronized(queuedKeys) {
                queuedKeys.remove(tile.key)
            }
        }
        return tile
    }

    suspend fun load(maxTotalLoads: Int, maxNewLoads: Int = maxTotalLoads) {
        val jobs = mutableListOf<ImageTile>()
        val tiles = desiredTiles ?: return
        releaseReadyTiles()
        while (loadingCount.get() < maxTotalLoads && jobs.size < maxNewLoads) {
            val tile = dequeue() ?: break
            if (tile.tile !in tiles) {
                // This tile is no longer wanted
                continue
            }

            val key = tile.key
            if (tile.isLoadable()) {
                val shouldLoad = synchronized(loadingKeys) {
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
                val start = if (shouldLog) SystemClock.elapsedRealtime() else 0L
                it.load()
                if (shouldLog) {
                    getAppService<Logger>().debug(
                        "TileQueue",
                        "${it.key} (${SystemClock.elapsedRealtime() - start}ms)",
                        writeToFile = false
                    )
                }
                onStateChange(it)
            }
        } finally {
            synchronized(loadingKeys) {
                jobs.forEach {
                    if (loadingKeys.remove(it.key)) {
                        loadingCount.decrementAndGet()
                    }
                }
            }
        }
    }

    private fun onStateChange(tile: ImageTile) {
        synchronized(loadingKeys) {
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
        val ready = synchronized(delayed) {
            buildList {
                while (delayed.peek()?.isLoadable() == true) {
                    add(delayed.remove())
                }
            }
        }
        ready.forEach { queue.enqueue(it) }
    }

    fun getLoadingCount(): Int {
        return loadingCount.get()
    }

    fun isEmpty(): Boolean {
        return count() == 0 && getLoadingCount() == 0
    }

    private fun getPriority(tile: ImageTile): Double {
        val projection = mapProjection ?: return 0.0
        val center = projection.toPixels(projection.center)
        val tileCenter = projection.toPixels(tile.tile.getCenter())
        val distance = center.distanceTo(tileCenter)
        val resolution = tile.tile.getResolution()
        // https://github.com/openlayers/openlayers/blob/main/src/ol/TileQueue.js
        return 65536 * log(resolution, 10.0) + distance / resolution
    }

    fun setChangeListener(listener: (tile: ImageTile) -> Unit) {
        changeListener = listener
    }

}
