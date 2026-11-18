package com.kylecorry.trail_sense.main.persistence

import com.kylecorry.trail_sense.tools.notes.infrastructure.NoteEntity
import kotlinx.coroutines.test.runTest
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class JvmAppDatabaseTest {
    @Test
    fun noteSurvivesClosingAndReopeningDesktopDatabase() = runTest {
        val directory = createTempDirectory("trail-sense-desktop-test")
        val path = directory.resolve("trail-sense.db").toString()
        try {
            val first = jvmAppDatabase(path)
            val id = try {
                first.noteDao().insert(NoteEntity("Trail", "Made it", 123L))
            } finally {
                first.close()
            }

            val reopened = jvmAppDatabase(path)
            try {
                assertEquals("Made it", reopened.noteDao().get(id)?.contents)
            } finally {
                reopened.close()
            }
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
