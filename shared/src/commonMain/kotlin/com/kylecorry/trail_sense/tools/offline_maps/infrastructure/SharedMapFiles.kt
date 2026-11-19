package com.kylecorry.trail_sense.tools.offline_maps.infrastructure

import androidx.compose.ui.graphics.ImageBitmap
import com.kylecorry.andromeda.geojson.*
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.coroutines.ioDispatcher
import com.kylecorry.trail_sense.shared.extensions.normalize
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMapEntity
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMapGeoreference
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.VectorMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.VectorMapFileType
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.mapsforge.MapsforgeReader
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.persistence.MapFileAccess
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.nameWithoutExtension
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.uuid.Uuid

internal const val MAX_MAP_PDF_EDGE = 3072f

internal class SharedMapFiles(
    private val storage: OfflineMapStorage,
    private val openPdf: OpenOfflinePdfPage? = null
) : MapFileAccess {
    val extensions: List<String> get() =
        listOf("geojson", "json", "map", "jpg", "jpeg", "png", "webp") + listOfNotNull("pdf".takeIf { openPdf != null })

    suspend fun import(file: PlatformFile, maps: MapService): IMap = withContext(ioDispatcher) {
        val extension = file.extension.lowercase()
        require(extension in extensions && extension != "pdf")
        val name = file.nameWithoutExtension
        val id = Uuid.random().toString()
        val createdOn = Clock.System.now()
        val paths = mutableListOf<String>()
        suspend fun copy(path: String): OfflineMapBlob {
            paths.add(path)
            storage.copy(path, file)
            return requireNotNull(storage.open(path))
        }
        try {
            when (extension) {
                "map", "geojson", "json" -> {
                    val path = "offline_maps/$id.$extension"
                    val blob = copy(path)
                    val type = if (extension == "map") VectorMapFileType.Mapsforge else VectorMapFileType.GeoJson
                    val bounds = if (type == VectorMapFileType.Mapsforge) MapsforgeReader.open(blob).bounds
                    else geoJsonBounds(requireNotNull(readGeoJson(blob)))
                    val map = VectorMap(0, name, type, path, blob.size, createdOn, bounds,
                        if (type == VectorMapFileType.Mapsforge) "OpenStreetMap contributors" else null,
                        visible = true)
                    withContext(NonCancellable) { map.copy(id = maps.add(map)) }
                }
                else -> {
                    val path = "maps/$id.$extension"
                    val blob = copy(path)
                    val image = blob.readAll(MAX_IMAGE_BYTES).decodeToImageBitmap()
                    val size = Size(image.width.toFloat(), image.height.toFloat())
                    val map = PhotoMap(0, name, path, blob.size, PhotoMapGeoreference(size), createdOn = createdOn)
                    withContext(NonCancellable) { map.copy(id = maps.add(map)) }
                }
            }
        } catch (exception: Exception) {
            withContext(NonCancellable) { paths.forEach { runCatching { storage.delete(it) } } }
            throw exception
        }
    }

    suspend fun preparePdf(file: PlatformFile): GeoPdfMap = withContext(ioDispatcher) {
        requireNotNull(openPdf)
        val path = "maps/${Uuid.random()}.pdf"
        try {
            storage.copy(path, file)
            val bytes = requireNotNull(storage.open(path)).readAll(MAX_PDF_BYTES)
            GeoPdfMap(file.nameWithoutExtension, bytes).also { require(it.pageCount > 0) }
        } finally {
            withContext(NonCancellable) { storage.delete(path) }
        }
    }

    suspend fun importPdf(document: GeoPdfMap, pageIndex: Int, maps: MapService): PhotoMap = withContext(ioDispatcher) {
        require(pageIndex in 0 until document.pageCount)
        val preview = renderPdfPreview(document.bytes, pageIndex)
        val image = preview.decodeToImageBitmap()
        val size = Size(image.width.toFloat(), image.height.toFloat())
        val georeference = runCatching { document.georeference(pageIndex, size) }.getOrElse { PhotoMapGeoreference(size) }
        val path = "maps/${Uuid.random()}-page-${pageIndex + 1}.png"
        val candidate = PhotoMap(0, document.name, path, document.bytes.size.toLong(), georeference, createdOn = Clock.System.now())
        val map = if (candidate.isCalibrated) candidate else candidate.copy(georeference = PhotoMapGeoreference(size))
        try {
            storage.write(map.pdfFileName, document.bytes)
            storage.write(path, preview)
            withContext(NonCancellable) { map.copy(id = maps.add(map)) }
        } catch (exception: Exception) {
            withContext(NonCancellable) {
                runCatching { storage.delete(path) }
                runCatching { storage.delete(map.pdfFileName) }
            }
            throw exception
        }
    }

    suspend fun openVector(map: VectorMap): OfflineVectorMap? = withContext(ioDispatcher) {
        val blob = open(map.path) ?: return@withContext null
        when (map.type) {
            VectorMapFileType.GeoJson -> {
                val features = readGeoJson(blob)?.normalize() ?: return@withContext null
                OfflineVectorMap { _, _ -> features }
            }
            VectorMapFileType.Mapsforge -> {
                val reader = MapsforgeReader.open(blob)
                OfflineVectorMap(reader::read)
            }
        }
    }

    suspend fun openPhoto(map: PhotoMap): Pair<PhotoMap, ImageBitmap>? = withContext(ioDispatcher) {
        val blob = open(map.filename)
        val bytes = when {
            blob != null -> blob.readAll(MAX_IMAGE_BYTES)
            openPdf != null -> open(map.pdfFileName)?.readAll(MAX_PDF_BYTES)?.let {
                renderPdfPreview(it, map.pdfPageIndex)
            }
            else -> null
        } ?: return@withContext null
        val image = bytes.decodeToImageBitmap()
        val size = Size(image.width.toFloat(), image.height.toFloat())
        map.copy(georeference = map.georeference.copy(size = size, imageSize = size)) to image
    }

    suspend fun <T> withPdfPage(map: PhotoMap, use: suspend (OfflinePdfPage) -> T): T? {
        if (openPdf == null) return null
        val bytes = withContext(ioDispatcher) { open(map.pdfFileName)?.readAll(MAX_PDF_BYTES) } ?: return null
        return usePdf(bytes, map.pdfPageIndex, use)
    }

    private suspend fun renderPdfPreview(bytes: ByteArray, index: Int): ByteArray = usePdf(bytes, index) { page ->
        val scale = MAX_MAP_PDF_EDGE / maxOf(page.width, page.height)
        page.render(
            PdfRegion(),
            (page.width * scale).roundToInt().coerceAtLeast(1),
            (page.height * scale).roundToInt().coerceAtLeast(1)
        )
    }

    private suspend fun <T> usePdf(bytes: ByteArray, index: Int, use: suspend (OfflinePdfPage) -> T): T {
        // Opening transfers a document handle. Always take ownership before observing cancellation.
        val page = withContext(NonCancellable) { requireNotNull(openPdf)(bytes, index) }
        return try {
            use(page)
        } finally {
            withContext(NonCancellable) { page.close() }
        }
    }

    private suspend fun open(path: String) = if (MANAGED_PATH.matches(path)) storage.open(path) else null

    private suspend fun readGeoJson(blob: OfflineMapBlob): GeoJsonObject? =
        GeoJsonConvert.fromJson(blob.readAll(MAX_GEOJSON_BYTES).decodeToString())

    private fun geoJsonBounds(geoJson: GeoJsonObject): CoordinateBounds {
        val positions = geoJson.normalize().flatMap { feature ->
            when (val geometry = feature.geometry) {
                is GeoJsonPoint -> listOfNotNull(geometry.point)
                is GeoJsonLineString -> geometry.line.orEmpty()
                is GeoJsonPolygon -> geometry.polygon.orEmpty().flatten()
                else -> emptyList()
            }
        }
        require(positions.isNotEmpty() && positions.all { it.x in -180.0..180.0 && it.y in -90.0..90.0 })
        return requireNotNull(CoordinateBounds.from(positions.map { it.coordinate }))
    }

    // Only Android's repository can resolve persistent external content URIs.
    override fun canRead(path: String) = false

    override suspend fun delete(path: String) {
        if (MANAGED_PATH.matches(path)) storage.delete(path)
    }

    override fun releaseExternal(path: String) = Unit
    override suspend fun copyToAppStorage(map: VectorMap): VectorMap? = null
    override fun populatePhotoMap(map: PhotoMapEntity) = map.toMap()

    companion object {
        private val MANAGED_PATH = Regex("(?:offline_maps|maps)/[A-Za-z0-9_-][A-Za-z0-9_ .-]*")
        private const val MAX_GEOJSON_BYTES = 16 * 1024 * 1024
        private const val MAX_IMAGE_BYTES = 48 * 1024 * 1024
        private const val MAX_PDF_BYTES = 64 * 1024 * 1024
    }
}

internal fun interface OfflineVectorMap {
    suspend fun features(bounds: CoordinateBounds, zoom: Int): List<GeoJsonFeature>
}
