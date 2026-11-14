package com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles

import com.kylecorry.trail_sense.shared.map_layers.tiles.SharedImageTile
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileState

/** An available tile and, when it is a lower-resolution parent, the desired region to clip it to. */
data class RenderedTile<T>(val imageTile: T, val clipTo: Tile? = null)

/** Chooses available tiles, preferring the requested tile, then children and finally a parent. */
fun <T : SharedImageTile<*>> selectTilesForRendering(
    desiredTiles: List<Tile>,
    get: (Tile) -> T?,
    peek: (Tile) -> T?
): List<RenderedTile<T>> {
    fun available(tile: T?): Boolean = tile?.state == TileState.Loaded ||
        tile?.state == TileState.Empty || tile?.state == TileState.Stale ||
        (tile?.state == TileState.Loading && tile.hasImage())

    val result = mutableSetOf<RenderedTile<T>>()
    desiredTiles.forEach { desired ->
        val candidates = mutableListOf<T>()
        val self = get(desired)
        if (self != null && available(self)) {
            candidates.add(self)
            if (!self.isFadingIn()) {
                result.add(RenderedTile(self))
                return@forEach
            }
        }
        val children = desired.getChildren().mapNotNull(peek).filter { available(it) }
        candidates.addAll(children)
        if (children.size < 4) {
            var parent = desired.getParent()
            while (parent != null) {
                val candidate = peek(parent)
                if (candidate != null && available(candidate)) {
                    candidates.add(candidate)
                    break
                }
                parent = parent.getParent()
            }
        }
        candidates.forEach { imageTile ->
            result.add(RenderedTile(imageTile, desired.takeIf { imageTile.tile.z < it.z }))
        }
    }
    return result.sortedBy { it.imageTile.tile.z }
}
