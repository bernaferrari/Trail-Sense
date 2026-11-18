package com.kylecorry.trail_sense.tools.offline_maps.infrastructure

import com.kylecorry.andromeda.bitmaps.encodePng
import com.kylecorry.andromeda.files.FileStorage
import com.kylecorry.andromeda.pdf.PdfPages
import com.kylecorry.trail_sense.shared.coroutines.ioDispatcher
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.copyTo
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

internal fun diskOfflineMapStorage(files: FileStorage) =
    DiskOfflineMapStorage(files) { file, path -> file.copyTo(PlatformFile(path)) }

internal suspend fun renderNativeMapPdf(bytes: ByteArray, pageIndex: Int): ByteArray = withContext(ioDispatcher) {
    PdfPages(bytes).use { pdf ->
        require(pageIndex in 0 until pdf.pageCount)
        val page = pdf.pageSize(pageIndex)
        require(page.widthPoints > 0 && page.heightPoints > 0)
        val scale = MAX_MAP_PDF_EDGE / maxOf(page.widthPoints, page.heightPoints)
        pdf.render(
            pageIndex,
            (page.widthPoints * scale).roundToInt().coerceAtLeast(1),
            (page.heightPoints * scale).roundToInt().coerceAtLeast(1)
        ).encodePng()
    }
}
