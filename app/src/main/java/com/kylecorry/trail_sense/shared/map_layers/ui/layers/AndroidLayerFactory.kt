package com.kylecorry.trail_sense.shared.map_layers.ui.layers

import android.graphics.Bitmap
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer

fun androidLayerFactory(): LayerFactory<Bitmap> = LayerFactory { definition ->
    TileMapLayer(
        definition.tileSource!!.invoke(),
        definition.id,
        shouldMultiply = definition.shouldMultiply,
        minZoomLevel = definition.minZoomLevel,
        isTimeDependent = definition.isTimeDependent,
        refreshInterval = definition.refreshInterval,
        refreshBroadcasts = definition.refreshBroadcasts,
        cacheKeys = definition.cacheKeys
    )
}
