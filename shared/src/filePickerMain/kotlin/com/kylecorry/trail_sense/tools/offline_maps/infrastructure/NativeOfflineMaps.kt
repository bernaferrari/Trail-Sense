package com.kylecorry.trail_sense.tools.offline_maps.infrastructure

import com.kylecorry.andromeda.bitmaps.encodePng
import com.kylecorry.andromeda.files.FileStorage
import com.kylecorry.andromeda.pdf.PdfPages
import com.kylecorry.trail_sense.shared.coroutines.ioDispatcher
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.copyTo
import kotlinx.coroutines.withContext

internal fun diskOfflineMapStorage(files: FileStorage) =
    DiskOfflineMapStorage(files) { file, path -> file.copyTo(PlatformFile(path)) }

internal suspend fun openNativeMapPdf(bytes: ByteArray, pageIndex: Int): OfflinePdfPage =
    withContext(ioDispatcher) {
        val document = PdfPages(bytes)
        try {
            val size = document.pageSize(pageIndex)
            require(size.widthPoints > 0 && size.heightPoints > 0)
            object : OfflinePdfPage {
                override val width = size.widthPoints.toDouble()
                override val height = size.heightPoints.toDouble()

                override suspend fun render(region: PdfRegion, width: Int, height: Int) =
                    withContext(ioDispatcher) {
                        document.renderRegion(pageIndex, width, height, region.left, region.top, region.right, region.bottom)
                            .encodePng()
                    }

                override suspend fun close() = withContext(ioDispatcher) { document.close() }
            }
        } catch (failure: Throwable) {
            document.close()
            throw failure
        }
    }
