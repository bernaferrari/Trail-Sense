package com.kylecorry.trail_sense.tools.survival_guide.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.core.view.children
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.ExpansionLayout
import com.kylecorry.andromeda.core.ui.Views
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.pdf.PdfConvert
import com.kylecorry.andromeda.print.ColorMode
import com.kylecorry.andromeda.print.Orientation
import com.kylecorry.andromeda.print.Printer
import com.kylecorry.andromeda.print.ScaleMode
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.debugging.isDebug
import com.kylecorry.trail_sense.shared.io.DeleteTempFilesCommand
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.text.TextUtils
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapter
import com.kylecorry.trail_sense.tools.survival_guide.domain.Chapters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentToolSurvivalGuideReader : Fragment() {

    private val chapterResourceId by lazy { requireArguments().getInt("chapter_resource_id") }
    private val headerIndex by lazy { requireArguments().getInt("header_index", 0) }
    private val chapter by lazy {
        Chapters.getChapters(requireContext()).first { it.resource == chapterResourceId }
    }
    private val files by lazy { FileSubsystem.getInstance(requireContext()) }
    private val markdown by lazy { AppServiceRegistry.get<MarkdownService>() }

    private var content by mutableStateOf<String?>(null)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    SurvivalGuideReaderScreen(
                        chapter = chapter,
                        content = content,
                        headerIndex = headerIndex,
                        canPrint = isDebug(),
                        onPrint = ::printChapter
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            content = withContext(Dispatchers.IO) {
                TextUtils.loadTextFromResources(requireContext(), chapterResourceId)
            }
        }
    }

    private fun printChapter(chapter: Chapter) {
        val context = requireContext()
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val loading = withContext(Dispatchers.Main) {
                Alerts.loading(context, getString(R.string.loading))
            }
            try {
                val printer = Printer(context)
                printer.setColorMode(ColorMode.Color)
                printer.setScaleMode(ScaleMode.Fit)
                printer.setOrientation(Orientation.Portrait)
                val content = TextUtils.removeMarkdownComments(
                    "# ${chapter.title}\n" + TextUtils.loadTextFromResources(
                        context,
                        chapter.resource
                    )
                )
                val file = files.createTemp("pdf")
                val parsed = markdown.toMarkdown(content)
                val textView = Views.text(context, null) as TextView
                textView.textSize = 6f
                textView.setTextColor(Color.BLACK)
                markdown.setParsedMarkdown(textView, parsed)
                delay(2000)
                val uri = file.toUri()
                files.output(uri)?.use {
                    PdfConvert.toPdf(textView, it)
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
}

@Composable
private fun SurvivalGuideReaderScreen(
    chapter: Chapter,
    content: String?,
    headerIndex: Int,
    canPrint: Boolean,
    onPrint: (Chapter) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SurvivalGuideHeader(
                title = chapter.title,
                canPrint = canPrint,
                onPrint = { onPrint(chapter) }
            )
            MarkdownChapterView(
                content = content,
                headerIndex = headerIndex,
                modifier = Modifier
                    .weight(1f)
                    .testTag("guide_scroll")
            )
        }
    }
}

@Composable
private fun SurvivalGuideHeader(
    title: String,
    canPrint: Boolean,
    onPrint: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .testTag("guide_name"),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (canPrint) {
                IconButton(onClick = onPrint) {
                    Icon(
                        painter = painterResource(R.drawable.ic_printer),
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkdownChapterView(
    content: String?,
    headerIndex: Int,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            NestedScrollView(context).apply {
                clipToPadding = false
                setPadding(0, 0, 0, Resources.dp(context, 16f).toInt())
            }
        },
        update = { scrollView ->
            scrollView.removeAllViews()
            val chapterContent = content
            if (chapterContent == null) {
                val loading = TextView(scrollView.context).apply {
                    val padding = Resources.dp(context, 16f).toInt()
                    setPadding(padding, padding, padding, padding)
                    text = context.getString(R.string.loading)
                }
                scrollView.addView(loading)
                return@AndroidView
            }

            scrollView.addView(
                TextUtils.getMarkdownView(
                    scrollView.context,
                    chapterContent,
                    shouldUppercaseSubheadings = true
                )
            )

            if (headerIndex > 0) {
                val layout = scrollView.getChildAt(0) as? ViewGroup
                val header = layout?.children
                    ?.filterIsInstance<ExpansionLayout>()
                    ?.elementAtOrNull(headerIndex - 1)

                header?.expand()

                scrollView.post {
                    scrollView.scrollTo(0, header?.top ?: 0)
                }
            }
        }
    )
}
