package com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles

import android.graphics.Bitmap
import com.kylecorry.trail_sense.shared.map_layers.tiles.ImageTile
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.tiles.TileState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class TileRenderSelectorTest {
    private fun loaded(tile: Tile) = ImageTile(
        key = tile.toString(), tile = tile, image = mock<Bitmap>(),
        state = TileState.Loaded, shouldFadeIn = false, loadFunction = { null }
    )

    @Test
    fun missingTilesUseClippedParentAndAvailableTilesTakePrecedence() {
        val parent = Tile(0, 0, 1)
        val children = parent.getChildren()
        val parentImage = loaded(parent)
        val firstImage = loaded(children.first())
        val cache = mapOf(parent to parentImage, children.first() to firstImage)

        val selected = selectTilesForRendering(children.take(2), cache::get, cache::get)

        assertEquals(
            listOf(RenderedTile(parentImage, children[1]), RenderedTile(firstImage)),
            selected
        )
    }

    @Test
    fun fourChildrenReplaceMissingParentWithoutUsingItsAncestor() {
        val desired = Tile(0, 0, 2)
        val children = desired.getChildren().map(::loaded)
        val ancestor = loaded(desired.getParent()!!)
        val cache = (children + ancestor).associateBy { it.tile }

        val selected = selectTilesForRendering(listOf(desired), cache::get, cache::get)

        assertEquals(children.map { RenderedTile(it) }, selected)
    }
}
