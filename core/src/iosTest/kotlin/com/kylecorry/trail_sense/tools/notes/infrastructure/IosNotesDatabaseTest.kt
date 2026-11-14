package com.kylecorry.trail_sense.tools.notes.infrastructure

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
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

    @Test
    fun migratesNotesFromTheOriginalIosDatabase() = runBlocking {
        val path = NSTemporaryDirectory() + NSUUID().UUIDString + ".sqlite"
        BundledSQLiteDriver().open(path).use { connection ->
            connection.execSQL("CREATE TABLE IF NOT EXISTS `notes` (`title` TEXT, `contents` TEXT, `created` INTEGER NOT NULL, `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
            connection.execSQL("INSERT INTO `notes` (`title`, `contents`, `created`) VALUES ('Saved', 'Before migration', 123)")
            connection.execSQL("PRAGMA user_version = 1")
        }
        val repo = iosNoteRepository(path)
        assertEquals(Note("Saved", "Before migration", 123L, 1L), repo.getNote(1L))
    }
}
