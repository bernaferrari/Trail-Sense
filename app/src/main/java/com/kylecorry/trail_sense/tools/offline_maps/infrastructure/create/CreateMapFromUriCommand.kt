package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.create

import com.kylecorry.trail_sense.tools.offline_maps.domain.CreateMapResult
import com.kylecorry.trail_sense.tools.offline_maps.domain.CreateOfflineMapError
import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.kylecorry.trail_sense.shared.alerts.ILoadingIndicator
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.shared.dialogs.CoroutinePickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.offline_maps.domain.IMap
import com.kylecorry.trail_sense.shared.dialogs.text
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.views.compose.from
import com.kylecorry.trail_sense.tools.offline_maps.ui.chooseOfflinePdfPage

class CreateMapFromUriCommand(
    private val context: Context,
    private val uri: Uri,
    private val loadingIndicator: ILoadingIndicator
) : ICreateMapCommand {

    private val files = FileSubsystem.getInstance(context)

    override suspend fun execute(): CreateMapResult = onIO {
        val filename = files.getFileName(uri, withExtension = false, fallbackToPathName = false)
        val name = onMain {
            CoroutinePickers.text(
                context,
                context.getString(R.string.name),
                hint = context.getString(R.string.name),
                default = filename
            )
        } ?: return@onIO CreateMapResult.cancelled

        val type = files.getMimeType(uri) ?: getMimeTypeFromExtension(uri)
        val pdfPage = if (type == MIME_TYPE_PDF) {
            val pages = runCatching {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                    PdfRenderer(descriptor).use { it.pageCount }
                } ?: 0
            }.getOrDefault(0)
            if (pages == 0) return@onIO CreateMapResult.Failed(CreateOfflineMapError.InvalidMapFile)
            val page = onMain { chooseOfflinePdfPage(AppHost.from(context), pages) }
                ?: return@onIO CreateMapResult.cancelled
            page to pages
        } else null

        try {
            onMain {
                loadingIndicator.show()
            }
            if (pdfPage != null) {
                CreateMapFromPDFCommand(context, name).execute(uri, pdfPage.first, allowLegacyMetadata = pdfPage.second == 1)
            } else if (type?.startsWith(MIME_TYPE_IMAGE_PREFIX) == true) {
                CreateMapFromImageCommand(context, name).execute(uri)
            } else {
                CreateVectorMapFromFileCommand(name).execute(uri)
            }
        } finally {
            onMain {
                loadingIndicator.hide()
            }
        }
    }

    private fun getMimeTypeFromExtension(uri: Uri): String? {
        val filename = files.getFileName(uri, withExtension = true, fallbackToPathName = true)
            ?: uri.lastPathSegment
            ?: return null

        return when (filename.substringAfterLast('.', "").lowercase()) {
            "pdf" -> MIME_TYPE_PDF
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> null
        }
    }

    private companion object {
        private const val MIME_TYPE_PDF = "application/pdf"
        private const val MIME_TYPE_IMAGE_PREFIX = "image/"
    }
}
