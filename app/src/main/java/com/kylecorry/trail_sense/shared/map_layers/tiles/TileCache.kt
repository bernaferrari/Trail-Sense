package com.kylecorry.trail_sense.shared.map_layers.tiles

/** Supplies Android bitmap size and disposal to the shared cache. */
class TileCache(
    source: String,
    maxSize: Int,
    removalListener: (ImageTile) -> Unit = { it.recycle() }
) : SharedTileCache<ImageTile>(source, maxSize, ::bitmapSizeBytes, removalListener)

private fun bitmapSizeBytes(tile: ImageTile): Long {
    var bytes = 0L
    tile.withImage { image -> bytes = image?.allocationByteCount?.toLong() ?: 0L }
    return bytes
}
