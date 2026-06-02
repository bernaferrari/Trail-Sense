package com.kylecorry.trail_sense.tools.survival_guide.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.core.ui.setCompoundDrawables
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.views.badge.Badge
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.navigateWithAnimation
import com.kylecorry.trail_sense.shared.views.SearchView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.SurvivalGuideSearch
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.SurvivalGuideSearchResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SurvivalGuideBottomSheetFragment : BottomSheetDialogFragment() {

    private var guideViews: SurvivalGuideSheetViews? = null
    private val binding: SurvivalGuideSheetViews
        get() = guideViews ?: error("Survival guide views are not ready")

    private var query = ""
    private var searchResults = emptyList<SurvivalGuideSearchResult>()
    private var summary = ""
    private var searchJob: Job? = null

    private val markdown by lazy { AppServiceRegistry.get<MarkdownService>() }
    private val search by lazy { SurvivalGuideSearch(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val views = createSurvivalGuideViews(requireContext())
        guideViews = views
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    SurvivalGuideHost(
                        views = views,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.title.rightButton.setOnClickListener {
            openSearchList()
        }
        binding.search.setOnSearchListener(::updateQuery)
        updateSummary()
    }

    override fun onDestroyView() {
        searchJob?.cancel()
        guideViews?.search?.setOnSearchListener(null)
        guideViews = null
        super.onDestroyView()
    }

    private fun updateQuery(value: String) {
        if (query == value) {
            return
        }
        query = value
        search(value)
    }

    private fun search(value: String) {
        searchJob?.cancel()
        if (value.isBlank()) {
            searchResults = emptyList()
            summary = ""
            updateSummary()
            return
        }

        searchJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(200)
            val (results, newSummary) = onDefault {
                val results = search.search(value)
                val firstResult = results.firstOrNull()
                val summary = if (firstResult != null && firstResult.score >= 0.5f) {
                    search.getSummary(value, firstResult)
                } else {
                    ""
                }
                results to summary
            }

            if (query != value) {
                return@launch
            }

            searchResults = results
            summary = newSummary
            updateSummary()
        }
    }

    private fun updateSummary() {
        val views = guideViews ?: return
        val result = searchResults.firstOrNull()
        views.summaryHolder.isVisible = summary.isNotBlank()
        views.emptyDescription.isVisible = summary.isBlank()
        markdown.setMarkdown(views.summary, summary)

        if (result == null) {
            views.summaryHolder.setOnClickListener(null)
            views.summaryTitle.text = ""
            return
        }

        views.summaryTitle.text = listOfNotNull(
            result.heading,
            if (SurvivalGuideSearch.shouldUseSubsection(result)) {
                result.bestSubsection?.heading
            } else {
                null
            }
        ).joinToString(" > ")

        views.summaryChapterTitle.setStatusText(result.chapter.title)
        ResourceListIcon(
            result.chapter.icon,
            size = 12f
        ).apply(views.summaryChapterTitle.statusImage)
        val backgroundColor = Resources.androidTextColorSecondary(requireContext())
        views.summaryChapterTitle.setBackgroundTint(backgroundColor)
        views.summaryChapterTitle.setForegroundTint(
            Colors.mostContrastingColor(
                Color.WHITE,
                Color.BLACK,
                backgroundColor
            )
        )
        views.summaryTitle.setCompoundDrawables(
            size = Resources.dp(requireContext(), 16f).toInt(),
            right = R.drawable.ic_keyboard_arrow_right
        )
        views.summaryHolder.setOnClickListener {
            openSummary(result)
        }
        views.summaryScroll.post {
            views.summaryScroll.scrollTo(0, 0)
        }
    }

    private fun openSearchList() {
        dismiss()
        findNavController().navigateWithAnimation(
            R.id.fragmentToolSurvivalGuideList,
            Bundle().apply {
                putString("search_query", query)
            }
        )
    }

    private fun openSummary(result: SurvivalGuideSearchResult) {
        dismiss()
        findNavController().navigateWithAnimation(
            R.id.fragmentToolSurvivalGuideReader,
            Bundle().apply {
                putInt("chapter_resource_id", result.chapter.resource)
                putInt("header_index", result.headingIndex)
            }
        )
    }
}

private data class SurvivalGuideSheetViews(
    val root: View,
    val title: Toolbar,
    val search: SearchView,
    val emptyDescription: TextView,
    val summaryHolder: MaterialCardView,
    val summaryChapterTitle: Badge,
    val summaryTitle: TextView,
    val summaryScroll: NestedScrollView,
    val summary: TextView
)

@Composable
private fun SurvivalGuideHost(
    views: SurvivalGuideSheetViews,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { views.root }
    )
}

private fun createSurvivalGuideViews(context: Context): SurvivalGuideSheetViews {
    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    val title = Toolbar(context, null).apply {
        id = R.id.title
        this.title.text = context.getString(R.string.survival_guide)
        subtitle.isVisible = false
        rightButton.setImageResource(R.drawable.survival_guide)
        rightButton.isVisible = true
    }
    root.addView(
        title,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )

    val search = SearchView(context, null).apply {
        id = R.id.search
        setPadding(0, 0, 0, Resources.dp(context, 4f).toInt())
    }
    root.addView(
        search,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = Resources.dp(context, 8f).toInt()
            rightMargin = Resources.dp(context, 8f).toInt()
        }
    )

    val emptyDescription = TextView(context).apply {
        id = R.id.empty_view_description
        gravity = Gravity.CENTER
        text = context.getString(R.string.survival_guide_empty_desc)
        textSize = 12f
    }
    root.addView(
        emptyDescription,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = Resources.dp(context, 8f).toInt()
        }
    )

    val summaryChapterTitle = Badge(context, null).apply {
        id = R.id.summary_chapter_title
    }
    val summaryTitle = TextView(context).apply {
        id = R.id.summary_title
        compoundDrawablePadding = Resources.dp(context, 8f).toInt()
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, Resources.dp(context, 4f).toInt(), 0, Resources.dp(context, 8f).toInt())
        textSize = 16f
        typeface = Typeface.DEFAULT_BOLD
    }
    val summaryScroll = NestedScrollView(context).apply {
        id = R.id.summary_scroll
        isScrollbarFadingEnabled = false
        isVerticalScrollBarEnabled = true
    }
    val summary = TextView(context).apply {
        id = R.id.summary
        setLineSpacing(Resources.dp(context, 4f), 1f)
        setPadding(
            Resources.dp(context, 8f).toInt(),
            Resources.dp(context, 8f).toInt(),
            Resources.dp(context, 8f).toInt(),
            Resources.dp(context, 8f).toInt()
        )
        textSize = 14f
    }
    summaryScroll.addView(
        summary,
        ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )

    val summaryHolder = MaterialCardView(context).apply {
        id = R.id.summary_holder
        radius = Resources.dp(context, 12f)
        cardElevation = Resources.dp(context, 2f)
        isVisible = false
    }
    val content = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        val padding = Resources.dp(context, 16f).toInt()
        setPadding(padding, padding, padding, padding)
    }
    summaryHolder.addView(
        content,
        ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    )
    content.addView(
        summaryChapterTitle,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )
    content.addView(
        summaryTitle,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )
    content.addView(
        summaryScroll,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        )
    )
    root.addView(
        summaryHolder,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            Resources.dp(context, 300f).toInt()
        ).apply {
            leftMargin = Resources.dp(context, 8f).toInt()
            rightMargin = Resources.dp(context, 8f).toInt()
            topMargin = Resources.dp(context, 12f).toInt()
        }
    )

    root.addView(
        View(context),
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            Resources.dp(context, 10f).toInt()
        ).apply {
            setMargins(
                Resources.dp(context, 10f).toInt(),
                Resources.dp(context, 10f).toInt(),
                Resources.dp(context, 10f).toInt(),
                Resources.dp(context, 10f).toInt()
            )
        }
    )

    return SurvivalGuideSheetViews(
        root,
        title,
        search,
        emptyDescription,
        summaryHolder,
        summaryChapterTitle,
        summaryTitle,
        summaryScroll,
        summary
    )
}
