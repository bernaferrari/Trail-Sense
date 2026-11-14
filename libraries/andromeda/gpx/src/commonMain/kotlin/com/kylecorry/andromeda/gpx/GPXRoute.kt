package com.kylecorry.andromeda.gpx

public data class GPXRoute(
    val name: String?,
    val description: String?,
    val comment: String?,
    val source: String?,
    val number: Long?,
    val type: String?,
    val points: List<GPXWaypoint>
)
