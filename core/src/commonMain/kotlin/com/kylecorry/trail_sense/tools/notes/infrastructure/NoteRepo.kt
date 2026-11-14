package com.kylecorry.trail_sense.tools.notes.infrastructure

import com.kylecorry.trail_sense.tools.notes.domain.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Persists notes through the platform's Room database. */
class NoteRepo(private val noteDao: NoteDao) : INoteRepo {
    override fun getNotes(): Flow<List<Note>> = noteDao.getAll().map { notes -> notes.map { it.toNote() } }

    override suspend fun getNote(id: Long): Note? = noteDao.get(id)?.toNote()

    override suspend fun deleteNote(note: Note) = noteDao.delete(NoteEntity.from(note))

    override suspend fun addNote(note: Note): Long = if (note.id != 0L) {
        noteDao.update(NoteEntity.from(note))
        note.id
    } else {
        noteDao.insert(NoteEntity.from(note))
    }
}
