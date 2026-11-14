package com.kylecorry.trail_sense.shared.map_layers.tiles

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MapLayerParams
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MapLayerValues
import kotlin.time.Instant

/** Cache operations needed to schedule tiles for the current viewport. */
interface TileLayerCache<T> {
    fun maxSize(): Int
    fun resize(maxSize: Int)
    fun getOrPut(tiles: List<Tile>, provider: (tile: Tile, key: String) -> T): List<T>
}

/** Keeps viewport tile requests, cache sizing, and queue loaders in sync. */
class TileLoadCoordinator<Image, T : SharedImageTile<Image>>(
    private val cache: TileLayerCache<T>,
    private val queue: SharedTileQueue<T>,
    private val createTile: (Tile, String, Boolean, suspend () -> Image?) -> T,
    private val loadTile: suspend (Tile, MapLayerValues) -> Image?,
    private val largeCacheSize: Int = 256
) {
    fun loadTiles(
        tiles: List<Tile>,
        time: Instant,
        preferences: MapLayerValues,
        featureId: String?,
        isWidget: Boolean,
        isHighDetailMode: Boolean,
        useLargeCache: Boolean
    ) {
        val screenCacheSize = (tiles.size * 2).coerceAtLeast(1)
        val cacheSize = if (useLargeCache) maxOf(largeCacheSize, screenCacheSize) else screenCacheSize
        if (cache.maxSize() != cacheSize) cache.resize(cacheSize)

        val params = MapLayerValues.of(
            MapLayerParams.PARAM_TIME to time.toEpochMilliseconds(),
            MapLayerParams.PARAM_IS_WIDGET to isWidget,
            MapLayerParams.PARAM_HIGH_DETAIL_MODE to isHighDetailMode,
            MapLayerParams.PARAM_PREFERENCES to preferences,
            MapLayerParams.PARAM_FEATURE_ID to featureId
        )
        val imageTiles = cache.getOrPut(tiles) { tile, key ->
            createTile(tile, key, !isWidget) { loadTile(tile, params) }
        }
        imageTiles.forEach { imageTile ->
            imageTile.setLoader { loadTile(imageTile.tile, params) }
        }
        imageTiles.forEach { queue.enqueue(it) }
    }
}
