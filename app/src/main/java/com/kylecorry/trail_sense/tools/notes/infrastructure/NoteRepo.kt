package com.kylecorry.trail_sense.tools.notes.infrastructure

import android.content.Context
import com.kylecorry.trail_sense.main.persistence.AppDatabase
import com.kylecorry.trail_sense.tools.notes.domain.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NoteRepo private constructor(context: Context) : INoteRepo {

    private val noteDao = AppDatabase.getInstance(context.applicationContext).noteDao()

    override fun getNotes(): Flow<List<Note>> = noteDao.getAll().map { notes -> notes.map { it.toNote() } }

    override suspend fun getNote(id: Long): Note? = noteDao.get(id)?.toNote()

    override suspend fun deleteNote(note: Note) = noteDao.delete(NoteEntity.from(note))

    override suspend fun addNote(note: Note): Long {
        return if (note.id != 0L) {
            noteDao.update(NoteEntity.from(note))
            note.id
        } else {
            noteDao.insert(NoteEntity.from(note))
        }
    }

    companion object {
        private var instance: NoteRepo? = null

        @Synchronized
        fun getInstance(context: Context): NoteRepo {
            if (instance == null) {
                instance = NoteRepo(context.applicationContext)
            }
            return instance!!
        }
    }

}
