package com.kylecorry.andromeda.files

import no.synth.kmpzip.io.readBytes
import okio.use
import no.synth.kmpzip.okio.zipTo
import no.synth.kmpzip.okio.SinkOutputStream
import no.synth.kmpzip.okio.SourceInputStream
import no.synth.kmpzip.zip.ZipEntry
import no.synth.kmpzip.zip.ZipInputStream
import no.synth.kmpzip.zip.ZipOutputStream
import no.synth.kmpzip.zip.safeEntrySegments
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Path
import okio.buffer

/** Creates a ZIP from files or directories in this store. */
suspend fun FileStorage.zip(archive: String, sources: List<String>, password: String? = null, exclude: Set<String> = emptySet()) {
    val target = root.resolve(archive)
    target.parent?.let { fileSystem.createDirectories(it) }
    val canonicalTarget = if (fileSystem.exists(target)) fileSystem.canonicalize(target) else {
        val parent = target.parent ?: error("Archive must have a parent directory")
        fileSystem.canonicalize(parent) / target.name
    }
    val paths = sources.map(root::resolve)
    for (source in paths) {
        val canonicalSource = fileSystem.canonicalize(source)
        var ancestor = canonicalTarget
        while (true) {
            require(ancestor != canonicalSource) { "Archive cannot be inside a source being archived" }
            ancestor = ancestor.parent ?: break
        }
    }
    if (exclude.isEmpty()) {
        fileSystem.zipTo(target = target, sources = paths, password = password)
        return
    }
    withContext(Dispatchers.Default) {
        val excluded = exclude.map(root::resolve).filter(fileSystem::exists).map(fileSystem::canonicalize).toSet()
        ZipOutputStream(SinkOutputStream(fileSystem.sink(target).buffer()), password?.encodeToByteArray()).use { zip ->
            suspend fun add(path: Path, prefix: String, depth: Int) {
                currentCoroutineContext().ensureActive()
                require(depth <= 1024) { "Archive directory depth exceeds 1024" }
                if (fileSystem.canonicalize(path) in excluded) return
                val name = if (prefix.isEmpty()) path.name else "$prefix/${path.name}"
                if (fileSystem.metadata(path).isDirectory) {
                    zip.putNextEntry(ZipEntry("$name/"))
                    zip.closeEntry()
                    for (child in fileSystem.list(path).sortedBy { it.name }) add(child, name, depth + 1)
                } else {
                    zip.putNextEntry(ZipEntry(name))
                    SourceInputStream(fileSystem.source(path).buffer()).use { input ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(buffer, 0, buffer.size)
                            if (count < 0) break
                            zip.write(buffer, 0, count)
                        }
                    }
                    zip.closeEntry()
                }
            }
            for (path in paths) add(path, "", 0)
        }
    }
}

/** Extracts a ZIP into this store, rejecting archive entries that escape the destination. */
suspend fun FileStorage.unzip(archive: String, destination: String, password: String? = null, maxEntries: Int = Int.MAX_VALUE) {
    require(maxEntries >= 0)
    withContext(Dispatchers.Default) {
        val target = root.resolve(destination)
        fileSystem.createDirectories(target)
        val canonicalRoot = fileSystem.canonicalize(target)
        fun checkInside(path: Path) {
            var current: Path? = fileSystem.canonicalize(path)
            while (current != null && current != canonicalRoot) current = current.parent
            require(current != null) { "Archive entry escapes the destination" }
        }
        ZipInputStream(SourceInputStream(fileSystem.source(root.resolve(archive)).buffer()), password?.encodeToByteArray()).use { zip ->
            var count = 0
            while (count < maxEntries) {
                currentCoroutineContext().ensureActive()
                val entry = zip.nextEntry ?: break
                val segments = safeEntrySegments(entry.name)
                var output = target
                for (segment in segments.dropLast(1)) {
                    output /= segment
                    fileSystem.createDirectories(output)
                    checkInside(output)
                }
                segments.lastOrNull()?.let { output /= it }
                if (entry.isDirectory) {
                    fileSystem.createDirectories(output)
                    checkInside(output)
                } else {
                    if (fileSystem.exists(output)) checkInside(output)
                    SinkOutputStream(fileSystem.sink(output).buffer()).use { sink ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val size = zip.read(buffer, 0, buffer.size)
                            if (size < 0) break
                            sink.write(buffer, 0, size)
                        }
                    }
                }
                count++
            }
        }
    }
}

/** Metadata for a file or directory in a ZIP archive. */
data class ArchiveEntry(val name: String, val isDirectory: Boolean, val size: Long, val compressedSize: Long)

/** Lists ZIP entries without loading their contents. */
fun FileStorage.listArchive(archive: String, password: String? = null, maxEntries: Int = Int.MAX_VALUE): List<ArchiveEntry> =
    fileSystem.openReadOnly(root.resolve(archive)).use { handle ->
        no.synth.kmpzip.okio.ZipFile(handle, password?.encodeToByteArray()).use { zip ->
            zip.entries.take(maxEntries).map { ArchiveEntry(it.name, it.isDirectory, it.size, it.compressedSize) }
        }
    }

/** Reads one named ZIP entry without extracting the rest of the archive. */
fun FileStorage.readArchiveEntry(archive: String, name: String, password: String? = null): ByteArray? =
    fileSystem.openReadOnly(root.resolve(archive)).use { handle ->
        no.synth.kmpzip.okio.ZipFile(handle, password?.encodeToByteArray()).use { zip ->
            val entry = zip.getEntry(name) ?: return null
            zip.getInputStream(entry).use { it.readBytes() }
        }
    }
