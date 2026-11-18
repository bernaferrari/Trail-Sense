package com.kylecorry.trail_sense.tools.offline_maps.infrastructure

import com.kylecorry.andromeda.files.FileStorage
import com.kylecorry.andromeda.geojson.*
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.coroutines.ioDispatcher
import com.kylecorry.trail_sense.shared.extensions.normalize
import com.kylecorry.trail_sense.shared.sharing.PickedFile
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMapEntity
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.VectorMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.VectorMapFileType
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.persistence.MapFileAccess
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.uuid.Uuid

internal class SharedMapFiles(private val storage: FileStorage) : MapFileAccess {
    suspend fun import(file: PickedFile, maps: MapService): Long? = withContext(ioDispatcher) {
        val geoJson = GeoJsonConvert.fromJson(file.decodeToString()) ?: return@withContext null
        val positions = geoJson.normalize().flatMap { feature ->
            when (val geometry = feature.geometry) {
                is GeoJsonPoint -> listOfNotNull(geometry.point)
                is GeoJsonLineString -> geometry.line.orEmpty()
                is GeoJsonPolygon -> geometry.polygon.orEmpty().flatten()
                else -> emptyList()
            }
        }
        if (positions.isEmpty() || positions.any { it.x !in -180.0..180.0 || it.y !in -90.0..90.0 }) {
            return@withContext null
        }
        val path = "offline_maps/${Uuid.random()}.geojson"
        storage.write(path, file.bytes)
        try {
            maps.add(
                VectorMap(
                    id = 0,
                    name = file.name.substringBeforeLast('.'),
                    type = VectorMapFileType.GeoJson,
                    path = path,
                    sizeBytes = file.bytes.size.toLong(),
                    createdOn = Clock.System.now(),
                    bounds = CoordinateBounds.from(positions.map { it.coordinate }),
                    attribution = null,
                    visible = true
                )
            )
        } catch (exception: Exception) {
            runCatching { storage.delete(path) }
            throw exception
        }
    }

    suspend fun read(map: VectorMap): GeoJsonObject? = withContext(ioDispatcher) {
        if (map.type == VectorMapFileType.GeoJson && canRead(map.path)) {
            GeoJsonConvert.fromJson(storage.readText(map.path))
        } else null
    }

    override fun canRead(path: String) = isManagedPath(path) && storage.exists(path)
    override fun delete(path: String) {
        if (isManagedPath(path)) storage.delete(path)
    }

    override fun releaseExternal(path: String) = Unit
    override suspend fun copyToAppStorage(map: VectorMap): VectorMap? = null
    override fun populatePhotoMap(map: PhotoMapEntity) = map.toMap()

    private fun isManagedPath(path: String) = MANAGED_PATH.matches(path)

    companion object {
        private val MANAGED_PATH = Regex("offline_maps/[0-9a-f-]+\\.geojson")
    }
}
