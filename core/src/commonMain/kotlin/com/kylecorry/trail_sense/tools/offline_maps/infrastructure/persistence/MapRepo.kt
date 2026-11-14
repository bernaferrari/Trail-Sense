package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.persistence

import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.luna.concurrency.ParallelCoroutineRunner
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.shared.getUpsertedId
import com.kylecorry.trail_sense.tools.offline_maps.domain.groups.MapGroup
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMapEntity
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.VectorMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.groups.MapGroupEntity
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.groups.MapGroupDao
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps.PhotoMapDao
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.persistence.VectorMapDao
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.persistence.VectorMapEntity

interface MapFileAccess {
    fun delete(path: String)
    fun canRead(path: String): Boolean
    fun releaseExternal(path: String)
    suspend fun copyToAppStorage(map: VectorMap): VectorMap?
    fun populatePhotoMap(map: PhotoMapEntity): PhotoMap
}

class MapRepo(
    private val offlineMapFileDao: VectorMapDao,
    private val photoMapDao: PhotoMapDao,
    private val mapGroupDao: MapGroupDao,
    private val files: MapFileAccess,
    private val invalidatePhotoMapCache: suspend (Long) -> Unit
) : IMapRepo {

    override suspend fun getPhotoMaps(): List<PhotoMap> = onIO {
        val photoMaps = photoMapDao.getAll()
        val runner = ParallelCoroutineRunner(MAX_PARALLEL)
        runner.map(photoMaps, ::convertToMap)
    }

    override suspend fun getVectorMaps(): List<VectorMap> = onIO {
        offlineMapFileDao.getAllSync().map { convertToMap(it) }
    }

    override suspend fun getMapGroup(id: Long): MapGroup? = onIO {
        mapGroupDao.get(id)?.toMapGroup()
    }

    override suspend fun getPhotoMap(id: Long): PhotoMap? = onIO {
        photoMapDao.get(id)?.let { convertToMap(it) }
    }

    override suspend fun getVectorMap(id: Long): VectorMap? = onIO {
        offlineMapFileDao.get(id)?.let { convertToMap(it) }
    }

    override suspend fun delete(map: PhotoMap) = onIO {
        tryOrNothing { files.delete(map.filename) }
        tryOrNothing { files.delete(map.pdfFileName) }
        photoMapDao.delete(PhotoMapEntity.from(map))
        invalidatePhotoMapCache(map.id)
    }

    override suspend fun delete(map: VectorMap) = onIO {
        if (map.isExternal) {
            releaseExternalAccessIfUnused(map)
        } else {
            tryOrNothing { files.delete(map.path) }
        }
        offlineMapFileDao.delete(VectorMapEntity.from(map))
    }

    override suspend fun copyToAppStorage(map: VectorMap): VectorMap? = onIO {
        if (!map.isExternal) {
            return@onIO map
        }

        val updated = files.copyToAppStorage(map) ?: return@onIO null
        add(updated)
        releaseExternalAccessIfUnused(map)
        updated
    }

    private suspend fun releaseExternalAccessIfUnused(map: VectorMap) = onIO {
        val isUsedByOtherMaps = offlineMapFileDao.getAllSync()
            .any { it.id != map.id && it.path == map.path }
        if (!isUsedByOtherMaps) {
            tryOrNothing { files.releaseExternal(map.path) }
        }
    }

    override suspend fun delete(group: MapGroup) {
        mapGroupDao.delete(MapGroupEntity.from(group))
    }

    override suspend fun add(group: MapGroup): Long = onIO {
        if (group.id != 0L) {
            mapGroupDao.update(MapGroupEntity.from(group))
            group.id
        } else {
            mapGroupDao.insert(MapGroupEntity.from(group))
        }
    }

    override suspend fun add(map: PhotoMap): Long = onIO {
        val newId = photoMapDao.upsert(PhotoMapEntity.from(map)).getUpsertedId(map.id)
        invalidatePhotoMapCache(newId)
        newId
    }

    override suspend fun add(file: VectorMap): Long = onIO {
        offlineMapFileDao.upsert(VectorMapEntity.from(file)).getUpsertedId(file.id)
    }

    override suspend fun getPhotoMaps(parentId: Long?): List<PhotoMap> = onIO {
        val maps = photoMapDao.getAllWithParent(parentId)
        val runner = ParallelCoroutineRunner(MAX_PARALLEL)
        runner.map(maps, ::convertToMap)
    }

    override suspend fun getVectorMaps(parentId: Long?): List<VectorMap> = onIO {
        offlineMapFileDao.getAllWithParent(parentId).map { convertToMap(it) }
    }

    override suspend fun getMapGroups(parentId: Long?): List<MapGroup> = onIO {
        mapGroupDao.getAllWithParent(parentId).map { it.toMapGroup() }
    }

    private fun convertToMap(map: VectorMapEntity): VectorMap {
        val newMap = map.toOfflineMapFile()
        return if (newMap.isExternal) {
            newMap.copy(isAvailable = files.canRead(newMap.path))
        } else {
            newMap
        }
    }

    private fun convertToMap(map: PhotoMapEntity): PhotoMap = files.populatePhotoMap(map)

    companion object {
        private const val MAX_PARALLEL = 10
    }
}
