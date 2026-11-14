package com.kylecorry.trail_sense.tools.notes.infrastructure

import com.kylecorry.trail_sense.tools.notes.domain.Note
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IosNotesDatabaseTest {
    @Test
    fun persistsNotesAcrossRepositoryInstances() = runBlocking {
        val path = NSTemporaryDirectory() + NSUUID().UUIDString + ".sqlite"
        val first = iosNoteRepository(path)
        val id = first.addNote(Note("Title", "Content", 123L))
        val reopened = iosNoteRepository(path)
        val saved = reopened.getNote(id)
        assertEquals(Note("Title", "Content", 123L, id), saved)
        reopened.deleteNote(saved!!)
        assertNull(first.getNote(id))
    }
}
