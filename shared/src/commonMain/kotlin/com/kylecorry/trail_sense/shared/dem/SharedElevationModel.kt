package com.kylecorry.trail_sense.shared.dem

import com.kylecorry.andromeda.bitmaps.FloatBitmap
import com.kylecorry.andromeda.core.cache.GeospatialCache
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.luna.cache.MemoryCachedValue
import com.kylecorry.luna.cache.MemoryLRUCache
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.shared.SequentialExecutor
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.data.BundledAssetCache
import com.kylecorry.trail_sense.shared.data.SharedEncodedDataImageReader
import com.kylecorry.trail_sense.shared.data.SharedGeographicImageSource
import com.kylecorry.trail_sense.shared.data.SharedImageSize
import com.kylecorry.trail_sense.shared.data.SharedPixelDecoders
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class SharedCompressedDemIndex(
    val r: Int,
    val c: String,
    val v: String? = null,
    val w: Int,
    val h: Int,
    val f: List<List<Double>>,
    val wm: Boolean? = null
)

class SharedElevationModel : ElevationModel {

    private val json = Json { ignoreUnknownKeys = true }
    private var cache = GeospatialCache<DEMElevation>(Distance.meters(10f), size = 500)
    private var sourcesCache = MemoryCachedValue<List<SharedGeographicImageSource>>()
    private var hasWaterMask: Boolean = false
    private val sourcesLock = Mutex()
    private var gridCache = MemoryLRUCache<String, Triple<FloatBitmap, DoubleArray, DoubleArray>>(2)

    override suspend fun getElevation(location: Coordinate): DEMElevation = onDefault {
        cache.getOrPut(location) {
            val allSources = getSources()
            val source = allSources.firstOrNull { it.contains(location) }
                ?: return@getOrPut DEMElevation(0f, null, hasData = false)
            val waterMask = hasWaterMask
            val neighbors = getNeighbors(source, allSources)
            onIO {
                tryOrDefault(DEMElevation(0f, null, hasData = false)) {
                    val data = source.read(location, neighbors)
                    val elevation = data[0]
                    if (waterMask) {
                        DEMElevation(elevation, DEMElevationType.fromWaterMask(data.getOrElse(1) { 0f }))
                    } else {
                        DEMElevation(elevation, null)
                    }
                }
            }
        }
    }

    override suspend fun getContourLines(
        bounds: CoordinateBounds,
        interval: Float,
        resolution: Double
    ): List<Contour> = onDefault {
        val latitudes = Interpolation.getMultiplesBetween(bounds.south - resolution, bounds.north + resolution, resolution)
        val longitudes = Interpolation.getMultiplesBetween(
            bounds.west - resolution,
            (if (bounds.west < bounds.east) bounds.east else bounds.east + 360) + resolution,
            resolution
        )
        val key = "${latitudes.firstOrNull()}_${latitudes.lastOrNull()}_${longitudes.firstOrNull()}_${longitudes.lastOrNull()}_$resolution"
        val (bitmap, lats, lons) = gridCache.getOrPut(key) {
            val allSources = getSources()
            val expanded = bounds.grow(0.1f)
            val matching = allSources.filter { it.bounds.intersects(expanded) }
            val channels = if (hasWaterMask) 2 else 1
            val output = FloatBitmap(longitudes.size, latitudes.size, channels)
            for (i in longitudes.indices) longitudes[i] = Coordinate.toLongitude(longitudes[i])
            for (source in matching) {
                source.read(latitudes, longitudes, output, getNeighbors(source, allSources))
            }
            Triple(output, latitudes, longitudes)
        }
        ContourGenerator.generate(lats, lons, interval, SequentialExecutor()) { x, y ->
            bitmap.get(x, y, 0)
        }
    }

    override fun isExternalModel(): Boolean = false

    override fun invalidateCache() {
        cache = GeospatialCache(Distance.meters(10f), size = 500)
        gridCache = MemoryLRUCache(2)
        sourcesCache = MemoryCachedValue()
        hasWaterMask = false
    }

    private fun getNeighbors(
        source: SharedGeographicImageSource,
        sources: List<SharedGeographicImageSource>
    ): List<SharedGeographicImageSource> {
        val expanded = source.bounds.grow(0.1f)
        return sources.filter { it !== source && it.bounds.intersects(expanded) }
    }

    private suspend fun getSources(): List<SharedGeographicImageSource> = onIO {
        sourcesLock.withLock {
            sourcesCache.getOrPut {
                val indexText = BundledAssetCache.readBytes("dem/index.json")?.decodeToString()
                    ?: return@getOrPut emptyList()
                val parsed = json.decodeFromString<SharedCompressedDemIndex>(indexText)
                val waterMask = parsed.wm ?: false
                hasWaterMask = waterMask
                parsed.f.map { row ->
                    val filename = "dem/${row[0].toInt()}.webp"
                    val a = row[1]
                    val b = row[2]
                    val north = row[3]
                    val west = row[4]
                    val south = row[5]
                    val east = row[6]
                    val w = if (row.size > 7) row[7].toInt() else parsed.w
                    val h = if (row.size > 7) row[8].toInt() else parsed.h
                    val decoder = when {
                        parsed.c == "8-bit" && waterMask -> SharedPixelDecoders.scaledDecoderWithMask(a, b)
                        parsed.c == "8-bit" -> SharedPixelDecoders.scaledDecoder(a, b)
                        waterMask -> SharedPixelDecoders.split16BitDecoderWithMask(a, b)
                        else -> SharedPixelDecoders.split16BitDecoder(a, b)
                    }
                    SharedGeographicImageSource(
                        SharedEncodedDataImageReader(
                            imageSize = SharedImageSize(w, h),
                            assetPath = filename,
                            maxChannels = if (waterMask) 2 else 1,
                            decoder = decoder
                        ),
                        bounds = CoordinateBounds(north, east, south, west),
                        precision = 10,
                        valuePixelOffset = 0.7f,
                        interpolationOrder = 2
                    )
                }
            }
        }
    }
}
