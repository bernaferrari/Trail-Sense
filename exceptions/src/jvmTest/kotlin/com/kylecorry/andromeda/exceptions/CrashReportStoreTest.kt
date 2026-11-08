package com.kylecorry.andromeda.exceptions

import java.nio.file.Files
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CrashReportStoreTest {
    @Test fun storesAndOnlyClearsTheReviewedReport() {
        val dir = Files.createTempDirectory("andromeda-crash-")
        val path = dir.resolve("errors/report.txt")
        try {
            val store = CrashReportStore(path.toString().toPath(), BugReportGenerator { it.message.orEmpty() })
            assertNull(store.readPending())
            store.record(IllegalStateException("first"))
            assertEquals("first", store.readPending())
            store.record(IllegalStateException("second"))
            assertFalse(store.clearPending("first"))
            assertEquals("second", store.readPending())
            assertTrue(store.clearPending("second"))
            assertNull(store.readPending())
        } finally {
            Files.deleteIfExists(path)
            Files.deleteIfExists(path.parent)
            Files.deleteIfExists(dir)
        }
    }
}
