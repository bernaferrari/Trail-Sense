package com.kylecorry.trail_sense.shared.map_layers.tiles.infrastructure.persistance

import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.main.persistence.ICleanable
import com.kylecorry.trail_sense.shared.getUpsertedId
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import kotlin.time.Instant

class CachedTileRepo(
    private val dao: CachedTileDao,
    private val listFiles: () -> List<String>,
    private val deleteFile: (String) -> Unit
) : ICleanable {

    suspend fun get(key: String, tile: Tile): CachedTile? = onIO {
        dao.get(key, tile.x, tile.y, tile.z)?.toCachedTile()
    }

    suspend fun add(cachedTile: CachedTile): Long = onIO {
        dao.upsert(CachedTileEntity.from(cachedTile)).getUpsertedId(cachedTile.id)
    }

    suspend fun delete(cachedTile: CachedTile) = onIO {
        dao.delete(CachedTileEntity.from(cachedTile))
    }

    suspend fun deleteCreatedBefore(instant: Instant) = onIO {
        dao.deleteCreatedBefore(instant.toEpochMilliseconds())
    }

    suspend fun deleteLeastRecentlyUsed(count: Int) = onIO {
        dao.deleteLeastRecentlyUsed(count)
    }

    suspend fun getTotalSizeBytes(): Long = onIO {
        dao.getTotalSizeBytes()
    }

    suspend fun updateLastUsedOn(id: Long, lastUsedOn: Instant = Clock.System.now()) = onIO {
        dao.updateLastUsedOn(id, lastUsedOn.toEpochMilliseconds())
    }

    suspend fun getFilenamesByKey(key: String): List<String> = onIO {
        dao.getFilenamesByKey(key)
    }

    suspend fun deleteByKey(key: String) = onIO {
        dao.deleteByKey(key)
    }

    suspend fun deleteAll() = onIO {
        dao.deleteAll()
    }

    override suspend fun clean() = onIO {
        // Delete tiles older than 7 days
        deleteCreatedBefore(Clock.System.now().minus(MAX_AGE))

        // Get all filenames from the cache and files on disk
        val cachedFilenames = dao.getAllFilenames().toSet()
        val diskFilenames = listFiles().toSet()

        // Delete cache entries for missing files
        val missingFiles = cachedFilenames - diskFilenames
        if (missingFiles.isNotEmpty()) {
            dao.deleteByFilenames(missingFiles.toList())
        }

        // Delete files that aren't in the cache
        val orphanedFiles = diskFilenames - cachedFilenames
        for (filename in orphanedFiles) {
            deleteFile(filename)
        }
    }

    companion object {
        const val TILES_FOLDER = "tiles"
        private val MAX_AGE = 7.days

    }
}
