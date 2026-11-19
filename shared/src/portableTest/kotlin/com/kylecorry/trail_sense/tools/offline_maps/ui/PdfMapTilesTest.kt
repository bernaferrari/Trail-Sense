package com.kylecorry.trail_sense.tools.offline_maps.ui

import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.PdfRegion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PdfMapTilesTest {
    @Test
    fun usesPreviewWhenItHasEnoughResolution() {
        assertTrue(planPdfTiles(3072, 1600, 1.0, PdfRegion()).isEmpty())
        assertTrue(planPdfTiles(3072, 1600, 4.0, PdfRegion(1.1, 0.0, 1.2, 1.0)).isEmpty())
    }

    @Test
    fun sharesTilesAcrossNearbyZoomLevelsAndClipsAtPageEdges() {
        val viewport = PdfRegion(0.99, 0.99, 1.1, 1.1)
        val tiles = planPdfTiles(300, 250, 3.0, viewport)
        assertEquals(tiles, planPdfTiles(300, 250, 3.5, viewport))
        val tile = tiles.single()
        assertEquals(176, tile.width)
        assertEquals(488, tile.height)
        assertEquals(PdfRegion(1024.0 / 1200, 512.0 / 1000, 1.0, 1.0), tile.region)
    }

    @Test
    fun reducesDetailWhenTheViewportWouldNeedTooManyTiles() {
        val tiles = planPdfTiles(1024, 1024, 32.0, PdfRegion())
        assertEquals(16, tiles.size)
        assertTrue(tiles.all { it.pageWidth == 2048 && it.pageHeight == 2048 })
    }
}
