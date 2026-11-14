package com.kylecorry.trail_sense.shared.map_layers.tiles

import android.graphics.Bitmap
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MapLayerParams
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MapLayerValues
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Instant

class TileLoadCoordinatorTest {
    @Test
    fun queuedTileUsesTheLatestParametersAndResizesTheCache() = runBlocking {
        val cache = TestCache()
        val queue = SharedTileQueue<ImageTile>()
        var loadedParams: MapLayerValues? = null
        val coordinator = TileLoadCoordinator<Bitmap, ImageTile>(
            cache, queue,
            createTile = { tile, key, fade, load ->
                ImageTile(key, tile, shouldFadeIn = fade, loadFunction = load)
            },
            loadTile = { _, params -> loadedParams = params; null }
        )
        val tile = Tile(0, 0, 1)
        queue.setDesiredTiles(listOf(tile))
        coordinator.loadTiles(
            listOf(tile), Instant.fromEpochMilliseconds(100), MapLayerValues.Empty, null, false, false, false
        )
        coordinator.loadTiles(
            listOf(tile), Instant.fromEpochMilliseconds(200), MapLayerValues.Empty, "new", true, true, true
        )

        queue.load(1)

        assertEquals(256, cache.maxSize())
        assertEquals(200L, loadedParams?.getLong(MapLayerParams.PARAM_TIME))
        assertEquals("new", loadedParams?.getString(MapLayerParams.PARAM_FEATURE_ID))
        assertTrue(loadedParams?.getBoolean(MapLayerParams.PARAM_IS_WIDGET) == true)
        assertTrue(loadedParams?.getBoolean(MapLayerParams.PARAM_HIGH_DETAIL_MODE) == true)
    }

    private class TestCache : TileLayerCache<ImageTile> {
        private val tiles = mutableMapOf<Tile, ImageTile>()
        private var limit = 1
        override fun maxSize() = limit
        override fun resize(maxSize: Int) { limit = maxSize }
        override fun getOrPut(tiles: List<Tile>, provider: (Tile, String) -> ImageTile): List<ImageTile> =
            tiles.map { tile -> this.tiles.getOrPut(tile) { provider(tile, tile.toString()) } }
    }
}
