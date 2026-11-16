package com.kylecorry.trail_sense.shared.data

import com.kylecorry.andromeda.bitmaps.FloatBitmap
import com.kylecorry.andromeda.bitmaps.ImageRegionDecoder
import com.kylecorry.andromeda.bitmaps.decodeWrappedRegion
import com.kylecorry.andromeda.bitmaps.samplePixel
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.luna.cache.MemoryLRUCache
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.sol.math.MathExtensions.roundPlaces
import com.kylecorry.sol.math.arithmetic.Arithmetic
import com.kylecorry.sol.math.arithmetic.Arithmetic.wrap
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.generated.resources.Res
import kotlinx.coroutines.CancellationException
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

data class SharedImageSize(val width: Int, val height: Int)

data class SharedImageRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

internal object BundledAssetCache {
    private val bytesCache = MemoryLRUCache<String, ByteArray>(size = 16)

    suspend fun readBytes(assetPath: String): ByteArray? = onIO {
        val clean = assetPath.trimStart('/')
        if (clean.split('/').any { it == ".." }) return@onIO null
        val cached = bytesCache.get(clean)
        if (cached != null) return@onIO cached
        try {
            val loaded = Res.readBytes("files/trailimages/$clean")
            bytesCache.put(clean, loaded)
            loaded
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    suspend fun samplePixel(assetPath: String, x: Float, y: Float, interpolate: Boolean = false): Int? = onIO {
        val bytes = readBytes(assetPath) ?: return@onIO null
        val decoder = ImageRegionDecoder(bytes)
        try {
            decoder.samplePixel(x, y, interpolate)
        } finally {
            decoder.close()
        }
    }
}

interface SharedPixelDecoder {
    val channels: Int
    fun decode(pixel: Int, dest: FloatArray)
}

object SharedPixelDecoders {
    fun scaledDecoder(a: Double, b: Double, convertZero: Boolean = true): SharedPixelDecoder =
        object : SharedPixelDecoder {
            override val channels = 4
            override fun decode(pixel: Int, dest: FloatArray) {
                val red = ((pixel ushr 16) and 0xff).toFloat()
                val green = ((pixel ushr 8) and 0xff).toFloat()
                val blue = (pixel and 0xff).toFloat()
                val alpha = ((pixel ushr 24) and 0xff).toFloat()

                if (!convertZero && red == 0f && green == 0f && blue == 0f) {
                    dest[0] = 0f
                    dest[1] = 0f
                    dest[2] = 0f
                    dest[3] = alpha
                } else {
                    dest[0] = (red / a - b).toFloat()
                    dest[1] = (green / a - b).toFloat()
                    dest[2] = (blue / a - b).toFloat()
                    dest[3] = (alpha / a - b).toFloat()
                }
            }
        }

    fun split16BitDecoder(a: Double = 1.0, b: Double = 0.0): SharedPixelDecoder =
        object : SharedPixelDecoder {
            override val channels = 2
            override fun decode(pixel: Int, dest: FloatArray) {
                val red = (pixel ushr 16) and 0xff
                val green = (pixel ushr 8) and 0xff
                val blue = pixel and 0xff
                val alpha = (pixel ushr 24) and 0xff
                dest[0] = ((green shl 8 or red).toDouble() / a - b).toFloat()
                dest[1] = ((alpha shl 8 or blue).toDouble() / a - b).toFloat()
            }
        }

    fun scaledDecoderWithMask(a: Double, b: Double, maskChannel: Int = 2): SharedPixelDecoder =
        object : SharedPixelDecoder {
            override val channels = 2
            override fun decode(pixel: Int, dest: FloatArray) {
                val red = (pixel ushr 16) and 0xff
                val green = (pixel ushr 8) and 0xff
                val blue = pixel and 0xff
                val alpha = (pixel ushr 24) and 0xff
                dest[0] = (red / a - b).toFloat()
                dest[1] = when (maskChannel) {
                    0 -> red
                    1 -> green
                    2 -> blue
                    else -> alpha
                }.toFloat()
            }
        }

    fun split16BitDecoderWithMask(a: Double = 1.0, b: Double = 0.0, maskChannel: Int = 2): SharedPixelDecoder =
        object : SharedPixelDecoder {
            override val channels = 2
            override fun decode(pixel: Int, dest: FloatArray) {
                val red = (pixel ushr 16) and 0xff
                val green = (pixel ushr 8) and 0xff
                val blue = pixel and 0xff
                val alpha = (pixel ushr 24) and 0xff
                dest[0] = ((green shl 8 or red).toDouble() / a - b).toFloat()
                dest[1] = when (maskChannel) {
                    0 -> red
                    1 -> green
                    2 -> blue
                    else -> alpha
                }.toFloat()
            }
        }
}

class SharedEncodedDataImageReader(
    private val imageSize: SharedImageSize,
    private val assetPath: String,
    private val treatZeroAsNaN: Boolean = false,
    private val maxChannels: Int? = null,
    private val decoder: SharedPixelDecoder
) {
    val channels: Int = maxChannels ?: decoder.channels

    fun getSize(): SharedImageSize = imageSize

    suspend fun getRegion(rect: SharedImageRect): Pair<FloatBitmap, Boolean>? = onIO {
        val bytes = BundledAssetCache.readBytes(assetPath) ?: return@onIO null
        val decoderRegion = ImageRegionDecoder(bytes)
        try {
            val left = rect.left.coerceIn(0, (decoderRegion.width - 1).coerceAtLeast(0))
            val top = rect.top.coerceIn(0, (decoderRegion.height - 1).coerceAtLeast(0))
            val right = rect.right.coerceIn(left, decoderRegion.width)
            val bottom = rect.bottom.coerceIn(top, decoderRegion.height)
            val width = right - left
            val height = bottom - top
            if (width <= 0 || height <= 0) return@onIO null

            val image = decoderRegion.decode(left, top, width, height)
            val outChannels = minOf(decoder.channels, channels)
            val pixelGrid = FloatBitmap(image.width, image.height, outChannels)
            val data = pixelGrid.data
            val buffer = FloatArray(decoder.channels)
            var isAllNaN = true
            var idx = 0

            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    decoder.decode(image[x, y], buffer)
                    for (c in 0 until outChannels) {
                        val value = buffer[c]
                        val finalValue = if ((treatZeroAsNaN && Arithmetic.isZero(value)) || value.isNaN()) {
                            Float.NaN
                        } else {
                            isAllNaN = false
                            value
                        }
                        data[idx++] = finalValue
                    }
                }
            }
            pixelGrid to !isAllNaN
        } finally {
            decoderRegion.close()
        }
    }
}

class SharedCrossBoundaryPixelProvider(
    private val primarySource: SharedGeographicImageSource,
    private val primaryBitmap: FloatBitmap,
    private val primaryRect: SharedImageRect,
    private val neighborSources: List<SharedGeographicImageSource>
) {
    private val neighborCache =
        mutableMapOf<SharedGeographicImageSource, MutableList<Pair<FloatBitmap, SharedImageRect>>>()

    suspend fun getPixel(x: Int, y: Int, channel: Int): Float? {
        val localX = x - primaryRect.left
        val localY = y - primaryRect.top

        if (localX in 0 until primaryBitmap.width && localY in 0 until primaryBitmap.height) {
            val value = primaryBitmap.get(localX, localY, channel)
            if (!value.isNaN()) return value
        }

        val location = primarySource.getLocation(PixelCoordinate(x.toFloat(), y.toFloat()))
        for (neighbor in neighborSources) {
            if (!neighbor.contains(location)) continue
            val cached = getFromCache(neighbor, location, channel)
            if (cached != null) return cached
            val loaded = loadNeighborRegion(neighbor, location) ?: continue
            val value = getValueFromRegion(neighbor, loaded, location, channel)
            if (value != null && !value.isNaN()) return value
        }
        return null
    }

    private fun getFromCache(
        neighbor: SharedGeographicImageSource,
        location: Coordinate,
        channel: Int
    ): Float? {
        val regions = neighborCache[neighbor] ?: return null
        for (region in regions) {
            val value = getValueFromRegion(neighbor, region, location, channel)
            if (value != null && !value.isNaN()) return value
        }
        return null
    }

    private fun getValueFromRegion(
        neighbor: SharedGeographicImageSource,
        regionData: Pair<FloatBitmap, SharedImageRect>,
        location: Coordinate,
        channel: Int
    ): Float? {
        val (bitmap, rect) = regionData
        val neighborPixel = neighbor.getPixel(location, clamp = false)
        val localX = neighborPixel.x.roundToInt() - rect.left
        val localY = neighborPixel.y.roundToInt() - rect.top
        if (localX in 0 until bitmap.width && localY in 0 until bitmap.height) {
            return bitmap.get(localX, localY, channel)
        }
        return null
    }

    private suspend fun loadNeighborRegion(
        source: SharedGeographicImageSource,
        location: Coordinate
    ): Pair<FloatBitmap, SharedImageRect>? {
        val pixel = source.getPixel(location, clamp = false)
        val size = source.imageSize
        val padding = 8
        val centerX = pixel.x.roundToInt()
        val centerY = pixel.y.roundToInt()
        val left = (centerX - padding).coerceAtLeast(0)
        val top = (centerY - padding).coerceAtLeast(0)
        val right = (centerX + padding + 1).coerceAtMost(size.width)
        val bottom = (centerY + padding + 1).coerceAtMost(size.height)
        if (right <= left || bottom <= top) return null
        val rect = SharedImageRect(left, top, right, bottom)
        val result = source.getRegion(rect) ?: return null
        val regionData = result.first to rect
        neighborCache.getOrPut(source) { mutableListOf() }.add(regionData)
        return regionData
    }
}

class SharedFloatBitmapInterpolator(
    interpolationOrder: Int,
    private val pixelProvider: SharedCrossBoundaryPixelProvider? = null
) {
    private val smoothInterpolator = when (interpolationOrder) {
        2 -> BicubicInterpolator()
        1 -> BilinearInterpolator()
        else -> null
    }

    suspend fun getValue(
        bitmap: FloatBitmap,
        rect: SharedImageRect,
        x: Float,
        y: Float,
        dest: FloatArray,
        destOffset: Int = 0
    ) {
        if (bitmap.width == 0 || bitmap.height == 0 || bitmap.channels == 0) return
        val localPixel = PixelCoordinate(x - rect.left, y - rect.top)
        for (i in 0 until bitmap.channels) {
            dest[destOffset + i] = interpolate(localPixel, bitmap, rect, i)
        }
    }

    private suspend fun interpolate(
        localPixel: PixelCoordinate,
        bitmap: FloatBitmap,
        rect: SharedImageRect,
        channel: Int
    ): Float {
        val provider = pixelProvider ?: return fallbackInterpolate(localPixel, bitmap, channel)
        val interpolator = smoothInterpolator ?: NearestInterpolator(max(bitmap.width, bitmap.height))
        return interpolator.interpolate(localPixel) { x, y ->
            val value = bitmap.getOrNull(x, y, channel)
            if (value != null && !value.isNaN()) {
                value
            } else {
                provider.getPixel(x + rect.left, y + rect.top, channel)
            }
        } ?: fallbackInterpolate(localPixel, bitmap, channel)
    }

    private suspend fun fallbackInterpolate(
        localPixel: PixelCoordinate,
        bitmap: FloatBitmap,
        channel: Int
    ): Float {
        val value = smoothInterpolator?.interpolate(localPixel) { x, y ->
            bitmap.getOrNull(x, y, channel)
        }
        if (value != null) return value
        return NearestInterpolator(max(bitmap.width, bitmap.height)).interpolate(localPixel) { x, y ->
            bitmap.getOrNull(x, y, channel)
        } ?: 0f
    }
}

class SharedGeographicImageSource(
    private val reader: SharedEncodedDataImageReader,
    val bounds: CoordinateBounds = CoordinateBounds.world,
    private val precision: Int = 2,
    private val valuePixelOffset: Float = 0f,
    private val interpolationOrder: Int = 1,
    private val verticalResolutionDegrees: Double? = null
) {
    val imageSize = reader.getSize()
    private val horizontalRes = bounds.widthDegrees() / imageSize.width
    private val verticalRes = verticalResolutionDegrees ?: (bounds.heightDegrees() / imageSize.height)
    private val west = bounds.west + horizontalRes * valuePixelOffset
    private val north = bounds.north - verticalRes * valuePixelOffset

    private fun getX(longitude: Double): Double = (longitude - west) / horizontalRes
    private fun getY(latitude: Double): Double = (north - latitude) / verticalRes

    fun getPixel(location: Coordinate, clamp: Boolean = true): PixelCoordinate {
        var x = getX(location.longitude)
        var y = getY(location.latitude)
        if (x.isNaN()) x = 0.0
        if (y.isNaN()) y = 0.0
        if (!clamp) {
            return PixelCoordinate(x.roundPlaces(precision).toFloat(), y.roundPlaces(precision).toFloat())
        }
        return PixelCoordinate(
            x.roundPlaces(precision).toFloat().coerceIn(-valuePixelOffset, imageSize.width.toFloat() - 1f + valuePixelOffset),
            y.roundPlaces(precision).toFloat().coerceIn(-valuePixelOffset, imageSize.height.toFloat() - 1f + valuePixelOffset)
        )
    }

    fun getLocation(pixel: PixelCoordinate): Coordinate {
        val longitude = (pixel.x + valuePixelOffset) * horizontalRes + bounds.west
        val latitude = bounds.north - (pixel.y + valuePixelOffset) * verticalRes
        return Coordinate(latitude.roundPlaces(precision), Coordinate.toLongitude(longitude.roundPlaces(precision)))
    }

    suspend fun read(
        location: Coordinate,
        neighborSources: List<SharedGeographicImageSource> = emptyList()
    ): FloatArray = onIO {
        val output = FloatBitmap(1, 1, reader.channels)
        read(doubleArrayOf(location.latitude), doubleArrayOf(location.longitude), output, neighborSources)
        output.data
    }

    suspend fun read(pixel: PixelCoordinate): FloatArray = onIO {
        val output = FloatBitmap(1, 1, reader.channels)
        val left = (floor(pixel.x).toInt() - interpolationOrder).coerceAtLeast(0)
        val top = (floor(pixel.y).toInt() - interpolationOrder).coerceAtLeast(0)
        val right = (floor(pixel.x).toInt() + 1 + interpolationOrder).coerceAtMost(imageSize.width)
        val bottom = (floor(pixel.y).toInt() + 1 + interpolationOrder).coerceAtMost(imageSize.height)
        if (right > left && bottom > top) {
            val rect = SharedImageRect(left, top, right, bottom)
            val region = reader.getRegion(rect)
            if (region != null && region.second) {
                SharedFloatBitmapInterpolator(interpolationOrder).getValue(region.first, rect, pixel.x, pixel.y, output.data)
            }
        }
        output.data
    }

    suspend fun read(
        latitudes: DoubleArray,
        longitudes: DoubleArray,
        output: FloatBitmap,
        neighborSources: List<SharedGeographicImageSource> = emptyList()
    ) = onIO {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        val yPixels = FloatArray(latitudes.size)
        val yInside = BooleanArray(latitudes.size)
        for (i in latitudes.indices) {
            val lat = latitudes[i]
            if (!bounds.containsLatitude(lat)) continue
            val y = getY(lat).toFloat()
            yPixels[i] = y
            yInside[i] = true
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }
        if (minY == Float.MAX_VALUE) return@onIO

        val xPixels = FloatArray(longitudes.size)
        val xInside = BooleanArray(longitudes.size)
        for (i in longitudes.indices) {
            val lon = longitudes[i]
            if (!bounds.containsLongitude(lon)) continue
            val x = getX(lon).toFloat()
            xPixels[i] = x
            xInside[i] = true
            if (x < minX) minX = x
            if (x > maxX) maxX = x
        }
        if (minX == Float.MAX_VALUE) return@onIO

        val left = (floor(minX).toInt() - interpolationOrder).coerceAtLeast(0)
        val top = (floor(minY).toInt() - interpolationOrder).coerceAtLeast(0)
        val right = (floor(maxX).toInt() + 1 + interpolationOrder).coerceAtMost(imageSize.width)
        val bottom = (floor(maxY).toInt() + 1 + interpolationOrder).coerceAtMost(imageSize.height)
        if (right <= left || bottom <= top) return@onIO

        val rect = SharedImageRect(left, top, right, bottom)
        val (pixelGrid, hasData) = reader.getRegion(rect) ?: return@onIO
        if (!hasData) return@onIO

        val needsCrossBoundary = neighborSources.isNotEmpty() && interpolationOrder > 0 &&
            (left == 0 || top == 0 || right == imageSize.width || bottom == imageSize.height)
        val pixelProvider = if (needsCrossBoundary) {
            val actualNeighbors = neighborSources.filter { it !== this@SharedGeographicImageSource }
            if (actualNeighbors.isNotEmpty()) {
                SharedCrossBoundaryPixelProvider(this@SharedGeographicImageSource, pixelGrid, rect, actualNeighbors)
            } else null
        } else null

        val interpolator = SharedFloatBitmapInterpolator(interpolationOrder, pixelProvider)
        val outputData = output.data
        val outputWidth = output.width
        val channels = output.channels

        for (yIdx in latitudes.indices) {
            if (!yInside[yIdx]) continue
            val y = yPixels[yIdx]
            for (xIdx in longitudes.indices) {
                if (!xInside[xIdx]) continue
                val x = xPixels[xIdx]
                val offset = (yIdx * outputWidth + xIdx) * channels
                interpolator.getValue(pixelGrid, rect, x, y, outputData, offset)
            }
        }
    }

    fun contains(location: Coordinate): Boolean = bounds.contains(location)

    suspend fun getRegion(rect: SharedImageRect): Pair<FloatBitmap, Boolean>? = onIO {
        reader.getRegion(rect)
    }

    companion object {
        suspend fun getNearestPixelOfAsset(
            source: SharedGeographicImageSource,
            location: Coordinate,
            assetPath: String,
            searchSize: Int = 5,
            hasValue: (Int) -> Boolean,
            hasMappedValue: (FloatArray) -> Boolean
        ): PixelCoordinate? = onIO {
            val actualPixel = source.getPixel(location)
            val sourceValue = source.read(actualPixel)
            if (hasMappedValue(sourceValue)) return@onIO actualPixel

            val bytes = BundledAssetCache.readBytes(assetPath) ?: return@onIO null
            val decoder = ImageRegionDecoder(bytes)
            try {
                val cx = actualPixel.x.roundToInt()
                val cy = actualPixel.y.roundToInt()
                val side = searchSize * 2 + 1
                val region = decoder.decodeWrappedRegion(
                    left = cx - searchSize,
                    top = cy - searchSize,
                    width = side,
                    height = side,
                    wrapHorizontally = true
                )
                val x = searchSize
                val y = searchSize
                for (i in 1 until searchSize) {
                    val topY = y - i
                    val bottomY = y + i
                    val leftX = x - i
                    val rightX = x + i
                    val hits = mutableListOf<PixelCoordinate>()
                    for (j in leftX..rightX) {
                        if (hasValue(region[j, topY])) hits.add(PixelCoordinate(j.toFloat(), topY.toFloat()))
                        if (hasValue(region[j, bottomY])) hits.add(PixelCoordinate(j.toFloat(), bottomY.toFloat()))
                    }
                    for (j in topY..bottomY) {
                        if (hasValue(region[leftX, j])) hits.add(PixelCoordinate(leftX.toFloat(), j.toFloat()))
                        if (hasValue(region[rightX, j])) hits.add(PixelCoordinate(rightX.toFloat(), j.toFloat()))
                    }
                    if (hits.isNotEmpty()) {
                        val globalHits = hits.map {
                            val globalX = wrap(actualPixel.x + it.x - searchSize, 0f, source.imageSize.width.toFloat())
                            val globalY = actualPixel.y + it.y - searchSize
                            PixelCoordinate(globalX, globalY)
                        }
                        return@onIO globalHits.minByOrNull { it.distanceTo(actualPixel) } ?: actualPixel
                    }
                }
            } finally {
                decoder.close()
            }
            null
        }
    }
}
