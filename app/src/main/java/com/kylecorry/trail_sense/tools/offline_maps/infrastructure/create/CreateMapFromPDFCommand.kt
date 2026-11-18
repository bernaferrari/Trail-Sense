package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.create

import com.kylecorry.trail_sense.tools.offline_maps.domain.CreateMapResult
import com.kylecorry.trail_sense.tools.offline_maps.domain.CreateOfflineMapError
import kotlin.time.Clock

import android.content.Context
import android.net.Uri
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.pdf.GeospatialPDFParser
import com.kylecorry.andromeda.pdf.PDFRenderer
import com.kylecorry.andromeda.pdf.PDFRenderer2
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.MapCalibrationPoint
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMapGeoreference
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.MapProjectionType
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PercentCoordinate
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.GeoPdfMap
import okio.buffer
import okio.source
import java.io.IOException
import java.util.UUID

class CreateMapFromPDFCommand(
    private val context: Context,
    private val name: String,
    private val shouldCopyAsPdf: Boolean = true
) {

    private val files = FileSubsystem.getInstance(context)
    private val service = getAppService<MapService>()

    suspend fun execute(uri: Uri, pageIndex: Int, allowLegacyMetadata: Boolean): CreateMapResult = onIO {
        val uuid = "${UUID.randomUUID()}-page-${pageIndex + 1}"
        val filename = "maps/$uuid.webp"
        val calibrationPoints = mutableListOf<MapCalibrationPoint>()

        val maxSize = 2048
        val (bp, scale) = PDFRenderer().toBitmap(context, uri, page = pageIndex, maxSize = maxSize)
            ?: return@onIO CreateMapResult.Failed(CreateOfflineMapError.InvalidMapFile)

        val previewSize = Size(bp.width.toFloat(), bp.height.toFloat())
        val shared = tryOrDefault(null) {
            files.stream(uri)?.source()?.buffer()?.use { source ->
                if (source.request(64L * 1024 * 1024 + 1)) null else {
                    GeoPdfMap(name, source.readByteArray()).georeference(pageIndex, previewSize)
                }
            }
        }
        // The legacy parser cannot associate a viewport with a particular page.
        val metadata = if (shared?.calibrationPoints.isNullOrEmpty() && allowLegacyMetadata) {
            tryOrDefault(null) { files.stream(uri)?.use { GeospatialPDFParser().parse(it) } }
        } else null

        if (metadata != null && metadata.points.size >= 4) {

            val first = metadata.points[0]
            val second = metadata.points.maxBy { it.first.distanceTo(first.first) }

            val points = listOf(first, second).map {
                MapCalibrationPoint(
                    Coordinate(it.second.latitude, it.second.longitude),
                    PercentCoordinate(scale * it.first.x / bp.width, scale * it.first.y / bp.height)
                )
            }
            calibrationPoints.addAll(points)
        }

        val projectionName = metadata?.projection?.projection
        val projection = if (projectionName == null || projectionName.contains("mercator", true)) MapProjectionType.Mercator
        else MapProjectionType.CylindricalEquidistant

        try {
            files.save(filename, bp, recycleOnSave = true)
        } catch (e: IOException) {
            return@onIO CreateMapResult.Failed(CreateOfflineMapError.UnableToCopy)
        }

        if (shouldCopyAsPdf) {
            tryOrNothing {
                files.copyToLocal(uri, "maps", "$uuid.pdf")
            }
        }

        val imageSize = files.imageSize(filename)
        val fileSize = files.size(filename)

        val pdfSize = if (shouldCopyAsPdf) {
            tryOrDefault(null) {
                PDFRenderer2(context, uri).use {
                    it.getSize(pageIndex)
                }
            }
        } else {
            null
        }

        val map = PhotoMap(
            0,
            name,
            filename,
            fileSize,
            PhotoMapGeoreference(
                Size(imageSize.width.toFloat(), imageSize.height.toFloat()),
                pdfSize?.let { Size(it.width.toFloat(), it.height.toFloat()) },
                projectionType = shared?.takeIf { it.calibrationPoints.isNotEmpty() }?.projectionType ?: projection,
                isWarpingCompleted = !shared?.calibrationPoints.isNullOrEmpty() || calibrationPoints.isNotEmpty(),
                calibrationPoints = shared?.calibrationPoints?.takeIf { it.isNotEmpty() } ?: calibrationPoints
            ),
            createdOn = Clock.System.now()
        )

        val id = service.add(map)
        CreateMapResult.Created(map.copy(id = id))
    }

}
