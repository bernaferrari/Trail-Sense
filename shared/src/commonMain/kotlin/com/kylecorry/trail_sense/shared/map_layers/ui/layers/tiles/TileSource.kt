package com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles

import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MapLayerValues

/** Produces tile images using resources supplied when the source is created. */
interface TileSource<out Image> {
    suspend fun loadTile(tile: Tile, params: MapLayerValues = MapLayerValues.Empty): Image?
    fun invalidate() {}
    suspend fun cleanup() {}
}
