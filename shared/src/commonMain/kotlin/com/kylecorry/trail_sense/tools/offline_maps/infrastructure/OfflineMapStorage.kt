package com.kylecorry.trail_sense.tools.offline_maps.infrastructure

import com.kylecorry.andromeda.files.FileStorage
import com.kylecorry.trail_sense.shared.coroutines.ioDispatcher
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.withContext
import okio.buffer
import okio.use

/** Private map files on disk or in the browser. A blob supports bounded reads for large Mapsforge files. */
internal interface OfflineMapStorage {
    suspend fun copy(path: String, file: PlatformFile)
    suspend fun write(path: String, bytes: ByteArray)
    suspend fun open(path: String): OfflineMapBlob?
    suspend fun delete(path: String)
}

internal interface OfflineMapBlob {
    val size: Long
    suspend fun read(offset: Long, length: Int): ByteArray

    suspend fun readAll(limit: Int): ByteArray {
        require(size in 1L..limit.toLong()) { "Map file exceeds the supported size" }
        return read(0, size.toInt())
    }
}

/** Shared bounded file reader. The picker owns security-scoped copy on Apple platforms. */
internal class DiskOfflineMapStorage(
    private val files: FileStorage,
    private val copyFile: suspend (PlatformFile, String) -> Unit
) : OfflineMapStorage {
    override suspend fun copy(path: String, file: PlatformFile) = withContext(ioDispatcher) {
        files.root.resolve(path).parent?.let { files.fileSystem.createDirectories(it) }
        copyFile(file, files.root.resolve(path).toString())
    }

    override suspend fun write(path: String, bytes: ByteArray) = withContext(ioDispatcher) {
        files.write(path, bytes)
    }

    override suspend fun open(path: String): OfflineMapBlob? = withContext(ioDispatcher) {
        val target = files.root.resolve(path)
        val size = files.fileSystem.metadataOrNull(target)?.takeIf { it.isRegularFile }?.size ?: return@withContext null
        object : OfflineMapBlob {
            override val size = size
            override suspend fun read(offset: Long, length: Int): ByteArray = withContext(ioDispatcher) {
                require(offset >= 0 && length >= 0 && offset <= size - length)
                files.fileSystem.openReadOnly(target).use { handle ->
                    handle.source(offset).buffer().use { it.readByteArray(length.toLong()) }
                }
            }
        }
    }

    override suspend fun delete(path: String) = withContext(ioDispatcher) { files.delete(path) }
}
