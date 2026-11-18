package com.kylecorry.trail_sense.tools.offline_maps

import com.kylecorry.andromeda.geojson.GeoJsonLineString
import com.kylecorry.andromeda.geojson.GeoJsonPoint
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.OfflineMapBlob
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.mapsforge.MapsforgeReader
import kotlinx.coroutines.test.runTest
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class MapsforgeReaderTest {
    @Test
    fun readsPointAndDoubleDeltaWayInVisibleTile() = runTest {
        val file = map()
        val reads = mutableListOf<Pair<Long, Int>>()
        val reader = MapsforgeReader.open(blob(file, reads))

        assertEquals(1.0, reader.bounds.west)
        assertEquals(2, reads.size)
        val features = reader.read(reader.bounds, 12)
        assertEquals(2, features.size)
        val point = features.first().geometry as GeoJsonPoint
        assertEquals(1.5, point.point?.x)
        val way = features.last().geometry as GeoJsonLineString
        assertEquals(listOf(1.0, 1.25, 1.75), way.line?.map { it.x })
        assertEquals(4, reads.size)
    }

    @Test
    fun rejectsTruncatedHeaderWithoutReadingPastEnd() = runTest {
        assertFails { MapsforgeReader.open(blob(map().copyOf(30))) }
    }

    private fun blob(bytes: ByteArray, reads: MutableList<Pair<Long, Int>> = mutableListOf()) =
        object : OfflineMapBlob {
            override val size = bytes.size.toLong()
            override suspend fun read(offset: Long, length: Int): ByteArray {
                reads.add(offset to length)
                return bytes.copyOfRange(offset.toInt(), offset.toInt() + length)
            }
        }

    private fun map(): ByteArray {
        val point = Buffer().apply {
            signed(-65_013_260)
            signed(1_500_000)
            writeByte(1)
            unsigned(0)
            writeByte(0)
        }.readByteArray()
        val way = Buffer().apply {
            writeShort(0xffff)
            writeByte(1)
            unsigned(0)
            writeByte(0x04) // Double-delta coordinates.
            unsigned(1)
            unsigned(3)
            signed(-65_513_260)
            signed(1_000_000)
            signed(250_000)
            signed(250_000)
            signed(125_000)
            signed(250_000)
        }.readByteArray()
        val tile = Buffer().apply {
            unsigned(1)
            unsigned(1)
            unsigned(point.size)
            write(point)
            unsigned(way.size)
            write(way)
        }.readByteArray()
        val subFile = Buffer().writeByte(0).writeInt(5).write(tile).readByteArray()

        fun header(total: Long, start: Long): ByteArray = Buffer().apply {
            writeInt(5)
            writeLong(total)
            writeLong(0)
            writeInt(1_000_000)
            writeInt(1_000_000)
            writeInt(2_000_000)
            writeInt(2_000_000)
            writeShort(256)
            string("Mercator")
            writeByte(0)
            writeShort(1)
            string("natural=peak")
            writeShort(1)
            string("highway=path")
            writeByte(1)
            writeByte(2)
            writeByte(12)
            writeByte(12)
            writeLong(start)
            writeLong(subFile.size.toLong())
        }.readByteArray()
        val headerLength = header(0, 0).size
        val start = 24L + headerLength
        return Buffer().writeUtf8("mapsforge binary OSM").writeInt(headerLength)
            .write(header(start + subFile.size, start)).write(subFile).readByteArray()
    }

    private fun Buffer.string(value: String) {
        val bytes = value.encodeToByteArray()
        unsigned(bytes.size)
        write(bytes)
    }

    private fun Buffer.unsigned(value: Int) {
        var remaining = value
        while (remaining > 0x7f) {
            writeByte((remaining and 0x7f) or 0x80)
            remaining = remaining ushr 7
        }
        writeByte(remaining)
    }

    private fun Buffer.signed(value: Int) {
        var remaining = kotlin.math.abs(value)
        while (remaining > 0x3f) {
            writeByte((remaining and 0x7f) or 0x80)
            remaining = remaining ushr 7
        }
        writeByte(remaining or if (value < 0) 0x40 else 0)
    }
}
