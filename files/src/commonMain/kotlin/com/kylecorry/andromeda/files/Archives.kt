package com.kylecorry.andromeda.files

import no.synth.kmpzip.io.readBytes
import no.synth.kmpzip.okio.unzipFrom
import okio.use
import no.synth.kmpzip.okio.zipTo

/** Creates a ZIP from files or directories in this store. */
suspend fun FileStorage.zip(archive: String, sources: List<String>, password: String? = null) {
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
    fileSystem.zipTo(
        target = target,
        sources = paths,
        password = password,
    )
}

/** Extracts a ZIP into this store, rejecting archive entries that escape the destination. */
suspend fun FileStorage.unzip(archive: String, destination: String, password: String? = null) {
    fileSystem.unzipFrom(
        archive = root.resolve(archive),
        target = root.resolve(destination),
        password = password,
    )
}

/** Metadata for a file or directory in a ZIP archive. */
data class ArchiveEntry(val name: String, val isDirectory: Boolean, val size: Long, val compressedSize: Long)

/** Lists ZIP entries without loading their contents. */
fun FileStorage.listArchive(archive: String, password: String? = null): List<ArchiveEntry> =
    fileSystem.openReadOnly(root.resolve(archive)).use { handle ->
        no.synth.kmpzip.okio.ZipFile(handle, password?.encodeToByteArray()).use { zip ->
            zip.entries.map { ArchiveEntry(it.name, it.isDirectory, it.size, it.compressedSize) }
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
