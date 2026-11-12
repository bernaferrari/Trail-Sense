package com.kylecorry.trail_sense.tools.offline_maps.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CreateMapResultTest {

    @Test
    fun failuresHaveNoMap() {
        assertNull(CreateMapResult.cancelled.mapOrNull())
        assertNull(CreateMapResult.Failed(CreateOfflineMapError.InvalidMapFile).mapOrNull())
    }

    @Test
    fun cancelledIsACancellationFailure() {
        assertEquals(CreateMapResult.Failed(CreateOfflineMapError.Cancelled), CreateMapResult.cancelled)
    }
}
