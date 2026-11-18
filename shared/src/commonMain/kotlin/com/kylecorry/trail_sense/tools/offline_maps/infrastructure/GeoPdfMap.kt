package com.kylecorry.trail_sense.tools.offline_maps.infrastructure

import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.MapCalibrationPoint
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.MapProjectionType
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PercentCoordinate
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMapGeoreference
import kotlin.math.abs

/** Reads the standardized geospatial viewport from the page being rendered. */
class GeoPdfMap(val name: String, internal val bytes: ByteArray) {
    private val document = PdfGeoMetadata(bytes)
    val pageCount: Int get() = document.pageCount

    fun georeference(pageIndex: Int, size: Size): PhotoMapGeoreference {
        val page = document.readPage(pageIndex)
        val georeferences = page.viewports.mapNotNull { viewport ->
            val bounds = viewport.bounds
            val coordinates = viewport.coordinates
            val positions = viewport.landmarks.ifEmpty { listOf(0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 0.0) }
            if (bounds.size != 4 || coordinates.size < 4 || coordinates.size % 2 != 0 ||
                coordinates.size != positions.size || coordinates.size > 32
            ) return@mapNotNull null

            val left = minOf(bounds[0], bounds[2])
            val bottom = minOf(bounds[1], bounds[3])
            val width = abs(bounds[0] - bounds[2])
            val height = abs(bounds[1] - bounds[3])
            if (width == 0.0 || height == 0.0) return@mapNotNull null
            val points = coordinates.indices.step(2).mapNotNull { index ->
                val latitude = coordinates[index]
                val longitude = coordinates[index + 1]
                if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return@mapNotNull null
                val (xFraction, yFraction) = page.toDisplayFraction(
                    left + width * positions[index], bottom + height * positions[index + 1]
                )
                if (xFraction !in -0.00001f..1.00001f || yFraction !in -0.00001f..1.00001f) return@mapNotNull null
                MapCalibrationPoint(Coordinate(latitude, longitude), PercentCoordinate(
                    xFraction.coerceIn(0f, 1f), yFraction.coerceIn(0f, 1f)
                ))
            }
            // Trail Sense stores two landmarks. Prefer the most widely separated usable diagonal.
            val pair = points.flatMapIndexed { index, first ->
                points.drop(index + 1).mapNotNull { second ->
                    val dx = abs(first.imageLocation.x - second.imageLocation.x) * size.width
                    val dy = abs(first.imageLocation.y - second.imageLocation.y) * size.height
                    if (dx < 1f || dy < 1f || first.location == second.location) null
                    else (dx * dx + dy * dy) to listOf(first, second)
                }
            }.maxByOrNull { it.first }?.second ?: return@mapNotNull null

            val wkt = viewport.wkt
            val projection = when {
                wkt == null || wkt.contains("mercator", ignoreCase = true) -> MapProjectionType.Mercator
                else -> MapProjectionType.CylindricalEquidistant
            }
            width * height to PhotoMapGeoreference(
                size, projectionType = projection, isWarpingCompleted = true, calibrationPoints = pair
            )
        }
        return georeferences.maxByOrNull { it.first }?.second ?: PhotoMapGeoreference(size)
    }

}

internal expect class PdfGeoMetadata(bytes: ByteArray) {
    val pageCount: Int
    fun readPage(index: Int): PdfGeoPage
}

internal data class PdfGeoPage(
    val left: Double,
    val bottom: Double,
    val right: Double,
    val top: Double,
    val rotation: Int,
    val viewports: List<PdfGeoViewport>
) {
    fun toDisplayFraction(x: Double, y: Double): Pair<Float, Float> {
        val u = ((x - left) / (right - left)).toFloat()
        val v = ((y - bottom) / (top - bottom)).toFloat()
        return when (rotation) {
            90 -> v to u
            180 -> (1f - u) to v
            270 -> (1f - v) to (1f - u)
            else -> u to (1f - v)
        }
    }
}

internal data class PdfGeoViewport(
    val bounds: List<Double>,
    val coordinates: List<Double>,
    val landmarks: List<Double>,
    val wkt: String?
)
