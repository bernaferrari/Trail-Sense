package com.kylecorry.trail_sense.tools.notes.infrastructure

import kotlinx.coroutines.flow.Flow
import com.kylecorry.trail_sense.tools.notes.domain.Note

interface INoteRepo {
    fun getNotes(): Flow<List<Note>>

    suspend fun getNote(id: Long): Note?

    suspend fun deleteNote(note: Note)

    suspend fun addNote(note: Note): Long
}
