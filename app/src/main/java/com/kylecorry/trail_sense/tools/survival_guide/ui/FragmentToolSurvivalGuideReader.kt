package com.kylecorry.trail_sense.tools.survival_guide.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.tools.survival_guide.domain.SurvivalGuideRepo

@Composable
fun Fragment.SurvivalGuideReaderContent(modifier: Modifier = Modifier) {
    val context = requireContext()
    val repo = remember { SurvivalGuideRepo.getInstance(context) }
    val chapterId = remember { arguments?.getLong("chapter_id") ?: 0L }

    var content by remember { mutableStateOf("Loading...") }
    var title by remember { mutableStateOf("") }

    LaunchedEffect(chapterId) {
        val chapter = repo.getChapter(chapterId)
        title = chapter?.name ?: "Survival Guide"
        content = chapter?.content ?: "Content not found."
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        TrailSenseCard {
            Column(Modifier.padding(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(
                    content,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

class FragmentToolSurvivalGuideReader : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        SurvivalGuideReaderContent()
    }
}

        // Print button
        useEffect(printChapter, titleView) {
            titleView.rightButton.isVisible = isDebug()
            titleView.rightButton.setOnClickListener {
                printChapter(chapter)
            }
        }

        // Render the content
        useEffect(scrollView, content, headerIndex) {
            if (content == null) {
                return@useEffect
            }
            scrollView.removeAllViews()
            scrollView.addView(
                TextUtils.getMarkdownView(
                    context,
                    content,
                    shouldUppercaseSubheadings = true
                )
            )

            if (headerIndex > 0) {
                // Open the ExpansionLayout at the headerIndex - 1 and scroll to it

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
    }

    private fun useChapter(): Chapter {
        val chapterResourceId = useArgument<Int>("chapter_resource_id")!!
        val context = useAndroidContext()
        return useMemo(context, chapterResourceId) {
            Chapters.getChapters(context).first { it.resource == chapterResourceId }
        }
    }

    private fun useChapterContent(): String? {
        val (content, setContent) = useState<String?>(null)
        val chapterResourceId = useArgument<Int>("chapter_resource_id")!!
        val context = useAndroidContext()
        useBackgroundEffect(context, chapterResourceId) {
            setContent(TextUtils.loadTextFromResources(context, chapterResourceId))
        }
        return content
    }

    private fun usePrintChapter(): (Chapter) -> Unit {
        val context = useAndroidContext()
        val files = useService<FileSubsystem>()
        val markdown = useService<MarkdownService>()
        return useCallback(context, files, markdown) { chapter: Chapter ->
            inBackground {
                onIO {
                    val loading =
                        onMain { Alerts.loading(context, getString(R.string.loading)) }
                    try {
                        // TODO: Extract this to a shared markdown printer
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
                        // TODO: Indented bullet points do not work properly when on a separate page
                        // TODO: Bullet point continuation does not render properly on a separate page
                        // TODO: Images are overlapping text
                        val parsed = markdown.toMarkdown(content)
                        val textView = Views.text(context, null) as TextView
                        textView.textSize = 6f
                        textView.setTextColor(Color.BLACK)
                        markdown.setParsedMarkdown(textView, parsed)
                        // Wait for images to load
                        // TODO: Find a better way to do this
                        delay(2000)
                        val uri = file.toUri()
                        files.output(uri)?.use {
                            PdfConvert.toPdf(textView, it)
                        }
                        onMain { loading.dismiss() }
                        printer.print(uri)
                    } finally {
                        DeleteTempFilesCommand(requireContext()).execute()
                        onMain { loading.dismiss() }
                    }
                }
            }
        }
    }
}
