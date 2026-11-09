package com.kylecorry.andromeda.files

import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArchiveTest {
    @Test
    fun excludesFilesAndLimitsExtraction() = runBlocking {
        val root = Files.createTempDirectory("andromeda-archive-limits").toString().toPath()
        val storage = FileStorage(root)
        try {
            storage.writeText("input/a.txt", "a")
            storage.writeText("input/b.txt", "b")
            storage.writeText("input/c.txt", "c")
            storage.zip("archive.zip", listOf("input"), password = "secret", exclude = setOf("input/b.txt"))
            assertEquals(listOf("input/", "input/a.txt", "input/c.txt"), storage.listArchive("archive.zip").map { it.name })
            storage.unzip("archive.zip", "restored", "secret", maxEntries = 2)
            assertTrue(storage.exists("restored/input/a.txt"))
            assertTrue(!storage.exists("restored/input/c.txt"))
        } finally {
            storage.fileSystem.deleteRecursively(root)
        }
    }

    @Test
    fun archivesNestedFilesAndReadsEntries() = runBlocking {
        val root = Files.createTempDirectory("andromeda-archive").toString().toPath()
        val storage = FileStorage(root)
        try {
            storage.writeText("input/nested/message.txt", "hello")
            storage.createFile("input/nested/empty.txt")
            assertTrue(storage.metadata("input/nested")!!.isDirectory)
            storage.zip("output/archive.zip", listOf("input"), password = "secret")
            assertTrue(storage.listArchive("output/archive.zip").any { it.name == "input/nested/message.txt" })
            assertContentEquals("hello".encodeToByteArray(), storage.readArchiveEntry("output/archive.zip", "input/nested/message.txt", "secret"))
            assertNull(storage.readArchiveEntry("output/archive.zip", "absent"))
            storage.unzip("output/archive.zip", "restored", "secret")
            assertEquals("hello", storage.readText("restored/input/nested/message.txt"))
            assertFailsWith<IllegalArgumentException> { storage.zip("input/self.zip", listOf("input")) }
            Unit
        } finally {
            storage.fileSystem.deleteRecursively(root)
        }
    }
}
