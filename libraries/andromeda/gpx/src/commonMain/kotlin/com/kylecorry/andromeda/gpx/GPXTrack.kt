package com.kylecorry.andromeda.gpx


public data class GPXTrack(
    val name: String? = null,
    val type: String? = null,
    val id: Long? = null,
    val description: String? = null,
    val comment: String? = null,
    val color: Int? = null,
    val lineStyle: String? = null,
    val group: String? = null,
    val segments: List<GPXTrackSegment> = emptyList()
)