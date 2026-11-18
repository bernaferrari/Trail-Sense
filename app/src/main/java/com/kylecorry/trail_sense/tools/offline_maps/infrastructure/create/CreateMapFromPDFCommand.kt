package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.create

import android.content.Context
import android.net.Uri
import android.util.Log
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.pdf.GeospatialPDFParser
import com.kylecorry.andromeda.pdf.PDFRenderer
import com.kylecorry.andromeda.pdf.PDFRenderer2
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.offline_maps.domain.CreateMapResult
import com.kylecorry.trail_sense.tools.offline_maps.domain.CreateOfflineMapError
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.MapCalibrationPoint
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.MapProjectionType
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PercentCoordinate
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMapGeoreference
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.GeoPdfMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService
import java.io.IOException
import java.util.UUID
import kotlin.time.Clock
import okio.buffer
import okio.source

class CreateMapFromPDFCommand(private val context: Context, private val name: String) {

    private val files = FileSubsystem.getInstance(context)
    private val service = getAppService<MapService>()

    suspend fun execute(uri: Uri, pageIndex: Int, allowLegacyMetadata: Boolean): CreateMapResult = onIO {
        val uuid = "${UUID.randomUUID()}-page-${pageIndex + 1}"
        val filename = "maps/$uuid.webp"
        val (bitmap, scale) = PDFRenderer().toBitmap(context, uri, page = pageIndex, maxSize = PREVIEW_EDGE)
            ?: return@onIO CreateMapResult.Failed(CreateOfflineMapError.InvalidMapFile)
        val previewSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat())
        val georeference = readGeoreference(uri, pageIndex, previewSize, scale, allowLegacyMetadata)

        try {
            files.save(filename, bitmap, recycleOnSave = true)
        } catch (exception: IOException) {
            Log.w(TAG, "Could not save the PDF map preview", exception)
            return@onIO CreateMapResult.Failed(CreateOfflineMapError.UnableToCopy)
        }

        val pdfSize = tryOrDefault(null) {
            files.copyToLocal(uri, "maps", "$uuid.pdf")?.let {
                PDFRenderer2(context, uri).use { pdf ->
                    val size = pdf.getSize(pageIndex)
                    Size(size.width.toFloat(), size.height.toFloat())
                }
            }
        }
        val map = PhotoMap(
            0, name, filename, files.size(filename),
            georeference.copy(unscaledPdfSize = pdfSize),
            createdOn = Clock.System.now()
        )
        CreateMapResult.Created(map.copy(id = service.add(map)))
    }

    private suspend fun readGeoreference(
        uri: Uri, pageIndex: Int, size: Size, scale: Float, allowLegacyMetadata: Boolean
    ): PhotoMapGeoreference {
        val shared = tryOrDefault(null) {
            files.stream(uri)?.source()?.buffer()?.use { source ->
                if (source.request(MAX_METADATA_FILE_BYTES + 1)) null else {
                    GeoPdfMap(name, source.readByteArray()).georeference(pageIndex, size)
                }
            }
        }
        if (shared?.calibrationPoints?.isNotEmpty() == true) return shared
        // The legacy parser cannot associate a viewport with a particular page.
        val legacy = if (allowLegacyMetadata) {
            tryOrDefault(null) { files.stream(uri)?.use { GeospatialPDFParser().parse(it) } }
        } else null
        val points = legacy?.points?.takeIf { it.size >= 4 }?.let { landmarks ->
            val first = landmarks.first()
            val second = landmarks.maxBy { it.first.distanceTo(first.first) }
            listOf(first, second).map { (pixel, coordinate) ->
                MapCalibrationPoint(coordinate, PercentCoordinate(scale * pixel.x / size.width, scale * pixel.y / size.height))
            }
        }.orEmpty()
        val projectionName = legacy?.projection?.projection
        val projection = if (projectionName == null || projectionName.contains("mercator", true)) {
            MapProjectionType.Mercator
        } else {
            MapProjectionType.CylindricalEquidistant
        }
        return PhotoMapGeoreference(
            size, projectionType = projection, isWarpingCompleted = points.isNotEmpty(), calibrationPoints = points
        )
    }

    private companion object {
        const val TAG = "CreateMapFromPDF"
        const val PREVIEW_EDGE = 2048
        const val MAX_METADATA_FILE_BYTES = 64L * 1024 * 1024
    }
}
