package com.kylecorry.trail_sense.tools.notes.domain

/** A note; [id] is 0 until it is saved. */
data class Note(
    val title: String?,
    val contents: String?,
    val createdOn: Long,
    val id: Long = 0
)
