package com.kylecorry.andromeda.gpx

import com.kylecorry.sol.units.Coordinate
import kotlin.time.Instant

public data class GPXWaypoint(
    val coordinate: Coordinate,
    val name: String? = null,
    val elevation: Float? = null,
    val type: String? = null,
    val description: String? = null,
    val comment: String? = null,
    val time: Instant? = null,
    val group: String? = null,
    val symbol: String? = null,
    val color: Int? = null
)
