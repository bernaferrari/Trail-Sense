package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.persistence

import com.kylecorry.trail_sense.tools.offline_maps.domain.groups.MapGroup
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.VectorMap

/**
 * Stores photo maps, vector maps and the groups that hold them.
 */
interface IMapRepo {
    suspend fun getPhotoMaps(): List<PhotoMap>
    suspend fun getVectorMaps(): List<VectorMap>
    suspend fun getMapGroup(id: Long): MapGroup?
    suspend fun getPhotoMap(id: Long): PhotoMap?
    suspend fun getVectorMap(id: Long): VectorMap?
    suspend fun delete(map: PhotoMap)
    suspend fun delete(map: VectorMap)
    suspend fun copyToAppStorage(map: VectorMap): VectorMap?
    suspend fun delete(group: MapGroup)
    suspend fun add(group: MapGroup): Long
    suspend fun add(map: PhotoMap): Long
    suspend fun add(file: VectorMap): Long
    suspend fun getPhotoMaps(parentId: Long?): List<PhotoMap>
    suspend fun getVectorMaps(parentId: Long?): List<VectorMap>
    suspend fun getMapGroups(parentId: Long?): List<MapGroup>
}
