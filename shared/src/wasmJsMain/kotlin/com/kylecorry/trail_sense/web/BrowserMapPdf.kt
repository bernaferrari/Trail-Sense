package com.kylecorry.trail_sense.web

import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MAX_MAP_PDF_EDGE
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.await
import kotlinx.coroutines.withContext
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.Promise

@OptIn(ExperimentalWasmJsInterop::class)
internal suspend fun renderBrowserMapPdf(bytes: ByteArray): ByteArray {
    val data = Uint8Array(bytes.size)
    for (index in bytes.indices) data[index] = bytes[index]
    val task = startPdf(pdfOptions(data)).await()
    return try {
        val image = renderPdfPage(task.promise.await(), MAX_MAP_PDF_EDGE.toInt()).await()
        ByteArray(image.length) { image[it] }
    } finally {
        withContext(NonCancellable) { task.destroy().await() }
    }
}

private external interface PdfLoadingTask : JsAny {
    val promise: Promise<JsAny>
    fun destroy(): Promise<JsAny?>
}

@JsFun("(options) => import('pdfjs-dist/webpack.mjs').then(module => module.getDocument(options))")
private external fun startPdf(options: JsAny): Promise<PdfLoadingTask>

@JsFun("(data) => ({data, useWasm: false, isEvalSupported: false, stopAtErrors: true})")
private external fun pdfOptions(data: Uint8Array): JsAny

@JsFun("""
    async (pdf, maxEdge) => {
        const page = await pdf.getPage(1);
        const original = page.getViewport({scale: 1});
        const viewport = page.getViewport({scale: maxEdge / Math.max(original.width, original.height)});
        const canvas = document.createElement('canvas');
        canvas.width = Math.max(1, Math.round(viewport.width));
        canvas.height = Math.max(1, Math.round(viewport.height));
        try {
            await page.render({canvas, canvasContext: canvas.getContext('2d'), viewport}).promise;
            const blob = await new Promise((resolve, reject) => canvas.toBlob(
                blob => blob ? resolve(blob) : reject(new Error('Could not render PDF')), 'image/png'
            ));
            return new Uint8Array(await blob.arrayBuffer());
        } finally {
            canvas.width = canvas.height = 0;
            page.cleanup();
        }
    }
""")
private external fun renderPdfPage(pdf: JsAny, maxEdge: Int): Promise<Uint8Array>
