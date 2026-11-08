package com.kylecorry.andromeda.files

import okio.FileSystem
import okio.Path
import okio.Sink
import okio.Source
import okio.buffer
import okio.use

/** A path-based file store using the platform filesystem or an injected filesystem. */
class FileStorage(val root: Path, val fileSystem: FileSystem = defaultFileSystem()) {
    fun exists(path: String): Boolean = fileSystem.exists(root.resolve(path))
    fun metadata(path: String) = fileSystem.metadataOrNull(root.resolve(path))
    fun createFile(path: String) {
        val target = root.resolve(path)
        if (fileSystem.exists(target)) return
        target.parent?.let { fileSystem.createDirectories(it) }
        fileSystem.sink(target, mustCreate = true).close()
    }
    fun createDirectory(path: String) = fileSystem.createDirectories(root.resolve(path))
    fun delete(path: String, recursive: Boolean = false) {
        val target = root.resolve(path)
        if (recursive) fileSystem.deleteRecursively(target) else fileSystem.delete(target)
    }
    fun list(path: String = ""): List<Path> = fileSystem.listOrNull(root.resolve(path)) ?: emptyList()
    fun source(path: String): Source = fileSystem.source(root.resolve(path))
    fun sink(path: String, append: Boolean = false): Sink {
        val target = root.resolve(path)
        target.parent?.let { fileSystem.createDirectories(it) }
        return if (append) fileSystem.appendingSink(target) else fileSystem.sink(target)
    }
    fun read(path: String): ByteArray = source(path).buffer().use { it.readByteArray() }
    fun readText(path: String): String = source(path).buffer().use { it.readUtf8() }
    fun write(path: String, bytes: ByteArray, append: Boolean = false) {
        sink(path, append).buffer().use { it.write(bytes) }
    }
    fun writeText(path: String, text: String, append: Boolean = false) {
        sink(path, append).buffer().use { it.writeUtf8(text) }
    }
    fun copy(from: String, to: String) {
        val target = root.resolve(to)
        target.parent?.let { fileSystem.createDirectories(it) }
        fileSystem.copy(root.resolve(from), target)
    }
    fun move(from: String, to: String) {
        val target = root.resolve(to)
        target.parent?.let { fileSystem.createDirectories(it) }
        fileSystem.atomicMove(root.resolve(from), target)
    }
}
