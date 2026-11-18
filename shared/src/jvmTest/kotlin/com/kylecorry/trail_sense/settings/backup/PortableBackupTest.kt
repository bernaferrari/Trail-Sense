package com.kylecorry.trail_sense.settings.backup

import com.kylecorry.andromeda.files.FileStorage
import com.kylecorry.trail_sense.main.persistence.jvmAppDatabase
import com.kylecorry.trail_sense.tools.notes.infrastructure.NoteEntity
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFails
import kotlin.test.assertTrue

class PortableBackupTest {
    @Test
    fun restoresNotesAndFilesAndKeepsExistingDataForInvalidArchives() = runTest {
        val originalFolder = Files.createTempDirectory("trail-sense-backup-source").toFile()
        val destinationFolder = Files.createTempDirectory("trail-sense-backup-destination").toFile()
        val originalFiles = FileStorage(originalFolder.path.toPath())
        val destinationFiles = FileStorage(destinationFolder.path.toPath())
        val original = jvmAppDatabase(originalFolder.resolve("app.sqlite").path)
        val destination = jvmAppDatabase(destinationFolder.resolve("app.sqlite").path)
        try {
            val id = original.noteDao().insert(NoteEntity("Trail", "Made it", 123))
            val map = "offline_maps/fixture.geojson"
            originalFiles.writeText(map, "test map content")
            val bytes = FilePortableBackup(original, originalFiles).export()
            val restore = FilePortableBackup(destination, destinationFiles)
            val previousId = destination.noteDao().insert(NoteEntity("Old note", null, 456))

            assertFails { restore.restore(byteArrayOf(1, 2, 3)) }
            assertEquals("Old note", destination.noteDao().get(previousId)?.title)
            assertFalse(destinationFiles.exists(map))

            restore.restore(bytes)
            assertEquals("Trail", destination.noteDao().get(id)?.title)
            assertEquals("test map content", destinationFiles.readText(map))
            restore.restore(bytes)
            assertTrue(destinationFiles.exists(map))
        } finally {
            original.close()
            destination.close()
            originalFolder.deleteRecursively()
            destinationFolder.deleteRecursively()
        }
    }
}
