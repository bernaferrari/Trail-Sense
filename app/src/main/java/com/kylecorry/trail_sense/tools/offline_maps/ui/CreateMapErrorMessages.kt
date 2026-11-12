package com.kylecorry.trail_sense.tools.offline_maps.ui

import androidx.annotation.StringRes
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.offline_maps.domain.CreateOfflineMapError

/** The message to show when creating a map fails, or null when the user cancelled. */
@StringRes
internal fun createMapErrorMessage(error: CreateOfflineMapError): Int? {
    return when (error) {
        CreateOfflineMapError.InvalidMapFile -> R.string.map_error_invalid_map_file
        CreateOfflineMapError.AccessDenied -> R.string.map_error_access_denied
        CreateOfflineMapError.UnableToCopy -> R.string.map_error_unable_to_copy
        CreateOfflineMapError.Cancelled -> null
    }
}
