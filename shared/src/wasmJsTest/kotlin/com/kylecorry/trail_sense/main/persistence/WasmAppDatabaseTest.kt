package com.kylecorry.trail_sense.main.persistence

import com.kylecorry.trail_sense.tools.notes.infrastructure.NoteEntity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WasmAppDatabaseTest {
    @Test
    fun noteCrudSurvivesReopen() = runTest {
        val name = "browser-note-test.db"
        val first = openWasmDatabase(name)
        val id = try {
            first.database.noteDao().insert(NoteEntity("Trail", "Started", 1_763_422_000_000))
        } finally {
            first.close()
        }

        val reopened = openWasmDatabase(name)
        try {
            val dao = reopened.database.noteDao()
            val note = requireNotNull(dao.get(id))
            assertEquals("Started", note.contents)
            val updated = note.copy(contents = "Finished").also { it.id = id }
            dao.update(updated)
            assertEquals("Finished", dao.get(id)?.contents)
            dao.delete(updated)
            assertNull(dao.get(id))
        } finally {
            reopened.close()
        }
    }
}
