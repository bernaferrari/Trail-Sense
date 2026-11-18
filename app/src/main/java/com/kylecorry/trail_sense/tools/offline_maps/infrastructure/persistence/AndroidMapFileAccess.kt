package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.persistence

import android.content.Context
import android.util.Size
import androidx.core.net.toUri
import com.kylecorry.andromeda.core.math.MathUtils
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMapEntity
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.VectorMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.MapFileTypeUtils
import java.util.UUID

class AndroidMapFileAccess(context: Context) : MapFileAccess {
    private val files = FileSubsystem.getInstance(context)

    override suspend fun delete(path: String) = files.delete(path)
    override fun canRead(path: String): Boolean = files.canRead(path)
    override fun releaseExternal(path: String) = files.releasePersistentAccess(path.toUri())

    override suspend fun copyToAppStorage(map: VectorMap): VectorMap? {
        val extension = MapFileTypeUtils.getExtension(map.type)
        val saved = files.copyToLocal(map.path.toUri(), "offline_maps", "${UUID.randomUUID()}.$extension")
            ?: return null
        return map.copy(path = files.getLocalPath(saved), sizeBytes = saved.length())
    }

    override fun populatePhotoMap(map: PhotoMapEntity): PhotoMap {
        val newMap = map.toMap()
        // TODO: Save the size in the DB
        val size = files.imageSize(newMap.filename)
        val fileSize = files.size(newMap.filename) + files.size(newMap.pdfFileName)

        val pdfWidth = map.pdfWidth
        val pdfHeight = map.pdfHeight
        val pdfSize =
            if (pdfHeight != null && pdfWidth != null && files.get(newMap.pdfFileName)
                    .exists()
            ) {

                val scaledSize = MathUtils.scaleToBounds(
                    Size(pdfWidth, pdfHeight),
                    Size(PhotoMap.DESIRED_PDF_SIZE, PhotoMap.DESIRED_PDF_SIZE)
                )

                com.kylecorry.sol.math.geometry.Size(
                    scaledSize.width.toFloat(),
                    scaledSize.height.toFloat()
                )
            } else {
                null
            }

        return newMap.copy(
            fileSizeBytes = fileSize,
            georeference = newMap.georeference.copy(
                size = pdfSize ?: com.kylecorry.sol.math.geometry.Size(
                    size.width.toFloat(),
                    size.height.toFloat()
                ),
                imageSize = com.kylecorry.sol.math.geometry.Size(
                    size.width.toFloat(),
                    size.height.toFloat()
                )
            )
        )
    }

}
