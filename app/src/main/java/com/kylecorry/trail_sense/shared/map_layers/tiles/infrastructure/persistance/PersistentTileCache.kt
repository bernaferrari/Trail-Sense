package com.kylecorry.trail_sense.shared.map_layers.tiles.infrastructure.persistance

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.kylecorry.andromeda.files.CacheFileSystem
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.shared.events.EventData
import com.kylecorry.trail_sense.shared.events.ToolBroadcasts
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.tools.offline_maps.OfflineMapsToolRegistration
import com.kylecorry.trail_sense.tools.offline_maps.domain.OfflineMapType
import com.kylecorry.andromeda.bitmaps.ImageSaver
import com.kylecorry.trail_sense.tools.offline_maps.map_layers.PhotoMapTileSource
import java.io.FileOutputStream
import java.util.UUID
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PersistentTileCache(context: Context, private val repo: CachedTileRepo) {

    private val appContext = context.applicationContext
    private val files = CacheFileSystem(appContext)
    private val imageSaver = ImageSaver()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val offlineMapsChangedCallback: (EventData) -> Unit = { data ->
        scope.launch {
            onOfflineMapsChanged(data)
        }
    }

    init {
        ToolBroadcasts.subscribe(
            OfflineMapsToolRegistration.BROADCAST_OFFLINE_MAP_CHANGED,
            offlineMapsChangedCallback
        )
        ToolBroadcasts.subscribe(
            OfflineMapsToolRegistration.BROADCAST_OFFLINE_MAP_ADDED,
            offlineMapsChangedCallback
        )
        ToolBroadcasts.subscribe(
            OfflineMapsToolRegistration.BROADCAST_OFFLINE_MAP_DELETED,
            offlineMapsChangedCallback
        )
    }

    private suspend fun onOfflineMapsChanged(data: EventData) {
        val mapId = data.getLong(OfflineMapsToolRegistration.BROADCAST_PARAM_OFFLINE_MAP_ID)
        val mapTypeId = data.getLong(OfflineMapsToolRegistration.BROADCAST_PARAM_OFFLINE_MAP_TYPE)

        if (mapTypeId != OfflineMapType.Photo.id) {
            return
        }

        val cacheKeys = listOf(
            "${PhotoMapTileSource.SOURCE_ID}-true-$mapId",
            "${PhotoMapTileSource.SOURCE_ID}-false-$mapId",
            "${PhotoMapTileSource.SOURCE_ID}-null-$mapId",
            "${PhotoMapTileSource.SOURCE_ID}-true",
            "${PhotoMapTileSource.SOURCE_ID}-false",
            "${PhotoMapTileSource.SOURCE_ID}-null",
        )

        cacheKeys.forEach {
            invalidate(it)
        }
    }


    suspend fun getOrPut(key: String, tile: Tile, producer: suspend () -> Bitmap): Bitmap = onIO {
        val cached = repo.get(key, tile)

        if (cached != null) {
            // Cache hit - load bitmap and update last used time
            val bitmap = loadBitmap(cached)
            if (bitmap != null) {
                repo.updateLastUsedOn(cached.id)
                return@onIO bitmap
            }
            // File was missing, delete the cache entry
            repo.delete(cached)
        }

        // Cache miss - produce bitmap and save
        val bitmap = producer()
        saveBitmap(key, tile, bitmap)

        // Check if cache exceeds limit and evict if needed
        val totalSize = repo.getTotalSizeBytes()
        if (totalSize > MAX_CACHE_SIZE_BYTES) {
            repo.deleteLeastRecentlyUsed(EVICTION_COUNT)
            CachedTileCleanupWorker.start(appContext)
        }

        bitmap
    }

    suspend fun invalidate(key: String) = onIO {
        val filenames = repo.getFilenamesByKey(key)
        repo.deleteByKey(key)
        for (filename in filenames) {
            files.delete("${CachedTileRepo.TILES_FOLDER}/$filename")
        }
    }

    private fun loadBitmap(cached: CachedTile): Bitmap? {
        val file = files.getFile("${CachedTileRepo.TILES_FOLDER}/${cached.filename}", false)
        if (!file.exists()) {
            return null
        }

        val options = BitmapFactory.Options().apply {
            inPreferredConfig = if (cached.hasAlpha) {
                Bitmap.Config.ARGB_8888
            } else {
                Bitmap.Config.RGB_565
            }
        }

        return BitmapFactory.decodeFile(file.path, options)
    }

    private suspend fun saveBitmap(key: String, tile: Tile, bitmap: Bitmap) {
        val filename = "${UUID.randomUUID()}.webp"
        val file = files.getFile("${CachedTileRepo.TILES_FOLDER}/$filename", create = true)

        FileOutputStream(file).use { out ->
            imageSaver.saveAsWebp(bitmap, out, LOSSLESS_QUALITY)
        }

        val hasAlpha = bitmap.config == Bitmap.Config.ARGB_8888
        val sizeBytes = file.length()
        val now = Clock.System.now()

        val cachedTile = CachedTile(
            id = 0,
            key = key,
            tile = tile,
            filename = filename,
            createdOn = now,
            lastUsedOn = now,
            sizeBytes = sizeBytes,
            hasAlpha = hasAlpha
        )

        repo.add(cachedTile)
    }

    companion object {
        private const val MAX_CACHE_SIZE_BYTES = 25 * 1024 * 1024L // 25 MB
        private const val EVICTION_COUNT = 150
        private const val LOSSLESS_QUALITY = 100
    }
}
