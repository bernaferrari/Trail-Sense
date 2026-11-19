package com.kylecorry.trail_sense.web

import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.OfflinePdfPage
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.PdfRegion
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.await
import kotlinx.coroutines.withContext
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.Promise

@OptIn(ExperimentalWasmJsInterop::class)
internal suspend fun openBrowserMapPdf(bytes: ByteArray, pageIndex: Int): OfflinePdfPage {
    val data = Uint8Array(bytes.size)
    for (index in bytes.indices) data[index] = bytes[index]
    val task = withContext(NonCancellable) { startPdf(data).await() }
    try {
        val page = getPdfPage(task.promise.await(), pageIndex + 1).await()
        val viewport = viewportFor(page)
        return object : OfflinePdfPage {
            override val width = viewport.width
            override val height = viewport.height
            override suspend fun render(region: PdfRegion, width: Int, height: Int): ByteArray {
                val image = withContext(NonCancellable) {
                    renderPdfRegion(page, viewport, region.left, region.top, region.right, region.bottom, width, height).await()
                }
                return ByteArray(image.length) { image[it] }
            }
            override suspend fun close() { task.destroy().await() }
        }
    } catch (failure: Throwable) {
        withContext(NonCancellable) { task.destroy().await() }
        throw failure
    }
}

private external interface PdfLoadingTask : JsAny {
    val promise: Promise<JsAny>
    fun destroy(): Promise<JsAny?>
}

private external interface PdfViewport : JsAny {
    val width: Double
    val height: Double
}

@JsFun("""
    data => import('pdfjs-dist/webpack.mjs').then(module => module.getDocument({
        data, useWasm: false, isEvalSupported: false, stopAtErrors: true
    }))
""")
private external fun startPdf(data: Uint8Array): Promise<PdfLoadingTask>

@JsFun("(pdf, page) => pdf.getPage(page)")
private external fun getPdfPage(pdf: JsAny, page: Int): Promise<JsAny>

@JsFun("page => page.getViewport({scale: 1})")
private external fun viewportFor(page: JsAny): PdfViewport

@JsFun("""
    async (page, viewport, left, top, right, bottom, width, height) => {
        const pageWidth = width / (right - left);
        const pageHeight = height / (bottom - top);
        const transform = [pageWidth / viewport.width, 0, 0, pageHeight / viewport.height,
            -left * pageWidth, -top * pageHeight];
        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;
        try {
            await page.render({canvas, canvasContext: canvas.getContext('2d'), viewport, transform}).promise;
            const blob = await new Promise((resolve, reject) => canvas.toBlob(
                blob => blob ? resolve(blob) : reject(new Error('Could not render PDF')), 'image/png'
            ));
            return new Uint8Array(await blob.arrayBuffer());
        } finally {
            canvas.width = canvas.height = 0;
        }
    }
""")
private external fun renderPdfRegion(
    page: JsAny, viewport: PdfViewport, left: Double, top: Double, right: Double, bottom: Double,
    width: Int, height: Int
): Promise<Uint8Array>
