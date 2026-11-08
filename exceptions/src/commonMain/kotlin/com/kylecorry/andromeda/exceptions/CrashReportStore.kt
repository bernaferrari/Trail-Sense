package com.kylecorry.andromeda.exceptions

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

/** Stores the most recent crash report for the host application to review on its next launch. */
class CrashReportStore(
    private val path: Path,
    private val generator: BugReportGenerator = StackTraceReport,
    private val fileSystem: FileSystem = defaultFileSystem(),
) {
    private val lock = SynchronizedObject()
    private val temporaryPath = path.parent?.resolve("${path.name}.pending") ?: "${path.name}.pending".toPath()

    /** Writes a new report via a temporary file, preserving an existing report if generation fails. */
    fun record(error: Throwable) = synchronized(lock) {
        val report = generator.generate(error)
        path.parent?.let { fileSystem.createDirectories(it) }
        try {
            fileSystem.sink(temporaryPath).buffer().use { it.writeUtf8(report) }
            fileSystem.atomicMove(temporaryPath, path)
        } finally {
            fileSystem.delete(temporaryPath, mustExist = false)
        }
    }

    fun readPending(): String? = synchronized(lock) {
        if (!fileSystem.exists(path)) null else fileSystem.source(path).buffer().use { it.readUtf8() }
    }

    /** Clears the pending report only if it still has the content that was reviewed. */
    fun clearPending(reviewed: String): Boolean = synchronized(lock) {
        if (!fileSystem.exists(path)) return@synchronized false
        val current = fileSystem.source(path).buffer().use { it.readUtf8() }
        if (current != reviewed) return@synchronized false
        fileSystem.delete(path)
        true
    }

    /** Records uncaught Kotlin exceptions before forwarding to the previous platform hook. */
    fun install(): AutoCloseable = installExceptionHook { error -> runCatching { record(error) } }
}
