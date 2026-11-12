@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.survival_guide.ui

import androidx.compose.runtime.getValue
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.print.ColorMode
import com.kylecorry.andromeda.print.Orientation
import com.kylecorry.andromeda.print.Printer
import com.kylecorry.andromeda.print.ScaleMode
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.debugging.isDebug
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.dialogs.loading
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.io.DeleteTempFilesCommand
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.text.RawTextReader
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.shared.text.markdown.MarkdownService
import com.kylecorry.trail_sense.shared.text.toSpanned
import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapter
import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A survival guide chapter for [Route.SurvivalGuideReader], scrolled to its heading; debug builds can print it. */
@Composable
fun SurvivalGuideReaderToolScreen(route: Route.SurvivalGuideReader) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val chapterResourceId = route.chapter
    val chapter = remember(chapterResourceId) {
        Chapters.getChapters().firstOrNull { it.resource == chapterResourceId }
    }
    if (chapter == null) {
        // A chapter that no longer exists (an old shortcut or a stale saved screen): go back instead of crashing.
        val navigator = LocalNavigator.current
        LaunchedEffect(Unit) { navigator.popBackStack() }
        return
    }
    val content by produceState<String?>(null, chapterResourceId) {
        value = withContext(Dispatchers.IO) {
            getAppService<RawTextReader>().read(chapterResourceId)
        }
    }
    SurvivalGuideReaderScreen(
        chapter = chapter,
        content = content,
        headerIndex = route.headerIndex,
        canPrint = isDebug(),
        onPrint = { printChapter(context, scope, it) }
    )
}

/** Prints [chapter] as a PDF (debug builds only); [context] is the screen's, for the loading dialog and printer. */
private fun printChapter(context: Context, scope: CoroutineScope, chapter: Chapter) {
    val files = FileSubsystem.getInstance(context)
    val markdown = DependencyRegistry.get<MarkdownService>()
    scope.launch(Dispatchers.IO) {
        val loading = withContext(Dispatchers.Main) {
            Alerts.loading(context, context.getString(R.string.loading))
        }
        try {
            val printer = Printer(context)
            printer.setColorMode(ColorMode.Color)
            printer.setScaleMode(ScaleMode.Fit)
            printer.setOrientation(Orientation.Portrait)
            val content = TextUtils.removeMarkdownComments(
                "# ${chapter.title}\n" + getAppService<RawTextReader>().read(chapter.resource)
            )
            val file = files.createTemp("pdf")
            val parsed = markdown.toMarkdown(content)
            val uri = file.toUri()
            files.output(uri)?.use {
                writePdf(parsed.toSpanned(PDF_TEXT_SIZE), it)
            }
            withContext(Dispatchers.Main) {
                printer.print(uri)
            }
        } finally {
            DeleteTempFilesCommand(context).execute()
            withContext(Dispatchers.Main) {
                loading.dismiss()
            }
        }
    }
}

/** Lays [text] out in small black type on A4 pages, the way the printed chapter looked. */
private const val PDF_TEXT_SIZE = 8f

private fun writePdf(text: CharSequence, output: java.io.OutputStream) {
    val pageWidth = 595
    val pageHeight = 842
    val margin = 36
    val paint = android.text.TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textSize = PDF_TEXT_SIZE
        color = android.graphics.Color.BLACK
    }
    val layout = android.text.StaticLayout.Builder
        .obtain(text, 0, text.length, paint, pageWidth - margin * 2)
        .build()
    val document = android.graphics.pdf.PdfDocument()
    val contentHeight = pageHeight - margin * 2
    var line = 0
    var pageNumber = 1
    while (line < layout.lineCount) {
        val top = layout.getLineTop(line)
        var last = line
        while (last + 1 < layout.lineCount && layout.getLineBottom(last + 1) - top <= contentHeight) {
            last++
        }
        val bottom = layout.getLineBottom(last)
        val page = document.startPage(
            android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
        )
        page.canvas.apply {
            translate(margin.toFloat(), (margin - top).toFloat())
            clipRect(0, top, pageWidth - margin * 2, bottom)
            layout.draw(this)
        }
        document.finishPage(page)
        line = last + 1
    }
    document.writeTo(output)
    document.close()
}
