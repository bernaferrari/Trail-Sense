package com.kylecorry.trail_sense.shared

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.luna.cache.MemoryLRUCache
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.data.SharedEncodedDataImageReader
import com.kylecorry.trail_sense.shared.data.SharedGeographicImageSource
import com.kylecorry.trail_sense.shared.data.SharedImageSize
import com.kylecorry.trail_sense.shared.data.SharedPixelDecoders

class SharedGeoidService : GeoidService {
    private val cache = MemoryLRUCache<PixelCoordinate, Float>(size = 5)
    private val source = SharedGeographicImageSource(
        SharedEncodedDataImageReader(
            imageSize = SharedImageSize(360, 180),
            assetPath = "geoids.webp",
            maxChannels = 1,
            decoder = SharedPixelDecoders.scaledDecoder(
                a = 1.3232228755950928,
                b = 106.9103012084961
            )
        ),
        interpolationOrder = 2,
        valuePixelOffset = 0.5f
    )

    override suspend fun getGeoid(location: Coordinate): Float = onIO {
        val pixel = source.getPixel(location)
        cache.getOrPut(pixel) {
            source.read(location).firstOrNull() ?: 0f
        }
    }

    override fun isSameGeoid(location1: Coordinate, location2: Coordinate): Boolean {
        return source.getPixel(location1) == source.getPixel(location2)
    }
}
