package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.mapsforge

import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.extensions.lineString
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.extensions.polygon
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.OfflineMapBlob
import kotlinx.coroutines.yield
import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.floor
import kotlin.math.sinh
import kotlin.math.tan

/**
 * Reads Mapsforge v3–5 tiles according to the published binary format. The picker and storage never
 * materialize a whole regional map in memory. The existing Compose GeoJSON layers draw the features.
 */
internal class MapsforgeReader private constructor(
    private val blob: OfflineMapBlob,
    val bounds: CoordinateBounds,
    private val debug: Boolean,
    private val points: List<String>,
    private val ways: List<String>,
    private val intervals: List<Interval>
) {
    suspend fun read(bounds: CoordinateBounds, zoom: Int): List<GeoJsonFeature> {
        if (!this.bounds.intersects(bounds)) return emptyList()
        val windows = if (bounds.west <= bounds.east) listOf(bounds)
        else listOf(bounds.copy(east = 180.0), bounds.copy(west = -180.0))

        val requestedZoom = zoom.coerceIn(intervals.first().min, intervals.last().max)
        val requestedInterval = intervals.last { it.min <= requestedZoom }
        val sections = intervals.filter { it.min <= requestedInterval.min }.asReversed()
        val interval = sections.firstOrNull { candidate ->
            val extent = tiles(this.bounds, candidate.base)
            windows.sumOf { extent.intersection(tiles(it, candidate.base))?.count ?: 0L } <= MAX_TILES
        } ?: return emptyList()
        val mapTiles = tiles(this.bounds, interval.base)
        val features = mutableListOf<GeoJsonFeature>()
        val indexStart = interval.start + if (debug) 16 else 0
        for (window in windows) {
            val visible = mapTiles.intersection(tiles(window, interval.base)) ?: continue
            for (y in visible.top..visible.bottom) {
                val first = (y - mapTiles.top).toLong() * mapTiles.width + visible.left - mapTiles.left
                val num = visible.width
                val entries = minOf(num.toLong() + 1, mapTiles.count - first).toInt()
                val index = Cursor(blob.read(indexStart + first * 5, entries * 5))
                val offsets = List(entries) { index.long(5) and OFFSET_MASK }
                for (column in 0 until num) {
                    val start = offsets[column]
                    val end = offsets.getOrElse(column + 1) { interval.size }
                    require(start >= indexStart - interval.start + mapTiles.count * 5 && end in start..interval.size)
                    if (end == start) continue
                    require(end - start <= MAX_BLOCK_BYTES)
                    val x = visible.left + column
                    val origin = Coordinate(tileLatitude(y, interval.base), tileLongitude(x, interval.base))
                    val data = Cursor(blob.read(interval.start + start, (end - start).toInt()))
                    readTile(data, origin, interval, requestedZoom, features)
                    if (features.size >= MAX_FEATURES) return features
                    yield()
                }
            }
        }
        return features
    }

    private fun readTile(
        data: Cursor,
        origin: Coordinate,
        interval: Interval,
        zoom: Int,
        features: MutableList<GeoJsonFeature>
    ) {
        if (debug) data.skip(32)
        var pointCount = 0
        var wayCount = 0
        val limit = zoom.coerceIn(interval.min, interval.max)
        for (level in interval.min..interval.max) {
            val pois = data.variable()
            val objects = data.variable()
            if (level <= limit) {
                pointCount += pois
                wayCount += objects
            }
        }
        val wayOffset = data.variable().toLong() + data.position
        require(wayOffset <= data.end && pointCount in 0..MAX_FEATURES && wayCount in 0..MAX_FEATURES)
        if (zoom >= 12) {
            val pointData = Cursor(data.bytes, data.position, wayOffset.toInt())
            repeat(pointCount) {
                readPoint(pointData, origin)?.let(features::add)
            }
        }
        data.position = wayOffset.toInt()
        repeat(wayCount) {
            if (features.size >= MAX_FEATURES) return
            if (debug) data.skip(32)
            val length = data.variable()
            val way = data.take(length)
            readWay(way, origin, features)
        }
    }

    private fun readPoint(data: Cursor, origin: Coordinate): GeoJsonFeature? {
        if (debug) data.skip(32)
        val coordinate = Coordinate(
            origin.latitude + data.variable(signed = true) / MICRODEGREES,
            origin.longitude + data.variable(signed = true) / MICRODEGREES
        )
        val tags = tags(data, points)
        val flags = data.byte()
        val name = if (flags and 0x80 != 0) data.string().substringBefore('\r') else null
        if (flags and 0x40 != 0) data.string()
        if (flags and 0x20 != 0) data.variable(signed = true)
        return if (tags.any { it.startsWith("natural=peak") || it.startsWith("tourism=") || it.startsWith("amenity=drinking_water") }) {
            GeoJsonFeature.point(coordinate, name = name, color = 0xff526c65.toInt(), size = 5f, isClickable = name != null)
        } else null
    }

    private fun readWay(data: Cursor, origin: Coordinate, features: MutableList<GeoJsonFeature>) {
        data.skip(2) // Subtile mask. Read all geometry in any intersecting base tile.
        val tags = tags(data, ways)
        val flags = data.byte()
        val name = if (flags and 0x80 != 0) data.string().substringBefore('\r') else null
        if (flags and 0x40 != 0) data.string()
        if (flags and 0x20 != 0) data.string()
        if (flags and 0x10 != 0) {
            data.variable(signed = true)
            data.variable(signed = true)
        }
        val blocks = if (flags and 0x08 != 0) data.variable() else 1
        require(blocks in 1..MAX_FEATURES && blocks <= data.remaining)
        repeat(blocks) {
            val rings = List(data.variable().also { require(it in 1..MAX_FEATURES && it <= data.remaining / 2) }) {
                readCoordinates(data, origin, flags and 0x04 != 0)
            }
            val first = rings.first()
            val color = areaColor(tags)
            if (first.size > 3 && first.first() == first.last() && color != null) {
                features.add(GeoJsonFeature.polygon(rings, name = name, color = color))
            } else {
                val stroke = lineColor(tags)
                for (line in rings) if (line.size >= 2) {
                    features.add(GeoJsonFeature.lineString(line, name = name, color = stroke, thicknessScale = .8f))
                }
            }
        }
    }

    private fun tags(data: Cursor, dictionary: List<String>): List<String> {
        val tags = List(data.byte() and 0x0f) { dictionary[data.variable()] }
        return tags.map { tag ->
            when (tag.substringAfter('=', "")) {
                "%b" -> data.skip(1)
                "%h" -> data.skip(2)
                "%i", "%f" -> data.skip(4)
                "%s" -> data.string()
            }
            tag
        }
    }

    private fun readCoordinates(data: Cursor, origin: Coordinate, doubleDelta: Boolean): List<Coordinate> {
        val count = data.variable()
        require(count in 2..32_767 && count <= data.remaining / 2)
        var lat = 0L
        var lon = 0L
        var lastLatDelta = 0L
        var lastLonDelta = 0L
        return List(count) { index ->
            val latDelta = data.variable(signed = true).toLong() + lastLatDelta
            val lonDelta = data.variable(signed = true).toLong() + lastLonDelta
            lat += latDelta
            lon += lonDelta
            if (doubleDelta && index > 0) {
                lastLatDelta = latDelta
                lastLonDelta = lonDelta
            }
            Coordinate(origin.latitude + lat / MICRODEGREES, origin.longitude + lon / MICRODEGREES)
        }
    }

    private data class Interval(val base: Int, val min: Int, val max: Int, val start: Long, val size: Long)
    private data class Tiles(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        val width = right - left + 1
        val count = width.toLong() * (bottom - top + 1)
        fun intersection(other: Tiles): Tiles? {
            val left = maxOf(left, other.left)
            val top = maxOf(top, other.top)
            val right = minOf(right, other.right)
            val bottom = minOf(bottom, other.bottom)
            return if (left <= right && top <= bottom) Tiles(left, top, right, bottom) else null
        }
    }

    private class Cursor(val bytes: ByteArray, start: Int = 0, val end: Int = bytes.size) {
        var position = start
        val remaining get() = end - position

        fun byte(): Int {
            require(position < end) { "Truncated Mapsforge section" }
            return bytes[position++].toInt() and 0xff
        }

        fun long(length: Int): Long {
            var value = 0L
            repeat(length) { value = (value shl 8) or byte().toLong() }
            return value
        }

        fun variable(signed: Boolean = false): Int {
            var value = 0L
            for (shift in 0..28 step 7) {
                val next = byte()
                val more = next and 0x80 != 0
                val signBit = if (!more && signed) next and 0x40 else 0
                val mask = if (!more && signed) 0x3f else 0x7f
                value = value or ((next and mask).toLong() shl shift)
                if (!more) {
                    require(value <= Int.MAX_VALUE)
                    return if (signBit != 0) -value.toInt() else value.toInt()
                }
            }
            error("Invalid Mapsforge number")
        }

        fun skip(length: Int) {
            require(length in 0..remaining)
            position += length
        }

        fun take(length: Int): Cursor {
            val start = position
            skip(length)
            return Cursor(bytes, start, position)
        }

        fun string(length: Int = variable()): String {
            val start = position
            skip(length)
            return bytes.decodeToString(start, position)
        }
    }

    companion object {
        private const val OFFSET_MASK = 0x7fffffffffL
        private const val MICRODEGREES = 1_000_000.0
        private const val MAX_TILES = 160L
        private const val MAX_FEATURES = 20_000
        private const val MAX_BLOCK_BYTES = 4L * 1024 * 1024

        suspend fun open(blob: OfflineMapBlob): MapsforgeReader {
            val prefix = Cursor(blob.read(0, 24))
            require(prefix.string(20) == "mapsforge binary OSM")
            val headerBytes = prefix.long(4).toInt()
            require(headerBytes in 40..1_000_000 && 24L + headerBytes <= blob.size)
            val data = Cursor(blob.read(24, headerBytes))
            require(data.long(4).toInt() in 3..5)
            require(data.long(8) == blob.size)
            data.skip(8) // File creation date.
            val south = data.long(4).toInt() / MICRODEGREES
            val west = data.long(4).toInt() / MICRODEGREES
            val north = data.long(4).toInt() / MICRODEGREES
            val east = data.long(4).toInt() / MICRODEGREES
            require(south in -90.0..north && north <= 90 && west in -180.0..east && east <= 180)
            val bounds = CoordinateBounds(north, east, south, west)
            require(data.long(2) > 0 && data.string() == "Mercator")
            val flags = data.byte()
            if (flags and 0x40 != 0) data.skip(8)
            if (flags and 0x20 != 0) data.skip(1)
            if (flags and 0x10 != 0) data.string()
            if (flags and 0x08 != 0) data.string()
            if (flags and 0x04 != 0) data.string()
            val points = List(data.long(2).toInt()) { data.string() }
            val ways = List(data.long(2).toInt()) { data.string() }
            val intervals = List(data.byte().also { require(it in 1..32) }) {
                val base = data.byte()
                val min = data.byte()
                val max = data.byte()
                val start = data.long(8)
                val size = data.long(8)
                require(base in 0..22 && min in 0..max && max <= 30)
                require(size >= 5 && start in (24L + headerBytes)..(blob.size - size))
                Interval(base, min, max, start, size)
            }.sortedBy { it.min }
            return MapsforgeReader(blob, bounds, flags and 0x80 != 0, points, ways, intervals)
        }

        private fun tiles(bounds: CoordinateBounds, zoom: Int): Tiles {
            val scale = 1 shl zoom
            fun x(lon: Double) = floor((lon + 180) / 360 * scale).toInt().coerceIn(0, scale - 1)
            fun y(lat: Double) = floor((1 - asinh(tan(lat.coerceIn(-85.05112878, 85.05112878) * PI / 180)) / PI) / 2 * scale)
                .toInt().coerceIn(0, scale - 1)
            return Tiles(x(bounds.west), y(bounds.north), x(bounds.east), y(bounds.south))
        }

        private fun tileLongitude(x: Int, zoom: Int) = x.toDouble() / (1 shl zoom) * 360 - 180
        private fun tileLatitude(y: Int, zoom: Int) = atan(sinh(PI * (1 - 2.0 * y / (1 shl zoom)))) * 180 / PI

        private fun areaColor(tags: List<String>): Int? = when {
            tags.any { it == "natural=water" || it == "waterway=riverbank" } -> 0xff688fa7.toInt()
            tags.any { it == "natural=wood" || it == "landuse=forest" || it == "leisure=park" } -> 0xff718570.toInt()
            tags.any { it.startsWith("building=") || it.startsWith("landuse=residential") } -> 0xffa49a8e.toInt()
            tags.any { it.startsWith("landuse=") || it.startsWith("leisure=") } -> 0xff899479.toInt()
            else -> null
        }

        private fun lineColor(tags: List<String>): Int = when {
            tags.any { it.startsWith("waterway=") } -> 0xff6794b0.toInt()
            tags.any { it.startsWith("highway=") } -> 0xffba9a71.toInt()
            tags.any { it.startsWith("contour=") } -> 0xff9a897e.toInt()
            else -> 0xff858982.toInt()
        }
    }
}
