package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps

import android.content.Context
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.shared.io.ExportService
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.io.UriPicker
import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.VectorMap

/** Copies a vector (Mapsforge) map's file to a location the user picks. */
class VectorMapExportService(
    context: Context,
    private val uriPicker: UriPicker
) : ExportService<VectorMap> {

    private val files = FileSubsystem.getInstance(context)

    override suspend fun export(data: VectorMap, filename: String): Boolean {
        val uri = uriPicker.create(filename, MIME_TYPE) ?: return false
        val input = files.fileInputStream(data.path) ?: return false
        val output = files.output(uri) ?: run {
            input.close()
            return false
        }
        return onIO {
            input.use { source -> output.use { source.copyTo(it) } }
            true
        }
    }

    private companion object {
        const val MIME_TYPE = "application/octet-stream"
    }
}
