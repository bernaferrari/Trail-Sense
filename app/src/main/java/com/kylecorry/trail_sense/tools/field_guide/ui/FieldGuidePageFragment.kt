package com.kylecorry.trail_sense.tools.field_guide.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTagRow
import com.kylecorry.trail_sense.tools.field_guide.domain.FieldGuidePage
import com.kylecorry.trail_sense.tools.field_guide.infrastructure.FieldGuideRepo

@Composable
fun Fragment.FieldGuidePageContent(modifier: Modifier = Modifier) {
    val context = requireContext()
    val repo = remember { FieldGuideRepo.getInstance(context) }
    val pageId = remember { arguments?.getLong("page_id") ?: 0L }

    var page by remember { mutableStateOf<FieldGuidePage?>(null) }

    LaunchedEffect(pageId) {
        page = repo.getPage(pageId)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TrailSenseCard {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = page?.name ?: "Loading...",
                    style = MaterialTheme.typography.headlineSmall
                )

                page?.tags?.takeIf { it.isNotEmpty() }?.let { tags ->
                    Spacer(Modifier.height(8.dp))
                    TrailSenseTagRow(tags = tags.map { it.name })
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = page?.description ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

class FieldGuidePageFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        FieldGuidePageContent()
    }
}

        useEffect(page, titleView, notesView, imageView, tagsView, navController, fieldGuideFormatter) {
            titleView.rightButton.isVisible = page?.isBuiltIn == false
            titleView.rightButton.setOnClickListener {
                navController.navigate(
                    R.id.createFieldGuidePageFragment,
                    Bundle().apply {
                        page?.id?.let {
                            putLong("page_id", it)
                        }
                    }
                )
            }
            titleView.title.text = page?.let(fieldGuideFormatter::formatTitle)
            val subtitle = page?.let(fieldGuideFormatter::formatSubtitle)
            titleView.subtitle.isVisible = subtitle != null
            titleView.subtitle.text = subtitle
            val noteText = page?.notes?.toSpannable()
            noteText?.let { LinkifyCompat.addLinks(it, WEB_URLS) }
            notesView.text = noteText
            val image = page?.images?.firstOrNull()
            imageView.setImageDrawable(
                image?.let { files.drawable(it) }
            )

            displayTags(tagsView, page?.tags)
        }

        useEffect(sightingsView, page, navController, prefs) {
            page ?: return@useEffect
            sightingsView.setOnClickListener {
                navController.navigate(
                    R.id.sightingListFragment, Bundle().apply {
                        putLong("page_id", page.id)
                    }
                )
            }
            sightingsView.text = getString(R.string.sightings_count, page.sightings.size)
        }

    }

    private val tagTypeColorMap = mapOf(
        FieldGuidePageTagType.Location to AppColor.Gray,
        FieldGuidePageTagType.Habitat to AppColor.Green,
        FieldGuidePageTagType.Classification to AppColor.Blue,
        FieldGuidePageTagType.ActivityPattern to AppColor.Yellow,
        FieldGuidePageTagType.HumanInteraction to AppColor.Brown
    )

    private fun displayTags(view: FlexboxLayout, tags: List<FieldGuidePageTag>? = null) {
        val mapper = FieldGuideTagNameMapper(requireContext())
        val sortedTags =
            (tags ?: emptyList()).sortedWith(compareBy({ it.type.ordinal }, { it.ordinal }))
        if (sortedTags.isEmpty()) {
            view.isVisible = false
            return
        }

        view.isVisible = true
        view.removeAllViews()

        val margin = Resources.dp(requireContext(), 8f).toInt()

        for (tag in sortedTags) {
            val badgeColor = (tagTypeColorMap[tag.type] ?: AppColor.Gray).color
            val foregroundColor = Colors.mostContrastingColor(Color.WHITE, Color.BLACK, badgeColor)
            val tagView = Badge(requireContext(), null).apply {
                statusImage.isVisible = false
                statusText.textSize = 12f
                setStatusText(mapper.getName(tag))
                statusText.setTextColor(foregroundColor)
                setBackgroundTint(badgeColor)
                layoutParams = FlexboxLayout.LayoutParams(
                    FlexboxLayout.LayoutParams.WRAP_CONTENT,
                    FlexboxLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, margin, margin)
                }
            }
            tagView.setOnClickListener { onTagClicked(tag) }
            view.addView(tagView)
        }
    }

    private fun usePage(): FieldGuidePage? {
        val pageId = useArgument<Long?>("page_id")
        val queue = useCoroutineQueue()
        val repo = useService<FieldGuideRepo>()
        val (page, setPage) = useState<FieldGuidePage?>(null)
        val (refreshKey, triggerRefresh) = useTrigger()
        useToolEventListener(FieldGuideToolRegistration.BROADCAST_SIGHTING_RECORDED) {
            triggerRefresh()
        }

        useBackgroundEffect(pageId, refreshKey, lifecycleHookTrigger.onResume()) {
            if (pageId == null) {
                queue.replace { setPage(null) }
            } else {
                queue.replace { setPage(repo.getPage(pageId)) }
            }
        }
        return page
    }

    private fun onTagClicked(tag: FieldGuidePageTag) {
        // TODO: Handle tag click
    }

}
