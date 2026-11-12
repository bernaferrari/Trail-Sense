package com.kylecorry.trail_sense.tools.offline_maps.ui

import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.offline_maps.domain.CreateOfflineMapError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class CreateMapErrorMessagesTest {

    @Test
    fun cancellingShowsNoMessage() {
        assertNull(createMapErrorMessage(CreateOfflineMapError.Cancelled))
    }

    @Test
    fun eachFailureHasItsOwnMessage() {
        assertEquals(
            R.string.map_error_invalid_map_file,
            createMapErrorMessage(CreateOfflineMapError.InvalidMapFile)
        )
        assertEquals(
            R.string.map_error_access_denied,
            createMapErrorMessage(CreateOfflineMapError.AccessDenied)
        )
        assertEquals(
            R.string.map_error_unable_to_copy,
            createMapErrorMessage(CreateOfflineMapError.UnableToCopy)
        )
    }
}
