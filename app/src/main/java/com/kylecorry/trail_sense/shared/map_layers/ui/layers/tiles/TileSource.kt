package com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MapLayerValues
import android.content.Context
import android.graphics.Bitmap
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile

interface TileSource {
    suspend fun loadTile(
        context: Context,
        tile: Tile,
        params: MapLayerValues = MapLayerValues.Empty
    ): Bitmap?

    fun invalidate() {
        // Do nothing
    }

    suspend fun cleanup() {
        // Do nothing
    }

}
