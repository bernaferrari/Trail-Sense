package com.kylecorry.trail_sense.main.persistence

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.kylecorry.trail_sense.tools.notes.infrastructure.NoteEntity
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class AppDatabaseTest {
    @Test
    fun sharedSchemaOpensAndPersistsNotes() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder<AppDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
        try {
            val id = database.noteDao().insert(NoteEntity("Trail", "Notes", 123L))
            assertEquals("Trail", database.noteDao().get(id)?.title)
        } finally {
            database.close()
        }
    }
}
