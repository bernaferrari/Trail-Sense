package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.create

import com.kylecorry.trail_sense.tools.offline_maps.domain.CreateMapResult
import com.kylecorry.trail_sense.tools.offline_maps.domain.CreateOfflineMapError
import kotlin.time.Clock

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMapGeoreference
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService

class CreateMapFromImageCommand(
    context: Context,
    private val name: String
) {
    private val files = FileSubsystem.getInstance(context)
    private val service = getAppService<MapService>()

    suspend fun execute(uri: Uri): CreateMapResult = onIO {
        val file = files.copyToLocal(uri, "maps")
            ?: return@onIO CreateMapResult.Failed(CreateOfflineMapError.UnableToCopy)
        var rotation = 0
        tryOrLog {
            val exif = ExifInterface(file)
            rotation = exif.rotationDegrees
        }

        val path = files.getLocalPath(file)
        val imageSize = files.imageSize(path)
        val fileSize = files.size(path)

        val map = PhotoMap(
            0,
            name,
            path,
            fileSize,
            PhotoMapGeoreference(
                Size(imageSize.width.toFloat(), imageSize.height.toFloat()),
                rotation = rotation.toFloat()
            ),
            createdOn = Clock.System.now()
        )

        val id = service.add(map)
        CreateMapResult.Created(map.copy(id = id))
    }

}
